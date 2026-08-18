package com.example.engine

import kotlin.math.*

object SlabDesignEngine {

    enum class SlabType(val label: String) {
        ONE_WAY("One-Way Spanning Slab (Ly/Lx > 2.0)"),
        TWO_WAY_SIMPLY_SUPPORTED("Two-Way Simply Supported (Ly/Lx ≤ 2.0)"),
        TWO_WAY_CONTINUOUS("Two-Way Restrained / Continuous Panel")
    }

    data class SlabInput(
        val shortSpanLxM: Double = 4.0,
        val longSpanLyM: Double = 5.0,
        val slabThicknessMm: Double = 150.0,
        val coverMm: Double = 20.0,
        val fckMPa: Double = 25.0,
        val fykMPa: Double = 500.0,
        val finishesGkKnM2: Double = 1.5, // screed + tiles + ceiling
        val partitionGkKnM2: Double = 1.0,
        val liveLoadQkKnM2: Double = 2.5, // residential / office occupancy
        val rebarDiaMm: Int = 10,
        val designCode: String = "Eurocode 2 (EN 1992)"
    )

    data class SlabResult(
        val slabType: SlabType,
        val aspectRatioLyLx: Double,
        val selfWeightKnM2: Double,
        val totalDeadLoadGkKnM2: Double,
        val designLoadWdKnM2: Double,
        val effectiveDepthDMm: Double,
        val momentShortSpanMsxKNm: Double,
        val momentLongSpanMsyKNm: Double,
        val requiredSteelShortSpanAsxMm2: Double,
        val requiredSteelLongSpanAsyMm2: Double,
        val minSteelAsMinMm2: Double,
        val suggestedRebarShortSpan: String,
        val suggestedRebarLongSpan: String,
        val concreteVolumeM3: Double,
        val formworkAreaSqm: Double,
        val totalSteelWeightKg: Double,
        val deflectionRatioActual: Double,
        val deflectionRatioAllowable: Double,
        val deflectionStatus: String,
        val steps: List<String>
    )

