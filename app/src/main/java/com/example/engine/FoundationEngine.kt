package com.example.engine

import kotlin.math.*

object FoundationEngine {

    enum class FootingType(val label: String) {
        ISOLATED_PAD("Isolated Pad Footing"),
        STRIP_FOOTING("Continuous Strip Foundation"),
        COMBINED_FOOTING("Combined Column Footing"),
        RAFT_FOUNDATION("Raft / Mat Foundation")
    }

    data class FoundationInput(
        val type: FootingType = FootingType.ISOLATED_PAD,
        val columnLoadGkKn: Double = 400.0,
        val columnLoadQkKn: Double = 200.0,
        val soilBearingCapacityQaKpa: Double = 150.0, // Allowable bearing capacity
        val columnWidthMm: Double = 230.0,
        val columnDepthMm: Double = 230.0,
        val foundationDepthDfM: Double = 1.2,
        val fckMPa: Double = 25.0,
        val fykMPa: Double = 500.0,
        val rebarDiaMm: Int = 12,
        val stripWallLengthM: Double = 10.0,
        val designCode: String = "Eurocode 2 & 7 (EN 1997)"
    )

    data class FoundationResult(
        val footingType: FootingType,
        val totalServiceLoadKn: Double,
        val totalDesignLoadNedKn: Double,
        val requiredAreaSqm: Double,
        val providedLengthM: Double,
        val providedWidthM: Double,
        val providedThicknessMm: Double,
        val actualBearingPressureKpa: Double,
        val netUltimatePressureQnetKpa: Double,
        val bendingMomentKNm: Double,
        val requiredSteelMeshMm2: Double,
        val suggestedMesh: String,
        val punchingShearCapacityKn: Double,
        val punchingShearDemandKn: Double,
        val punchingStatus: String,
        val excavationVolumeM3: Double,
        val blindingConcreteM3: Double,
        val footingConcreteM3: Double,
        val steelWeightKg: Double,
        val status: String,
        val geotechnicalWarning: String?,
        val steps: List<String>
    )

