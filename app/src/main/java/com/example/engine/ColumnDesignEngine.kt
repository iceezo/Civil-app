package com.example.engine

import kotlin.math.*

object ColumnDesignEngine {

    data class ColumnInput(
        val widthMm: Double = 230.0,
        val depthMm: Double = 230.0,
        val clearHeightM: Double = 3.0,
        val fckMPa: Double = 25.0,
        val fykMPa: Double = 500.0,
        val axialDeadLoadGkKn: Double = 350.0,
        val axialLiveLoadQkKn: Double = 150.0,
        val momentMxKNm: Double = 15.0,
        val momentMyKNm: Double = 10.0,
        val effectiveLengthFactorBeta: Double = 0.85, // Pinned/Fixed end conditions
        val rebarDiaMm: Int = 16,
        val linkDiaMm: Int = 8,
        val designCode: String = "Eurocode 2 (EN 1992)"
    )

    data class ColumnResult(
        val grossAreaAcMm2: Double,
        val effectiveHeightLeM: Double,
        val slendernessRatioLambda: Double,
        val isSlender: Boolean,
        val designAxialLoadNedKn: Double,
        val axialCapacityNrdKn: Double,
        val minEccentricityE0Mm: Double,
        val designMomentMedKNm: Double,
        val requiredSteelAscMm2: Double,
        val minSteelAscMinMm2: Double,
        val maxSteelAscMaxMm2: Double,
        val suggestedLongitudinalBars: String,
        val numberOfBars: Int,
        val barDiameterMm: Int,
        val suggestedLinks: String,
        val status: String,
        val warning: String?,
        val steps: List<String>
    )

    fun calculateColumn(input: ColumnInput): ColumnResult {
        val grossAc = input.widthMm * input.depthMm
        val gammaG = if (input.designCode.contains("ACI")) 1.2 else 1.35
        val gammaQ = if (input.designCode.contains("ACI")) 1.6 else 1.5

        val nEd = (gammaG * input.axialDeadLoadGkKn) + (gammaQ * input.axialLiveLoadQkKn)

        // Effective length & Slenderness
        val l0 = input.effectiveLengthFactorBeta * input.clearHeightM
        val radiusOfGyrationI = input.depthMm / sqrt(12.0)
        val lambda = (l0 * 1000.0) / radiusOfGyrationI
        val lambdaLim = 20.0 * 0.7 // Standard limit for non-slender column (~15-20)
        val isSlender = lambda > lambdaLim

        // Minimum eccentricity e0 = max(h/30, 20mm)
        val e0 = max(input.depthMm / 30.0, 20.0)
        val minMomentFromEccentricity = nEd * (e0 / 1000.0)
        val designMoment = max(max(input.momentMxKNm, input.momentMyKNm), minMomentFromEccentricity)

        // Simplified axial capacity & required reinforcement
        // Under pure axial / minimal eccentricity: N_Rd = 0.567 * fck * Ac + 0.87 * fyk * Asc
        // For axial + moment interaction, approximate using equivalent axial load enhancement
        val momentFactor = 1.0 + (1.5 * (designMoment * 1000.0) / (nEd * input.depthMm)).coerceAtMost(1.0)
        val equivalentNed = nEd * momentFactor

        val concreteCapacity = 0.567 * (input.fckMPa / 1.5) * grossAc / 1000.0
        val remainingLoad = (equivalentNed - concreteCapacity).coerceAtLeast(0.0)
        val rawAsc = (remainingLoad * 1000.0) / (0.87 * input.fykMPa)

        val ascMin = 0.002 * grossAc // 0.2% Ac min per Eurocode 2
        val ascMax = 0.04 * grossAc // 4% Ac max

        val finalAsc = max(rawAsc, ascMin)

        // Bar layout calculation (symmetric 4 or 8 bars)
        val singleBarArea = (PI * input.rebarDiaMm.toDouble().pow(2)) / 4.0
        var barCount = ceil(finalAsc / singleBarArea).toInt().coerceAtLeast(4)
        if (barCount % 2 != 0) barCount += 1 // even number of bars for symmetry
        if (barCount == 6 && input.widthMm == input.depthMm) barCount = 8 // corners + faces

        val provAsc = barCount * singleBarArea
        val totalCapacity = (concreteCapacity * 1.5 * 0.567) + (0.87 * input.fykMPa * provAsc / 1000.0)

        // Tie spacing: min(20 * mainBarDia, min(b, h), 400mm)
        val linkSpacing = min(min(input.widthMm, input.depthMm), min(20.0 * input.rebarDiaMm, 400.0)).toInt()
        val roundedLinkSpacing = (floor(linkSpacing / 25.0) * 25.0).toInt().coerceIn(100, 300)

        var status = "PASS"
        var warning: String? = null

        if (provAsc > ascMax) {
            status = "FAIL"
            warning = "Required steel exceeds 4% gross column area. Increase column section dimensions."
        } else if (isSlender) {
            status = "WARNING"
            warning = "Column is slender (λ = ${String.format("%.1f", lambda)} > ${String.format("%.1f", lambdaLim)}). Second-order moments must be checked in rigorous design."
        }

        val steps = listOf(
            "1. Gross Section Area: A_c = ${input.widthMm.toInt()} × ${input.depthMm.toInt()} = ${grossAc.toInt()} mm²",
            "2. Design Axial Load: N_Ed = ($gammaG × ${input.axialDeadLoadGkKn}) + ($gammaQ × ${input.axialLiveLoadQkKn}) = ${String.format("%.1f", nEd)} kN",
            "3. Slenderness: l_0 = ${String.format("%.2f", l0)} m → λ = l_0/i = ${String.format("%.1f", lambda)} (${if (isSlender) "Slender" else "Short/Stocky"})",
            "4. Minimum Eccentricity: e_0 = max(h/30, 20mm) = ${e0.toInt()} mm → M_min = ${String.format("%.2f", minMomentFromEccentricity)} kNm",
            "5. Design Moment M_Ed = ${String.format("%.2f", designMoment)} kNm",
            "6. Steel Area Req: A_sc,req = ${finalAsc.toInt()} mm² (Min: ${ascMin.toInt()} mm², Max: ${ascMax.toInt()} mm²)",
            "7. Provided Rebar: ${barCount}T${input.rebarDiaMm} (Area: ${provAsc.toInt()} mm²)",
            "8. Lateral Links: T${input.linkDiaMm} @ ${roundedLinkSpacing}mm c/c"
        )

        return ColumnResult(
            grossAreaAcMm2 = grossAc,
            effectiveHeightLeM = l0,
            slendernessRatioLambda = lambda,
            isSlender = isSlender,
            designAxialLoadNedKn = nEd,
            axialCapacityNrdKn = totalCapacity,
            minEccentricityE0Mm = e0,
            designMomentMedKNm = designMoment,
            requiredSteelAscMm2 = finalAsc,
            minSteelAscMinMm2 = ascMin,
            maxSteelAscMaxMm2 = ascMax,
            suggestedLongitudinalBars = "${barCount}T${input.rebarDiaMm} (${provAsc.toInt()} mm²)",
            numberOfBars = barCount,
            barDiameterMm = input.rebarDiaMm,
            suggestedLinks = "T${input.linkDiaMm} @ ${roundedLinkSpacing}mm c/c",
            status = status,
            warning = warning,
            steps = steps
        )
    }
}
