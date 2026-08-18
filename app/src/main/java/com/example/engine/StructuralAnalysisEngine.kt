package com.example.engine

import kotlin.math.*

object StructuralAnalysisEngine {

    enum class BeamSupportType(val label: String) {
        SIMPLY_SUPPORTED("Simply Supported (Pinned - Roller)"),
        CANTILEVER("Cantilever (Fixed - Free)"),
        FIXED_FIXED("Fixed - Fixed"),
        PROPPED_CANTILEVER("Propped Cantilever (Fixed - Roller)")
    }

    data class PointLoad(
        val positionM: Double,
        val magnitudeKn: Double,
        val label: String = "Point Load"
    )

    data class AnalysisInput(
        val spanM: Double = 6.0,
        val supportType: BeamSupportType = BeamSupportType.SIMPLY_SUPPORTED,
        val udlKnM: Double = 18.0,
        val pointLoads: List<PointLoad> = listOf(PointLoad(3.0, 25.0, "Central Point Load")),
        val elasticModulusE_Gpa: Double = 30.0, // Concrete E = 30-34 GPa
        val momentOfInertiaI_Mm4: Double = 1.75e9 // 230 x 450 beam: I = (230 * 450^3)/12 = 1.745e9 mm4
    )

    data class DiagramPoint(
        val xM: Double,
        val shearKn: Double,
        val momentKNm: Double,
        val deflectionMm: Double
    )

    data class AnalysisResult(
        val supportType: BeamSupportType,
        val reactionA_Kn: Double,
        val reactionB_Kn: Double,
        val reactionMomentA_KNm: Double,
        val maxShearKn: Double,
        val maxMomentKNm: Double,
        val maxDeflectionMm: Double,
        val diagramPoints: List<DiagramPoint>,
        val memberUtilizationPercent: Double,
        val steps: List<String>
    )

