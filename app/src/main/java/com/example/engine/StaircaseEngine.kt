package com.example.engine

import kotlin.math.*

object StaircaseEngine {

    enum class StairType(val label: String) {
        DOG_LEGGED("Dog-Legged Staircase (Two Flights with Half Landing)"),
        STRAIGHT_FLIGHT("Single Straight Flight"),
        OPEN_WELL("Open Well Staircase (Three Flights)")
    }

    data class StairInput(
        val type: StairType = StairType.DOG_LEGGED,
        val floorToFloorHeightM: Double = 3.0,
        val preferredRiserMm: Double = 150.0,
        val preferredGoingMm: Double = 275.0,
        val flightWidthM: Double = 1.0,
        val waistThicknessMm: Double = 150.0,
        val landingWidthM: Double = 1.0,
        val fckMPa: Double = 25.0,
        val fykMPa: Double = 500.0,
        val liveLoadQkKnM2: Double = 3.0,
        val finishesGkKnM2: Double = 1.2,
        val mainRebarDiaMm: Int = 12
    )

    data class StairResult(
        val totalRisers: Int,
        val actualRiserMm: Double,
        val actualGoingMm: Double,
        val numberOfGoingsPerFlight: Int,
        val flightLengthOnPlanM: Double,
        val pitchAngleDeg: Double,
        val formulaComfortCheck2RplusG: Double,
        val effectiveSpanM: Double,
        val designLoadKnM2: Double,
        val maxBendingMomentKNm: Double,
        val requiredMainSteelMm2: Double,
        val suggestedMainSteel: String,
        val suggestedDistributionSteel: String,
        val concreteVolumeM3: Double,
        val formworkAreaSqm: Double,
        val totalSteelWeightKg: Double,
        val steps: List<String>
    )