    fun calculateSlab(input: SlabInput): SlabResult {
        val aspectRatio = input.longSpanLyM / input.shortSpanLxM
        val slabType = when {
            aspectRatio > 2.0 -> SlabType.ONE_WAY
            else -> SlabType.TWO_WAY_SIMPLY_SUPPORTED
        }

        val selfWeight = (input.slabThicknessMm / 1000.0) * 25.0 // kN/m2
        val totalGk = selfWeight + input.finishesGkKnM2 + input.partitionGkKnM2

        val gammaG = if (input.designCode.contains("ACI")) 1.2 else 1.35
        val gammaQ = if (input.designCode.contains("ACI")) 1.6 else 1.5
        val designWd = (gammaG * totalGk) + (gammaQ * input.liveLoadQkKnM2)

        val d = input.slabThicknessMm - input.coverMm - (input.rebarDiaMm / 2.0)

        // Moments calculation based on aspect ratio
        val (msx, msy) = if (slabType == SlabType.ONE_WAY) {
            val m1 = (designWd * input.shortSpanLxM.pow(2)) / 8.0
            Pair(m1, m1 * 0.20) // distribution steel handles minimum in long direction
        } else {
            // Marcus / Rankine-Grashof coefficients for simply supported two-way slabs
            val alphaX = (aspectRatio.pow(4) / (8.0 * (1.0 + aspectRatio.pow(4))))
            val alphaY = (1.0 / (8.0 * (1.0 + aspectRatio.pow(4))))
            val mx = alphaX * designWd * input.shortSpanLxM.pow(2)
            val my = alphaY * designWd * input.shortSpanLxM.pow(2)
            Pair(mx, my)
        }

        // Required steel per meter width (b = 1000mm)
        val asMin = 0.0013 * 1000.0 * d
        val zx = min(0.95 * d, d * (0.5 + sqrt((0.25 - ((msx * 1_000_000.0) / (1000.0 * d.pow(2) * input.fckMPa * 1.134))).coerceAtLeast(0.01))))
        val rawAsx = (msx * 1_000_000.0) / (0.87 * input.fykMPa * zx)
        val finalAsx = max(rawAsx, asMin)

        val zy = min(0.95 * d, d * (0.5 + sqrt((0.25 - ((msy * 1_000_000.0) / (1000.0 * d.pow(2) * input.fckMPa * 1.134))).coerceAtLeast(0.01))))
        val rawAsy = (msy * 1_000_000.0) / (0.87 * input.fykMPa * zy)
        val finalAsy = max(rawAsy, asMin)

        // Rebar spacing determination (T10 or T12)
        val barArea = (PI * input.rebarDiaMm.toDouble().pow(2)) / 4.0
        val spacingX = min(3.0 * input.slabThicknessMm, min(300.0, (barArea * 1000.0) / finalAsx)).toInt()
        val roundedSpacingX = (floor(spacingX / 25.0) * 25.0).toInt().coerceIn(100, 250)

        val spacingY = min(3.0 * input.slabThicknessMm, min(350.0, (barArea * 1000.0) / finalAsy)).toInt()
        val roundedSpacingY = (floor(spacingY / 25.0) * 25.0).toInt().coerceIn(100, 300)

        val suggestedRebarX = "T${input.rebarDiaMm} @ ${roundedSpacingX}mm c/c (Short span, ${finalAsx.toInt()} mm²/m)"
        val suggestedRebarY = "T${input.rebarDiaMm} @ ${roundedSpacingY}mm c/c (Long span, ${finalAsy.toInt()} mm²/m)"

        // Quantities
        val panelArea = input.shortSpanLxM * input.longSpanLyM
        val concreteVol = panelArea * (input.slabThicknessMm / 1000.0)
        val formworkArea = panelArea

        val numBarsX = ceil((input.longSpanLyM * 1000.0) / roundedSpacingX).toInt() + 1
        val numBarsY = ceil((input.shortSpanLxM * 1000.0) / roundedSpacingY).toInt() + 1
        val totalLengthM = (numBarsX * input.shortSpanLxM) + (numBarsY * input.longSpanLyM)
        val steelWeightKg = (input.rebarDiaMm.toDouble().pow(2) / 162.0) * totalLengthM

        // Deflection check
        val actualSpanDepth = (input.shortSpanLxM * 1000.0) / d
        val allowSpanDepth = 26.0 * 1.2 // basic ratio ~26 for two-way lightly loaded
        val deflStatus = if (actualSpanDepth <= allowSpanDepth) "PASS (${String.format("%.1f", actualSpanDepth)} ≤ ${String.format("%.1f", allowSpanDepth)})" else "WARNING: Slab depth may be too thin for deflection"

        val steps = listOf(
            "1. Aspect Ratio: Ly/Lx = ${input.longSpanLyM}m / ${input.shortSpanLxM}m = ${String.format("%.2f", aspectRatio)} (${slabType.label})",
            "2. Slab Self-Weight: (${input.slabThicknessMm}/1000) × 25 = ${String.format("%.2f", selfWeight)} kN/m²",
            "3. Total Factored Load: w_d = ($gammaG × ${String.format("%.2f", totalGk)}) + ($gammaQ × ${input.liveLoadQkKnM2}) = ${String.format("%.2f", designWd)} kN/m²",
            "4. Short Span Moment: M_sx = ${String.format("%.2f", msx)} kNm/m → A_sx,req = ${finalAsx.toInt()} mm²/m",
            "5. Long Span Moment: M_sy = ${String.format("%.2f", msy)} kNm/m → A_sy,req = ${finalAsy.toInt()} mm²/m",
            "6. Reinforcement Proposal: X-dir: $suggestedRebarX | Y-dir: $suggestedRebarY",
            "7. Material Requirements: Concrete: ${String.format("%.2f", concreteVol)} m³ | Formwork: ${String.format("%.1f", formworkArea)} m² | Rebar: ${String.format("%.1f", steelWeightKg)} kg"
        )

        return SlabResult(
            slabType = slabType,
            aspectRatioLyLx = aspectRatio,
            selfWeightKnM2 = selfWeight,
            totalDeadLoadGkKnM2 = totalGk,
            designLoadWdKnM2 = designWd,
            effectiveDepthDMm = d,
            momentShortSpanMsxKNm = msx,
            momentLongSpanMsyKNm = msy,
            requiredSteelShortSpanAsxMm2 = finalAsx,
            requiredSteelLongSpanAsyMm2 = finalAsy,
            minSteelAsMinMm2 = asMin,
            suggestedRebarShortSpan = suggestedRebarX,
            suggestedRebarLongSpan = suggestedRebarY,
            concreteVolumeM3 = concreteVol,
            formworkAreaSqm = formworkArea,
            totalSteelWeightKg = steelWeightKg,
            deflectionRatioActual = actualSpanDepth,
            deflectionRatioAllowable = allowSpanDepth,
            deflectionStatus = deflStatus,
            steps = steps
        )
    }
}
