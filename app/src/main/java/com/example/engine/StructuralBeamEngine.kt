package com.example.engine

import kotlin.math.*

object StructuralBeamEngine {

    data class BeamInput(
        val spanM: Double = 5.0,
        val widthMm: Double = 230.0,
        val depthMm: Double = 450.0,
        val coverMm: Double = 25.0,
        val fckMPa: Double = 25.0, // Characteristic compressive cylinder strength
        val fykMPa: Double = 500.0, // Yield strength of steel
        val deadLoadGkKnM: Double = 12.0, // Superimposed dead load (excluding self-weight)
        val liveLoadQkKnM: Double = 6.0,
        val pointLoadGkKn: Double = 0.0, // Additional point load at midspan
        val pointLoadQkKn: Double = 0.0,
        val mainBarDiaMm: Int = 16,
        val linkDiaMm: Int = 8,
        val designCode: String = "Eurocode 2 (EN 1992-1-1)"
    )

    data class BeamResult(
        val selfWeightKnM: Double,
        val totalDeadLoadGkKnM: Double,
        val designUdlKnM: Double,
        val designPointLoadKn: Double,
        val maxMomentMedKNm: Double,
        val maxShearVedKn: Double,
        val effectiveDepthDMm: Double,
        val kValue: Double,
        val kPrime: Double,
        val isDoublyReinforced: Boolean,
        val leverArmZMm: Double,
        val requiredSteelAreaAsReqMm2: Double,
        val minSteelAreaAsMinMm2: Double,
        val maxSteelAreaAsMaxMm2: Double,
        val suggestedBottomBars: String,
        val providedBottomAreaMm2: Double,
        val suggestedTopHangerBars: String,
        val suggestedLinks: String,
        val shearConcreteCapacityVrdcKn: Double,
        val shearStatus: String, // OK or REQUIRES_SHEAR_LINKS
        val deflectionActualSpanToDepth: Double,
        val deflectionAllowableSpanToDepth: Double,
        val deflectionStatus: String,
        val overallStatus: String, // PASS, WARNING, FAIL
        val warningMessage: String?,
        val designAssumptions: List<String>,
        val governingEquations: List<String>
    )

