package com.example.engine

import kotlin.math.*

object RebarEngine {

    data class BBSItem(
        val barMark: String,
        val memberType: String, // Beam, Column, Footing, Slab, Staircase
        val diameterMm: Int,
        val numberOfMembers: Int,
        val barsPerMember: Int,
        val totalBars: Int,
        val shapeCode: String, // 00: Straight, 21: L-Bend, 37: U-Hook, 51: Rectangular Link
        val lengthA_Mm: Double,
        val lengthB_Mm: Double = 0.0,
        val lengthC_Mm: Double = 0.0,
        val totalCuttingLengthM: Double,
        val unitWeightKgM: Double,
        val totalWeightKg: Double
    )

    data class BBSResult(
        val items: List<BBSItem>,
        val totalWeightKg: Double,
        val totalWeightTonnes: Double,
        val weightByDiameter: Map<Int, Double>,
        val standardCommercial12mBarsReq: Int,
        val lapLength40dMm: Int,
        val lapLength50dMm: Int
    )

    fun calculateUnitWeightKgM(diameterMm: Int): Double {
        // Universal civil formula: w = D^2 / 162 (kg/m)
        return diameterMm.toDouble().pow(2) / 162.0
    }

    fun calculateCuttingLengthM(shapeCode: String, aMm: Double, bMm: Double = 0.0, cMm: Double = 0.0, diaMm: Int = 16): Double {
        return when (shapeCode) {
            "00" -> aMm / 1000.0 // Straight
            "21" -> (aMm + bMm - (0.5 * diaMm)) / 1000.0 // L-Bend
            "37" -> (aMm + (2.0 * bMm)) / 1000.0 // U-Hook
            "51" -> (2.0 * (aMm + bMm) + (24.0 * diaMm)) / 1000.0 // Rectangular link with hooks
            else -> aMm / 1000.0
        }
    }

    fun generateStandardProjectBBS(
        beamSpanM: Double = 5.0,
        columnHeightM: Double = 3.0,
        footingSizeM: Double = 1.4,
        slabSpanM: Double = 4.0
    ): BBSResult {
        val items = mutableListOf<BBSItem>()

        // 1. Beam Main Bottom Bars (T16)
        val beamBotCut = calculateCuttingLengthM("21", beamSpanM * 1000.0 + 300.0, 300.0, diaMm = 16)
        items.add(
            BBSItem(
                barMark = "01",
                memberType = "Main Beams",
                diameterMm = 16,
                numberOfMembers = 4,
                barsPerMember = 3,
                totalBars = 12,
                shapeCode = "21 (L-Bar)",
                lengthA_Mm = beamSpanM * 1000.0 + 300.0,
                lengthB_Mm = 300.0,
                totalCuttingLengthM = beamBotCut,
                unitWeightKgM = calculateUnitWeightKgM(16),
                totalWeightKg = 12 * beamBotCut * calculateUnitWeightKgM(16)
            )
        )

        // 2. Beam Top Hanger Bars (T12)
        val beamTopCut = calculateCuttingLengthM("00", beamSpanM * 1000.0 + 200.0, diaMm = 12)
        items.add(
            BBSItem(
                barMark = "02",
                memberType = "Main Beams (Top)",
                diameterMm = 12,
                numberOfMembers = 4,
                barsPerMember = 2,
                totalBars = 8,
                shapeCode = "00 (Straight)",
                lengthA_Mm = beamSpanM * 1000.0 + 200.0,
                totalCuttingLengthM = beamTopCut,
                unitWeightKgM = calculateUnitWeightKgM(12),
                totalWeightKg = 8 * beamTopCut * calculateUnitWeightKgM(12)
            )
        )

        // 3. Beam Shear Links (T8)
        val linkCut = calculateCuttingLengthM("51", 180.0, 400.0, diaMm = 8)
        val linksPerBeam = (beamSpanM / 0.175).toInt()
        items.add(
            BBSItem(
                barMark = "03",
                memberType = "Beam Stirrups",
                diameterMm = 8,
                numberOfMembers = 4,
                barsPerMember = linksPerBeam,
                totalBars = 4 * linksPerBeam,
                shapeCode = "51 (Rect. Link)",
                lengthA_Mm = 180.0,
                lengthB_Mm = 400.0,
                totalCuttingLengthM = linkCut,
                unitWeightKgM = calculateUnitWeightKgM(8),
                totalWeightKg = (4 * linksPerBeam) * linkCut * calculateUnitWeightKgM(8)
            )
        )

        // 4. Columns Vertical Starters & Mains (T16)
        val colCut = calculateCuttingLengthM("21", columnHeightM * 1000.0 + 600.0, 300.0, diaMm = 16)
        items.add(
            BBSItem(
                barMark = "04",
                memberType = "Columns (Longitudinal)",
                diameterMm = 16,
                numberOfMembers = 6,
                barsPerMember = 4,
                totalBars = 24,
                shapeCode = "21 (L-Starter)",
                lengthA_Mm = columnHeightM * 1000.0 + 600.0,
                lengthB_Mm = 300.0,
                totalCuttingLengthM = colCut,
                unitWeightKgM = calculateUnitWeightKgM(16),
                totalWeightKg = 24 * colCut * calculateUnitWeightKgM(16)
            )
        )

        // 5. Footing Mesh (T12)
        val footCut = calculateCuttingLengthM("37", footingSizeM * 1000.0 - 100.0, 150.0, diaMm = 12)
        items.add(
            BBSItem(
                barMark = "05",
                memberType = "Pad Footings Base Mesh",
                diameterMm = 12,
                numberOfMembers = 6,
                barsPerMember = 16,
                totalBars = 96,
                shapeCode = "37 (U-Bar)",
                lengthA_Mm = footingSizeM * 1000.0 - 100.0,
                lengthB_Mm = 150.0,
                totalCuttingLengthM = footCut,
                unitWeightKgM = calculateUnitWeightKgM(12),
                totalWeightKg = 96 * footCut * calculateUnitWeightKgM(12)
            )
        )

        val totalWeight = items.sumOf { it.totalWeightKg }
        val weightMap = items.groupBy { it.diameterMm }.mapValues { entry -> entry.value.sumOf { it.totalWeightKg } }
        val totalLengthAllBarsM = items.sumOf { it.totalBars * it.totalCuttingLengthM }
        val bars12mReq = ceil(totalLengthAllBarsM / 11.5).toInt() // with 500mm cutting offcut

        return BBSResult(
            items = items,
            totalWeightKg = totalWeight,
            totalWeightTonnes = totalWeight / 1000.0,
            weightByDiameter = weightMap,
            standardCommercial12mBarsReq = bars12mReq,
            lapLength40dMm = 16 * 40,
            lapLength50dMm = 16 * 50
        )
    }
}