    fun calculateStaircase(input: StairInput): StairResult {
        val totalRisers = ceil((input.floorToFloorHeightM * 1000.0) / input.preferredRiserMm).toInt()
        val actualRiser = (input.floorToFloorHeightM * 1000.0) / totalRisers
        val actualGoing = input.preferredGoingMm

        val risersPerFlight = when (input.type) {
            StairType.DOG_LEGGED -> totalRisers / 2
            StairType.STRAIGHT_FLIGHT -> totalRisers
            StairType.OPEN_WELL -> totalRisers / 3
        }
        val goingsPerFlight = risersPerFlight - 1
        val flightLengthPlan = (goingsPerFlight * actualGoing) / 1000.0

        val pitchAngleRad = atan(actualRiser / actualGoing)
        val pitchAngleDeg = Math.toDegrees(pitchAngleRad)
        val comfortCheck = (2.0 * actualRiser) + actualGoing // Ideally 550 to 700mm per building code

        // Dead load of waist & steps on slope
        val waistM = input.waistThicknessMm / 1000.0
        val deadWaistOnSlope = waistM * 25.0 // kN/m2
        val deadWaistOnPlan = deadWaistOnSlope / cos(pitchAngleRad)
        val deadStepsOnPlan = 0.5 * (actualRiser / 1000.0) * 25.0
        val totalGkOnPlan = deadWaistOnPlan + deadStepsOnPlan + input.finishesGkKnM2

        val designWd = (1.35 * totalGkOnPlan) + (1.50 * input.liveLoadQkKnM2)

        // Effective span (flight plan length + landing half spans)
        val effectiveSpan = flightLengthPlan + (input.landingWidthM * (if (input.type == StairType.DOG_LEGGED) 1.0 else 0.5))
        val maxMoment = (designWd * input.flightWidthM * effectiveSpan.pow(2)) / 8.0

        val d = input.waistThicknessMm - 20.0 - (input.mainRebarDiaMm / 2.0)
        val mNmm = maxMoment * 1_000_000.0
        val z = min(0.95 * d, d * (0.5 + sqrt((0.25 - (mNmm / (input.flightWidthM * 1000.0 * d.pow(2) * input.fckMPa * 1.134))).coerceAtLeast(0.01))))
        val asReq = mNmm / (0.87 * input.fykMPa * z)
        val asMin = 0.0013 * (input.flightWidthM * 1000.0) * d
        val finalAs = max(asReq, asMin)

        val barArea = (PI * input.mainRebarDiaMm.toDouble().pow(2)) / 4.0
        val mainSpacing = min(250.0, (barArea * (input.flightWidthM * 1000.0)) / finalAs)
        val roundedMainSpacing = (floor(mainSpacing / 25.0) * 25.0).toInt().coerceIn(100, 200)
        val suggestedMain = "T${input.mainRebarDiaMm} @ ${roundedMainSpacing}mm c/c (Bottom main tension)"
        val suggestedDist = "T10 @ 200mm c/c (Distribution/transverse steel)"

        // Quantities
        val slopeLength = sqrt(flightLengthPlan.pow(2) + ((risersPerFlight * actualRiser) / 1000.0).pow(2))
        val waistVol = slopeLength * waistM * input.flightWidthM
        val stepsVol = goingsPerFlight * 0.5 * (actualRiser / 1000.0) * (actualGoing / 1000.0) * input.flightWidthM
        val landingVol = input.landingWidthM * (input.flightWidthM * 2.0) * waistM
        val totalConcreteVol = (waistVol + stepsVol) * (if (input.type == StairType.DOG_LEGGED) 2.0 else 1.0) + landingVol

        val formworkArea = (slopeLength * input.flightWidthM * 2) + (risersPerFlight * (actualRiser / 1000.0) * input.flightWidthM * 2)
        val steelWeight = (input.mainRebarDiaMm.toDouble().pow(2) / 162.0) * 45.0 + (100.0 / 162.0) * 30.0

        val steps = listOf(
            "1. Stair Geometry: Floor Height = ${input.floorToFloorHeightM}m → $totalRisers Total Risers @ ${String.format("%.1f", actualRiser)}mm with ${actualGoing.toInt()}mm Going",
            "2. Pitch Angle: tan⁻¹(${String.format("%.1f", actualRiser)}/${actualGoing.toInt()}) = ${String.format("%.1f", pitchAngleDeg)}° (Code limit: 25°–42°)",
            "3. Comfort Rule: (2R + G) = 2(${String.format("%.1f", actualRiser)}) + ${actualGoing.toInt()} = ${String.format("%.1f", comfortCheck)}mm (Ideal range: 550–700mm)",
            "4. Factored Design Load (w_d): 1.35(${String.format("%.2f", totalGkOnPlan)}) + 1.5(${input.liveLoadQkKnM2}) = ${String.format("%.2f", designWd)} kN/m²",
            "5. Maximum Design Bending Moment: M_Ed = (w_d × L_eff²)/8 = ${String.format("%.2f", maxMoment)} kNm",
            "6. Reinforcement: Main Steel = ${finalAs.toInt()} mm² → $suggestedMain",
            "7. Concrete & Formwork: Concrete = ${String.format("%.2f", totalConcreteVol)} m³ | Formwork = ${String.format("%.1f", formworkArea)} m²"
        )

        return StairResult(
            totalRisers = totalRisers,
            actualRiserMm = actualRiser,
            actualGoingMm = actualGoing,
            numberOfGoingsPerFlight = goingsPerFlight,
            flightLengthOnPlanM = flightLengthPlan,
            pitchAngleDeg = pitchAngleDeg,
            formulaComfortCheck2RplusG = comfortCheck,
            effectiveSpanM = effectiveSpan,
            designLoadKnM2 = designWd,
            maxBendingMomentKNm = maxMoment,
            requiredMainSteelMm2 = finalAs,
            suggestedMainSteel = suggestedMain,
            suggestedDistributionSteel = suggestedDist,
            concreteVolumeM3 = totalConcreteVol,
            formworkAreaSqm = formworkArea,
            totalSteelWeightKg = steelWeight,
            steps = steps
        )
    }
}