    fun calculateFoundation(input: FoundationInput): FoundationResult {
        // 1. Serviceability load for sizing base area (include 10% self weight allowance)
        val totalServiceLoad = (input.columnLoadGkKn + input.columnLoadQkKn) * 1.10
        val gammaG = if (input.designCode.contains("ACI")) 1.2 else 1.35
        val gammaQ = if (input.designCode.contains("ACI")) 1.6 else 1.5
        val nEd = (gammaG * input.columnLoadGkKn) + (gammaQ * input.columnLoadQkKn)

        val reqArea = totalServiceLoad / input.soilBearingCapacityQaKpa

        // 2. Determine Footing Dimensions based on Type
        val (providedL, providedW, providedH) = when (input.type) {
            FootingType.ISOLATED_PAD -> {
                val side = ceil(sqrt(reqArea) * 10.0) / 10.0 // round to nearest 0.1m
                val roundedSide = max(side, 1.0)
                // Thickness rule of thumb: ~ (Side - colSize)/3 or min 300mm
                val h = max(350.0, (roundedSide * 1000.0 - input.columnWidthMm) / 2.5)
                val roundedH = ceil(h / 50.0) * 50.0
                Triple(roundedSide, roundedSide, roundedH)
            }
            FootingType.STRIP_FOOTING -> {
                val width = max(ceil((totalServiceLoad / (input.stripWallLengthM * input.soilBearingCapacityQaKpa)) * 10.0) / 10.0, 0.6)
                val thickness = max(300.0, width * 1000.0 * 0.4)
                Triple(input.stripWallLengthM, width, ceil(thickness / 50.0) * 50.0)
            }
            FootingType.COMBINED_FOOTING -> {
                val width = 1.8
                val length = max(ceil((reqArea * 1.8 / width) * 10.0) / 10.0, 3.5)
                Triple(length, width, 500.0)
            }
            FootingType.RAFT_FOUNDATION -> {
                val length = 15.0
                val width = 12.0
                Triple(length, width, 400.0)
            }
        }

        val actualArea = providedL * providedW
        val actualBearingPressure = totalServiceLoad / actualArea
        val netUltimatePressure = nEd / actualArea

        // 3. Flexural Design at face of column
        val projection = if (input.type == FootingType.ISOLATED_PAD) {
            (providedW * 1000.0 - input.columnWidthMm) / 2000.0 // in meters
        } else {
            (providedW * 1000.0 - input.columnWidthMm) / 2000.0
        }

        val moment = (netUltimatePressure * providedL * projection.pow(2)) / 2.0 // kNm
        val effD = providedH - 50.0 - (input.rebarDiaMm / 2.0) // 50mm cover for ground contact
        val mNmm = moment * 1_000_000.0

        val z = min(effD * 0.95, effD * (0.5 + sqrt((0.25 - (mNmm / (providedL * 1000.0 * effD.pow(2) * input.fckMPa * 1.134))).coerceAtLeast(0.01))))
        val asReq = mNmm / (0.87 * input.fykMPa * z)
        val asMin = 0.0013 * (providedL * 1000.0) * effD
        val finalAs = max(asReq, asMin)

        val singleBarArea = (PI * input.rebarDiaMm.toDouble().pow(2)) / 4.0
        val barsCount = ceil(finalAs / singleBarArea).toInt().coerceAtLeast(5)
        val spacingMm = ((providedL * 1000.0 - 100.0) / (barsCount - 1)).toInt().coerceIn(100, 250)
        val suggestedMesh = "T${input.rebarDiaMm} @ ${spacingMm}mm c/c (B & T Both Ways, $barsCount Bars)"

        // 4. Punching shear check at 2d from column perimeter
        val u0 = 2.0 * (input.columnWidthMm + input.columnDepthMm)
        val u1 = u0 + (2.0 * PI * 2.0 * effD)
        val aPunching = (input.columnWidthMm + 4.0 * effD) * (input.columnDepthMm + 4.0 * effD) / 1_000_000.0
        val vEdPunch = nEd - (netUltimatePressure * aPunching).coerceAtLeast(0.0)

        // Concrete shear resistance VRd,c
        val kPunch = min(1.0 + sqrt(200.0 / effD), 2.0)
        val rho = (finalAs / (providedL * 1000.0 * effD)).coerceAtMost(0.02)
        val vRdcPunch = ((0.18 / 1.5) * kPunch * (100.0 * rho * input.fckMPa).pow(1.0 / 3.0)) * u1 * effD / 1000.0

        val punchingStatus = if (vEdPunch <= vRdcPunch) "PASS (VED = ${vEdPunch.toInt()} kN ≤ VRd,c = ${vRdcPunch.toInt()} kN)" else "WARNING: Punching shear exceeded. Increase footing depth."

        // 5. Material quantities
        val excavationVol = (providedL + 0.6) * (providedW + 0.6) * input.foundationDepthDfM
        val blindingVol = providedL * providedW * 0.05 // 50mm blinding
        val concreteVol = providedL * providedW * (providedH / 1000.0)
        val totalSteelLength = barsCount * (providedL + (2 * providedH / 1000.0)) * 2 // both ways
        val steelWeight = (input.rebarDiaMm.toDouble().pow(2) / 162.0) * totalSteelLength

        var status = "PASS"
        var geoWarning: String? = null

        if (input.soilBearingCapacityQaKpa < 100.0) {
            geoWarning = "Low soil bearing capacity (< 100 kPa). Geotechnical site investigation and raft/pile foundation assessment strictly required."
            status = "WARNING"
        } else if (vEdPunch > vRdcPunch) {
            status = "WARNING"
            geoWarning = "Punching shear demand exceeds capacity. Increase footing thickness to avoid shear failure."
        }

        val steps = listOf(
            "1. Service Column Load (+10% self weight): P_serv = ${String.format("%.1f", totalServiceLoad)} kN",
            "2. Required Footing Plan Area: A_req = P_serv / q_allowable = $totalServiceLoad / ${input.soilBearingCapacityQaKpa} = ${String.format("%.2f", reqArea)} m²",
            "3. Provided Base: ${providedL}m × ${providedW}m × ${providedH.toInt()}mm (Area = ${String.format("%.2f", actualArea)} m²)",
            "4. Service Soil Pressure: q_actual = ${String.format("%.1f", actualBearingPressure)} kPa ≤ ${input.soilBearingCapacityQaKpa} kPa (SAFE)",
            "5. Design Ultimate Soil Pressure: q_net = $nEd / $actualArea = ${String.format("%.1f", netUltimatePressure)} kPa",
            "6. Critical Bending Moment at Column Face: M_Ed = ${String.format("%.1f", moment)} kNm",
            "7. Reinforcement Mesh: $suggestedMesh (Total: ${String.format("%.1f", steelWeight)} kg)",
            "8. Punching Shear: $punchingStatus"
        )

        return FoundationResult(
            footingType = input.type,
            totalServiceLoadKn = totalServiceLoad,
            totalDesignLoadNedKn = nEd,
            requiredAreaSqm = reqArea,
            providedLengthM = providedL,
            providedWidthM = providedW,
            providedThicknessMm = providedH,
            actualBearingPressureKpa = actualBearingPressure,
            netUltimatePressureQnetKpa = netUltimatePressure,
            bendingMomentKNm = moment,
            requiredSteelMeshMm2 = finalAs,
            suggestedMesh = suggestedMesh,
            punchingShearCapacityKn = vRdcPunch,
            punchingShearDemandKn = vEdPunch,
            punchingStatus = punchingStatus,
            excavationVolumeM3 = excavationVol,
            blindingConcreteM3 = blindingVol,
            footingConcreteM3 = concreteVol,
            steelWeightKg = steelWeight,
            status = status,
            geotechnicalWarning = geoWarning,
            steps = steps
        )
    }
}
