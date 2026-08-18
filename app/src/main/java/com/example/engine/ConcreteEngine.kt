package com.example.engine

import kotlin.math.ceil

object ConcreteEngine {

    data class MixRatio(
        val name: String,
        val cementPart: Double,
        val sandPart: Double,
        val aggregatePart: Double,
        val characteristicStrengthMPa: String,
        val recommendedUsage: String
    )

    val PRESET_MIXES = listOf(
        MixRatio("1:2:4 (Nominal Class 20/25)", 1.0, 2.0, 4.0, "20-25 MPa", "General beams, slabs, columns, pad footings"),
        MixRatio("1:1.5:3 (Class 25/30)", 1.0, 1.5, 3.0, "25-30 MPa", "Heavy columns, retaining walls, cantilever beams"),
        MixRatio("1:1:2 (Class 30/37)", 1.0, 1.0, 2.0, "30-37 MPa", "Water-retaining structures, pre-stressed elements"),
        MixRatio("1:3:6 (Mass/Blinding)", 1.0, 3.0, 6.0, "10-15 MPa", "Foundation blinding, mass concrete, oversite paths"),
        MixRatio("1:4:8 (Lean Concrete)", 1.0, 4.0, 8.0, "7-10 MPa", "Trench fill, non-structural backfill")
    )

    data class ConcreteResult(
        val wetVolumeM3: Double,
        val dryVolumeM3: Double,
        val wastagePercentage: Double,
        val totalVolumeWithWastageM3: Double,
        val cementBags50kg: Int,
        val cementWeightKg: Double,
        val sandVolumeM3: Double,
        val sandWeightTonnes: Double,
        val aggregateVolumeM3: Double,
        val aggregateWeightTonnes: Double,
        val estimatedWaterLitres: Double,
        val mixRatioLabel: String,
        val targetStrength: String,
        val assumptions: List<String>,
        val stepByStepFormulas: List<String>
    )

    fun calculateConcrete(
        lengthM: Double,
        widthM: Double,
        depthM: Double,
        mixRatio: MixRatio = PRESET_MIXES[0],
        wastagePercent: Double = 5.0,
        densitySandKgM3: Double = 1600.0,
        densityAggregateKgM3: Double = 1450.0,
        waterCementRatio: Double = 0.50
    ): ConcreteResult {
        val wetVolume = lengthM * widthM * depthM
        val wastageFactor = 1.0 + (wastagePercent / 100.0)
        val wetVolWithWastage = wetVolume * wastageFactor

        // Concrete dry volume factor is universally 1.54 to 1.57 due to shrinkage and void fill
        val dryVolumeConstant = 1.54
        val dryVolume = wetVolWithWastage * dryVolumeConstant

        val totalParts = mixRatio.cementPart + mixRatio.sandPart + mixRatio.aggregatePart

        val cementVolume = dryVolume * (mixRatio.cementPart / totalParts)
        val sandVolume = dryVolume * (mixRatio.sandPart / totalParts)
        val aggregateVolume = dryVolume * (mixRatio.aggregatePart / totalParts)

        // 1 bag of 50kg cement = approx 0.0347 m3 (density ~1440 kg/m3)
        val volumePerBag = 0.0347
        val cementBags = ceil(cementVolume / volumePerBag).toInt()
        val cementKg = cementBags * 50.0

        val sandTonnes = (sandVolume * densitySandKgM3) / 1000.0
        val aggregateTonnes = (aggregateVolume * densityAggregateKgM3) / 1000.0
        val waterLitres = cementKg * waterCementRatio

        val assumptions = listOf(
            "Dry volume factor = $dryVolumeConstant (accounts for void fill and wet-to-dry volumetric shrinkage).",
            "50kg Portland cement bag volume = 0.0347 m³ (nominal density: 1440 kg/m³).",
            "Sand bulk density = $densitySandKgM3 kg/m³, Coarse aggregate bulk density = $densityAggregateKgM3 kg/m³.",
            "Water-Cement Ratio (w/c) = $waterCementRatio by mass.",
            "Wastage allowance included = $wastagePercent%."
        )

        val steps = listOf(
            "1. Wet Section Volume: V_wet = L ($lengthM m) × W ($widthM m) × D ($depthM m) = ${String.format("%.3f", wetVolume)} m³",
            "2. Adjusted Volume with Wastage: V_waste = V_wet × (1 + $wastagePercent/100) = ${String.format("%.3f", wetVolWithWastage)} m³",
            "3. Dry Volume Factor (1.54): V_dry = V_waste × 1.54 = ${String.format("%.3f", dryVolume)} m³",
            "4. Sum of Mix Parts (${mixRatio.cementPart} + ${mixRatio.sandPart} + ${mixRatio.aggregatePart}) = $totalParts",
            "5. Cement Required: V_cem = $dryVolume × (${mixRatio.cementPart}/$totalParts) = ${String.format("%.3f", cementVolume)} m³ → $cementBags Bags (50kg)",
            "6. Fine Aggregate (Sand): V_sand = $dryVolume × (${mixRatio.sandPart}/$totalParts) = ${String.format("%.3f", sandVolume)} m³ (${String.format("%.2f", sandTonnes)} tonnes)",
            "7. Coarse Aggregate (Granite): V_agg = $dryVolume × (${mixRatio.aggregatePart}/$totalParts) = ${String.format("%.3f", aggregateVolume)} m³ (${String.format("%.2f", aggregateTonnes)} tonnes)",
            "8. Mixing Water: Water = Cement ($cementKg kg) × $waterCementRatio = ${String.format("%.1f", waterLitres)} Litres"
        )

        return ConcreteResult(
            wetVolumeM3 = wetVolume,
            dryVolumeM3 = dryVolume,
            wastagePercentage = wastagePercent,
            totalVolumeWithWastageM3 = wetVolWithWastage,
            cementBags50kg = cementBags,
            cementWeightKg = cementKg,
            sandVolumeM3 = sandVolume,
            sandWeightTonnes = sandTonnes,
            aggregateVolumeM3 = aggregateVolume,
            aggregateWeightTonnes = aggregateTonnes,
            estimatedWaterLitres = waterLitres,
            mixRatioLabel = mixRatio.name,
            targetStrength = mixRatio.characteristicStrengthMPa,
            assumptions = assumptions,
            stepByStepFormulas = steps
        )
    }
}
