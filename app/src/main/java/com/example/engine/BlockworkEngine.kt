package com.example.engine

import kotlin.math.ceil

object BlockworkEngine {

    data class BlockType(
        val name: String,
        val lengthMm: Double,
        val heightMm: Double,
        val thicknessMm: Double,
        val isHollow: Boolean = true,
        val description: String
    )

    val PRESET_BLOCKS = listOf(
        BlockType("9-inch Sandcrete (225mm)", 450.0, 225.0, 225.0, true, "Standard external load-bearing wall"),
        BlockType("6-inch Sandcrete (150mm)", 450.0, 225.0, 150.0, true, "Standard internal partition wall"),
        BlockType("5-inch Sandcrete (125mm)", 450.0, 225.0, 125.0, true, "Non-loadbearing partition"),
        BlockType("4-inch Sandcrete (100mm)", 450.0, 200.0, 100.0, true, "Light partition / dwarf walls"),
        BlockType("Standard Red Brick (9x4.5x3 in)", 230.0, 75.0, 110.0, false, "Burnt clay solid face brick")
    )

    data class Opening(
        val label: String,
        val widthM: Double,
        val heightM: Double,
        val count: Int
    ) {
        val totalAreaSqm: Double get() = widthM * heightM * count
    }

    data class BlockworkResult(
        val grossWallAreaSqm: Double,
        val totalDeductionAreaSqm: Double,
        val netWallAreaSqm: Double,
        val wallThicknessMm: Double,
        val blockTypeLabel: String,
        val singleBlockFaceAreaSqm: Double,
        val theoreticalBlockCount: Int,
        val totalBlocksWithWastage: Int,
        val wastagePercentage: Double,
        val mortarVolumeM3: Double,
        val cementBagsForMortar50kg: Int,
        val sandForMortarM3: Double,
        val sandForMortarTonnes: Double,
        val assumptions: List<String>,
        val steps: List<String>
    )

    fun calculateBlockwork(
        wallLengthM: Double,
        wallHeightM: Double,
        blockType: BlockType = PRESET_BLOCKS[0],
        openings: List<Opening> = emptyList(),
        wastagePercent: Double = 5.0,
        mortarJointMm: Double = 12.5,
        mortarMixRatio: Pair<Double, Double> = Pair(1.0, 6.0) // 1:6 cement to sand
    ): BlockworkResult {
        val grossArea = wallLengthM * wallHeightM
        val totalDeductions = openings.sumOf { it.totalAreaSqm }
        val netArea = (grossArea - totalDeductions).coerceAtLeast(0.1)

        // Single block face area including mortar joint
        val effectiveLengthM = (blockType.lengthMm + mortarJointMm) / 1000.0
        val effectiveHeightM = (blockType.heightMm + mortarJointMm) / 1000.0
        val effectiveBlockFaceArea = effectiveLengthM * effectiveHeightM

        // Blocks per m2 = 1 / effectiveBlockFaceArea
        val blocksPerSqm = 1.0 / effectiveBlockFaceArea
        val rawBlocks = netArea * blocksPerSqm
        val totalBlocks = ceil(rawBlocks * (1.0 + (wastagePercent / 100.0))).toInt()

        // Mortar volume calculation
        // Mortar volume = (Total wall gross volume - Total block actual solid/hollow volume)
        // Empirical rule for standard sandcrete masonry: ~0.015 - 0.025 m3 mortar per m2 of wall
        val wallThicknessM = blockType.thicknessMm / 1000.0
        val netWallVolume = netArea * wallThicknessM
        val singleBlockSolidVol = (blockType.lengthMm / 1000.0) * (blockType.heightMm / 1000.0) * (blockType.thicknessMm / 1000.0) * (if (blockType.isHollow) 0.60 else 1.0)
        val allBlocksSolidVol = rawBlocks * singleBlockSolidVol
        val wetMortarVol = (netWallVolume - allBlocksSolidVol).coerceAtLeast(netArea * 0.018)

        // Mortar dry volume factor is 1.33
        val dryMortarVol = wetMortarVol * 1.33 * (1.0 + (wastagePercent / 100.0))
        val mortarTotalParts = mortarMixRatio.first + mortarMixRatio.second
        val cementMortarVol = dryMortarVol * (mortarMixRatio.first / mortarTotalParts)
        val sandMortarVol = dryMortarVol * (mortarMixRatio.second / mortarTotalParts)

        val cementBags = ceil(cementMortarVol / 0.0347).toInt()
        val sandTonnes = sandMortarVol * 1.60

        val assumptions = listOf(
            "Block type: ${blockType.name} (${blockType.lengthMm.toInt()}x${blockType.thicknessMm.toInt()}x${blockType.heightMm.toInt()} mm).",
            "Mortar bedding joint thickness = $mortarJointMm mm.",
            "Mortar mix ratio = 1:${mortarMixRatio.second.toInt()} (Cement : Sand).",
            "Wastage allowance = $wastagePercent%.",
            "Dry volume conversion factor for masonry mortar = 1.33."
        )

        val calculationSteps = listOf(
            "1. Gross Wall Area: A_gross = $wallLengthM m × $wallHeightM m = ${String.format("%.2f", grossArea)} m²",
            "2. Total Openings Deductions: A_deduct = ${String.format("%.2f", totalDeductions)} m² across ${openings.size} openings",
            "3. Net Masonry Area: A_net = $grossArea - $totalDeductions = ${String.format("%.2f", netArea)} m²",
            "4. Effective Unit Area with ${mortarJointMm}mm Joint: (${blockType.lengthMm + mortarJointMm}mm × ${blockType.heightMm + mortarJointMm}mm) = ${String.format("%.4f", effectiveBlockFaceArea)} m²",
            "5. Blocks per m²: 1 / ${String.format("%.4f", effectiveBlockFaceArea)} = ${String.format("%.2f", blocksPerSqm)} blocks/m²",
            "6. Total Blocks (+${wastagePercent}% wastage): ${String.format("%.1f", rawBlocks)} × (1 + ${wastagePercent/100}) = $totalBlocks Units",
            "7. Mortar Volume: V_mortar = ${String.format("%.3f", wetMortarVol)} m³ (Dry Vol: ${String.format("%.3f", dryMortarVol)} m³)",
            "8. Mortar Cement: $cementBags Bags (50kg) | Sand: ${String.format("%.2f", sandMortarVol)} m³ (${String.format("%.2f", sandTonnes)} tonnes)"
        )

        return BlockworkResult(
            grossWallAreaSqm = grossArea,
            totalDeductionAreaSqm = totalDeductions,
            netWallAreaSqm = netArea,
            wallThicknessMm = blockType.thicknessMm,
            blockTypeLabel = blockType.name,
            singleBlockFaceAreaSqm = effectiveBlockFaceArea,
            theoreticalBlockCount = ceil(rawBlocks).toInt(),
            totalBlocksWithWastage = totalBlocks,
            wastagePercentage = wastagePercent,
            mortarVolumeM3 = wetMortarVol,
            cementBagsForMortar50kg = cementBags,
            sandForMortarM3 = sandMortarVol,
            sandForMortarTonnes = sandTonnes,
            assumptions = assumptions,
            steps = calculationSteps
        )
    }
}