    fun calculateBeam(input: BeamInput): BeamResult {
        // 1. Self Weight = b (m) * h (m) * 25 kN/m3 (reinforced concrete density)
        val bM = input.widthMm / 1000.0
        val hM = input.depthMm / 1000.0
        val selfWeight = bM * hM * 25.0
        val totalGk = input.deadLoadGkKnM + selfWeight

        // 2. Factored Design Loads based on Design Code
        val gammaG = when {
            input.designCode.contains("ACI") -> 1.2
            input.designCode.contains("BS 8110") -> 1.4
            else -> 1.35 // Eurocode 2
        }
        val gammaQ = when {
            input.designCode.contains("ACI") -> 1.6
            input.designCode.contains("BS 8110") -> 1.6
            else -> 1.5 // Eurocode 2
        }

        val designUdl = (gammaG * totalGk) + (gammaQ * input.liveLoadQkKnM)
        val designPointLoad = (gammaG * input.pointLoadGkKn) + (gammaQ * input.pointLoadQkKn)

        // 3. Structural Analysis: Simply supported max moment & max shear
        val momentUdl = (designUdl * input.spanM.pow(2)) / 8.0
        val momentPoint = (designPointLoad * input.spanM) / 4.0
        val maxMomentMed = momentUdl + momentPoint

        val shearUdl = (designUdl * input.spanM) / 2.0
        val shearPoint = designPointLoad / 2.0
        val maxShearVed = shearUdl + shearPoint

        // 4. Effective Depth
        val d = input.depthMm - input.coverMm - input.linkDiaMm - (input.mainBarDiaMm / 2.0)
        val medNmm = maxMomentMed * 1_000_000.0

        // 5. Flexural Design Parameters (Eurocode 2 / ACI)
        val kValue = medNmm / (input.widthMm * d.pow(2) * input.fckMPa)
        val kPrime = 0.167 // redistribution limit for singly reinforced beam

        val isDoubly = kValue > kPrime
        val leverArmZ = if (!isDoubly) {
            val calcZ = d * (0.5 + sqrt((0.25 - (kValue / 1.134)).coerceAtLeast(0.0)))
            min(calcZ, 0.95 * d)
        } else {
            0.95 * d // Simplified lever arm upper bound for doubly
        }

        // 6. Steel area required
        val asReq = medNmm / (0.87 * input.fykMPa * leverArmZ)

        // 7. Code Min & Max Steel
        // EC2: As,min = 0.26 * (fctm / fyk) * b * d >= 0.0013 * b * d
        val fctm = 0.30 * input.fckMPa.pow(2.0 / 3.0)
        val asMin = max(0.26 * (fctm / input.fykMPa) * input.widthMm * d, 0.0013 * input.widthMm * d)
        val asMax = 0.04 * input.widthMm * input.depthMm // 4% of gross section

        val finalAsReq = max(asReq, asMin)

        // 8. Auto-bar selection for tension reinforcement
        val singleBarArea = (PI * input.mainBarDiaMm.toDouble().pow(2)) / 4.0
        val barsCount = ceil(finalAsReq / singleBarArea).toInt().coerceAtLeast(2)
        val providedArea = barsCount * singleBarArea
        val suggestedBottom = "${barsCount}T${input.mainBarDiaMm} (Provided: ${providedArea.toInt()} mm² vs Req: ${finalAsReq.toInt()} mm²)"
        val suggestedTop = "2T12 (Hanger Bars)"

        // 9. Shear Design Check
        val rho1 = (providedArea / (input.widthMm * d)).coerceAtMost(0.02)
        val kShear = min(1.0 + sqrt(200.0 / d), 2.0)
        val vRdcMin = (0.035 * kShear.pow(1.5) * sqrt(input.fckMPa)) * input.widthMm * d / 1000.0
        val vRdcCalc = ((0.18 / 1.5) * kShear * (100.0 * rho1 * input.fckMPa).pow(1.0 / 3.0)) * input.widthMm * d / 1000.0
        val vRdc = max(vRdcCalc, vRdcMin)

        val shearStatus = if (maxShearVed <= vRdc) {
            "Nominal Shear Links Required (Ved ≤ VRd,c)"
        } else {
            "Design Shear Links Required (Ved > VRd,c)"
        }

        // Link spacing calculation: Asw/s = (Ved * 1000) / (0.78 * d * fyk * cot(theta)) with theta=21.8 deg
        val linkArea2Legs = 2.0 * (PI * input.linkDiaMm.toDouble().pow(2) / 4.0)
        val maxLinkSpacing = min(0.75 * d, 300.0)
        val calcSpacing = (linkArea2Legs * 0.78 * d * input.fykMPa * 2.5) / (maxShearVed * 1000.0)
        val finalLinkSpacing = min(calcSpacing, maxLinkSpacing).coerceAtLeast(75.0)
        val roundedLinkSpacing = (floor(finalLinkSpacing / 25.0) * 25.0).toInt().coerceIn(75, 300)
        val suggestedLinks = "T${input.linkDiaMm} @ ${roundedLinkSpacing}mm c/c"

        // 10. Deflection check (Span/depth ratio)
        val actualSpanToDepth = (input.spanM * 1000.0) / d
        // Basic ratio for simply supported beam = ~20 for lightly stressed member
        val tensionModFactor = min(310.0 / (0.87 * input.fykMPa * (finalAsReq / providedArea)), 1.5)
        val allowableSpanToDepth = 20.0 * tensionModFactor
        val deflectionStatus = if (actualSpanToDepth <= allowableSpanToDepth) "PASS (Span/d: ${String.format("%.1f", actualSpanToDepth)} ≤ ${String.format("%.1f", allowableSpanToDepth)})" else "WARNING: Deflection check fails (Span/d: ${String.format("%.1f", actualSpanToDepth)} > ${String.format("%.1f", allowableSpanToDepth)})"

        var overallStatus = "PASS"
        var warning: String? = null

        if (isDoubly) {
            overallStatus = "WARNING"
            warning = "K ($kValue) exceeds K' ($kPrime). Doubly reinforced beam required. Increase beam depth ($input.depthMm mm) or width ($input.widthMm mm) for economical singly reinforced section."
        } else if (actualSpanToDepth > allowableSpanToDepth) {
            overallStatus = "WARNING"
            warning = "Span-to-effective depth ratio (${String.format("%.1f", actualSpanToDepth)}) exceeds code allowable limit (${String.format("%.1f", allowableSpanToDepth)}). Excessive deflection anticipated."
        } else if (finalAsReq > asMax) {
            overallStatus = "FAIL"
            warning = "Required steel area exceeds maximum allowable 4% gross concrete section limit."
        }

        val assumptions = listOf(
            "Design Code: ${input.designCode}",
            "Load Factors: γ_G = $gammaG (Dead), γ_Q = $gammaQ (Live)",
            "Material Strengths: Concrete f_ck = ${input.fckMPa} MPa, Steel f_yk = ${input.fykMPa} MPa",
            "Nominal Concrete Cover: ${input.coverMm} mm to links",
            "Self-weight calculated automatically at 25.0 kN/m³ = ${String.format("%.2f", selfWeight)} kN/m",
            "Effective Depth: d = h ($input.depthMm) - c ($input.coverMm) - link ($input.linkDiaMm) - bar/2 (${input.mainBarDiaMm / 2}) = ${d.toInt()} mm"
        )

        val equations = listOf(
            "1. Design Load: w_d = ($gammaG × Total G_k) + ($gammaQ × Q_k) = ${String.format("%.2f", designUdl)} kN/m",
            "2. Max Design Moment: M_Ed = (w_d × L²)/8 = ${String.format("%.2f", maxMomentMed)} kNm (${String.format("%.0f", medNmm)} N·mm)",
            "3. Section K-factor: K = M_Ed / (b × d² × f_ck) = ${String.format("%.4f", kValue)} (K' limit = $kPrime)",
            "4. Lever Arm: z = d × [0.5 + √(0.25 - K/1.134)] = ${leverArmZ.toInt()} mm (≤ 0.95d)",
            "5. Required Steel Area: A_s,req = M_Ed / (0.87 × f_yk × z) = ${finalAsReq.toInt()} mm²",
            "6. Minimum Steel: A_s,min = 0.26 × (f_ctm/f_yk) × b × d = ${asMin.toInt()} mm²",
            "7. Shear Capacity without Links: V_Rd,c = ${String.format("%.2f", vRdc)} kN vs Design Shear V_Ed = ${String.format("%.2f", maxShearVed)} kN",
            "8. Shear Links: $suggestedLinks"
        )

        return BeamResult(
            selfWeightKnM = selfWeight,
            totalDeadLoadGkKnM = totalGk,
            designUdlKnM = designUdl,
            designPointLoadKn = designPointLoad,
            maxMomentMedKNm = maxMomentMed,
            maxShearVedKn = maxShearVed,
            effectiveDepthDMm = d,
            kValue = kValue,
            kPrime = kPrime,
            isDoublyReinforced = isDoubly,
            leverArmZMm = leverArmZ,
            requiredSteelAreaAsReqMm2 = finalAsReq,
            minSteelAreaAsMinMm2 = asMin,
            maxSteelAreaAsMaxMm2 = asMax,
            suggestedBottomBars = suggestedBottom,
            providedBottomAreaMm2 = providedArea,
            suggestedTopHangerBars = suggestedTop,
            suggestedLinks = suggestedLinks,
            shearConcreteCapacityVrdcKn = vRdc,
            shearStatus = shearStatus,
            deflectionActualSpanToDepth = actualSpanToDepth,
            deflectionAllowableSpanToDepth = allowableSpanToDepth,
            deflectionStatus = deflectionStatus,
            overallStatus = overallStatus,
            warningMessage = warning,
            designAssumptions = assumptions,
            governingEquations = equations
        )
    }
}