    fun solveBeam(input: AnalysisInput): AnalysisResult {
        val totalUdlLoad = input.udlKnM * input.spanM
        val totalPointLoad = input.pointLoads.sumOf { it.magnitudeKn }
        val totalLoad = totalUdlLoad + totalPointLoad

        var ra = 0.0
        var rb = 0.0
        var ma = 0.0

        when (input.supportType) {
            BeamSupportType.SIMPLY_SUPPORTED -> {
                // Moment about B = 0 -> Ra * L = (UDL * L * L/2) + sum(P_i * (L - x_i))
                val momentAboutB = (input.udlKnM * input.spanM.pow(2) / 2.0) +
                        input.pointLoads.sumOf { it.magnitudeKn * (input.spanM - it.positionM) }
                ra = momentAboutB / input.spanM
                rb = totalLoad - ra
                ma = 0.0
            }
            BeamSupportType.CANTILEVER -> {
                ra = totalLoad
                rb = 0.0
                ma = -( (input.udlKnM * input.spanM.pow(2) / 2.0) + input.pointLoads.sumOf { it.magnitudeKn * it.positionM } )
            }
            BeamSupportType.FIXED_FIXED -> {
                ra = totalLoad / 2.0
                rb = totalLoad / 2.0
                ma = -( (input.udlKnM * input.spanM.pow(2) / 12.0) + input.pointLoads.sumOf { (it.magnitudeKn * it.positionM * (input.spanM - it.positionM).pow(2)) / input.spanM.pow(2) } )
            }
            BeamSupportType.PROPPED_CANTILEVER -> {
                rb = (3.0 * input.udlKnM * input.spanM / 8.0) + input.pointLoads.sumOf { it.magnitudeKn * (input.positionMToProppedCoeff(it.positionM, input.spanM)) }
                ra = totalLoad - rb
                ma = -( (input.udlKnM * input.spanM.pow(2) / 8.0) )
            }
        }

        // Generate 41 discretized points along the span for SFD, BMD, and Deflection curves
        val numSegments = 40
        val dx = input.spanM / numSegments
        val points = mutableListOf<DiagramPoint>()

        var maxV = 0.0
        var maxM = 0.0
        var maxDelta = 0.0

        val eInN_M2 = input.elasticModulusE_Gpa * 1e9
        val iInM4 = input.momentOfInertiaI_Mm4 * 1e-12

        for (i in 0..numSegments) {
            val x = i * dx

            // Calculate Shear V(x)
            var vx = when (input.supportType) {
                BeamSupportType.CANTILEVER -> -(input.udlKnM * (input.spanM - x) + input.pointLoads.filter { it.positionM >= x }.sumOf { it.magnitudeKn })
                else -> ra - (input.udlKnM * x) - input.pointLoads.filter { it.positionM <= x }.sumOf { it.magnitudeKn }
            }

            // Calculate Moment M(x)
            var mx = when (input.supportType) {
                BeamSupportType.SIMPLY_SUPPORTED -> (ra * x) - (input.udlKnM * x.pow(2) / 2.0) - input.pointLoads.filter { it.positionM <= x }.sumOf { it.magnitudeKn * (x - it.positionM) }
                BeamSupportType.CANTILEVER -> -( (input.udlKnM * (input.spanM - x).pow(2) / 2.0) + input.pointLoads.filter { it.positionM >= x }.sumOf { it.magnitudeKn * (it.positionM - x) } )
                BeamSupportType.FIXED_FIXED -> ma + (ra * x) - (input.udlKnM * x.pow(2) / 2.0) - input.pointLoads.filter { it.positionM <= x }.sumOf { it.magnitudeKn * (x - it.positionM) }
                BeamSupportType.PROPPED_CANTILEVER -> ma + (ra * x) - (input.udlKnM * x.pow(2) / 2.0) - input.pointLoads.filter { it.positionM <= x }.sumOf { it.magnitudeKn * (x - it.positionM) }
            }

            // Calculate Deflection y(x) in mm (Simplified elastic Euler-Bernoulli integration)
            val deflectionM = when (input.supportType) {
                BeamSupportType.SIMPLY_SUPPORTED -> {
                    // delta = (w * x / (24 * E * I)) * (L^3 - 2*L*x^2 + x^3)
                    val wN_M = input.udlKnM * 1000.0
                    val udlDefl = (wN_M * x / (24.0 * eInN_M2 * iInM4)) * (input.spanM.pow(3) - 2.0 * input.spanM * x.pow(2) + x.pow(3))
                    udlDefl
                }
                BeamSupportType.CANTILEVER -> {
                    val wN_M = input.udlKnM * 1000.0
                    val cantDefl = (wN_M * x.pow(2) / (24.0 * eInN_M2 * iInM4)) * (x.pow(2) + 6.0 * input.spanM.pow(2) - 4.0 * input.spanM * x)
                    cantDefl
                }
                else -> {
                    val wN_M = input.udlKnM * 1000.0
                    (wN_M * x.pow(2) * (input.spanM - x).pow(2)) / (24.0 * eInN_M2 * iInM4 * 16.0)
                }
            }
            val deflectionMm = deflectionM * 1000.0

            if (abs(vx) > abs(maxV)) maxV = vx
            if (abs(mx) > abs(maxM)) maxM = mx
            if (abs(deflectionMm) > abs(maxDelta)) maxDelta = deflectionMm

            points.add(DiagramPoint(x, vx, mx, deflectionMm))
        }

        val maxAllowedSpanDeflection = (input.spanM * 1000.0) / 250.0 // L/250 limit
        val utilization = (abs(maxDelta) / maxAllowedSpanDeflection * 100.0).coerceIn(5.0, 100.0)

        val steps = listOf(
            "1. Boundary Conditions: ${input.supportType.label} (Span L = ${input.spanM}m)",
            "2. Total Applied Loading: UDL = ${input.udlKnM} kN/m (${String.format("%.1f", totalUdlLoad)} kN) + Point Loads = ${String.format("%.1f", totalPointLoad)} kN",
            "3. Support Reactions: R_A = ${String.format("%.2f", ra)} kN | R_B = ${String.format("%.2f", rb)} kN | M_A = ${String.format("%.2f", ma)} kNm",
            "4. Peak Internal Shear: |V_max| = ${String.format("%.2f", abs(maxV))} kN",
            "5. Peak Bending Moment: |M_max| = ${String.format("%.2f", abs(maxM))} kNm",
            "6. Maximum Elastic Deflection: δ_max = ${String.format("%.2f", abs(maxDelta))} mm (Allowable L/250 = ${String.format("%.1f", maxAllowedSpanDeflection)} mm)"
        )

        return AnalysisResult(
            supportType = input.supportType,
            reactionA_Kn = ra,
            reactionB_Kn = rb,
            reactionMomentA_KNm = ma,
            maxShearKn = abs(maxV),
            maxMomentKNm = abs(maxM),
            maxDeflectionMm = abs(maxDelta),
            diagramPoints = points,
            memberUtilizationPercent = utilization,
            steps = steps
        )
    }

    private fun AnalysisInput.positionMToProppedCoeff(a: Double, l: Double): Double {
        val b = l - a
        return (b / (2.0 * l.pow(3))) * (3.0 * l.pow(2) - b.pow(2))
    }
}
