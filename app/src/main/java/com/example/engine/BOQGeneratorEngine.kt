package com.example.engine

import com.example.data.BOQItemEntity

object BOQGeneratorEngine {

    data class CostBreakdown(
        val subtotal: Double,
        val preliminariesAndInsurance: Double, // 3%
        val labourAndEquipment: Double, // 25% of subtotal
        val materialWastageAllowance: Double, // 5%
        val contractorOverheadAndProfit: Double, // 10%
        val contingencyAllowance: Double, // 5%
        val valueAddedTaxVat: Double, // 7.5%
        val grandTotalEstimatedCost: Double,
        val currencySymbol: String
    )

    fun generateStandardProjectBOQ(
        projectId: Long,
        buildingType: String = "3-Bedroom Residential Bungalow",
        plotAreaSqm: Double = 600.0,
        buildingAreaSqm: Double = 150.0,
        currencyCode: String = "NGN"
    ): Pair<List<BOQItemEntity>, CostBreakdown> {
        val rateMultiplier = when {
            currencyCode.contains("USD") || currencyCode.contains("$") -> 0.00065
            currencyCode.contains("GBP") || currencyCode.contains("£") -> 0.00052
            currencyCode.contains("EUR") || currencyCode.contains("€") -> 0.00060
            currencyCode.contains("GHS") || currencyCode.contains("GH₵") -> 0.0098
            currencyCode.contains("KES") || currencyCode.contains("KSh") -> 0.085
            currencyCode.contains("INR") || currencyCode.contains("₹") -> 0.055
            else -> 1.0 // NGN default rates
        }

        fun r(ngnRate: Double): Double = ngnRate * rateMultiplier

        val items = listOf(
            // SECTION 1: PRELIMINARIES
            BOQItemEntity(
                projectId = projectId,
                section = "1.0 PRELIMINARIES & GENERAL",
                itemNumber = "1.01",
                description = "Site installation, signboard, temporary water supply and electrical hookup",
                unit = "Item",
                quantity = 1.0,
                rate = r(450000.0),
                amount = r(450000.0)
            ),
            BOQItemEntity(
                projectId = projectId,
                section = "1.0 PRELIMINARIES & GENERAL",
                itemNumber = "1.02",
                description = "Setting out of building profile lines using total station / optical level",
                unit = "Item",
                quantity = 1.0,
                rate = r(180000.0),
                amount = r(180000.0)
            ),

            // SECTION 2: SUBSTRUCTURE & EARTHWORKS
            BOQItemEntity(
                projectId = projectId,
                section = "2.0 SUBSTRUCTURE & EARTHWORKS",
                itemNumber = "2.01",
                description = "Site clearing and topsoil excavation not exceeding 200mm depth",
                unit = "m²",
                quantity = plotAreaSqm * 0.75,
                rate = r(850.0),
                amount = (plotAreaSqm * 0.75) * r(850.0)
            ),
            BOQItemEntity(
                projectId = projectId,
                section = "2.0 SUBSTRUCTURE & EARTHWORKS",
                itemNumber = "2.02",
                description = "Excavation for foundation strip trenches and pad footings depth 1.2m",
                unit = "m³",
                quantity = 75.0,
                rate = r(3500.0),
                amount = 75.0 * r(3500.0)
            ),
            BOQItemEntity(
                projectId = projectId,
                section = "2.0 SUBSTRUCTURE & EARTHWORKS",
                itemNumber = "2.03",
                description = "50mm Mass concrete blinding (1:3:6) in trenches and column pits",
                unit = "m³",
                quantity = 6.5,
                rate = r(52000.0),
                amount = 6.5 * r(52000.0)
            ),
            BOQItemEntity(
                projectId = projectId,
                section = "2.0 SUBSTRUCTURE & EARTHWORKS",
                itemNumber = "2.04",
                description = "Reinforced concrete (1:2:4, C20/25) in pad footings and strip base",
                unit = "m³",
                quantity = 18.0,
                rate = r(85000.0),
                amount = 18.0 * r(85000.0)
            ),
            BOQItemEntity(
                projectId = projectId,
                section = "2.0 SUBSTRUCTURE & EARTHWORKS",
                itemNumber = "2.05",
                description = "9-inch (225mm) Solid foundation sandcrete blockwork filled with concrete",
                unit = "m²",
                quantity = 95.0,
                rate = r(9500.0),
                amount = 95.0 * r(9500.0)
            ),
            BOQItemEntity(
                projectId = projectId,
                section = "2.0 SUBSTRUCTURE & EARTHWORKS",
                itemNumber = "2.06",
                description = "300mm Hardcore filling, compacted in 150mm layers, with 1000g DPM membrane",
                unit = "m²",
                quantity = buildingAreaSqm,
                rate = r(4800.0),
                amount = buildingAreaSqm * r(4800.0)
            ),
            BOQItemEntity(
                projectId = projectId,
                section = "2.0 SUBSTRUCTURE & EARTHWORKS",
                itemNumber = "2.07",
                description = "150mm Reinforced concrete ground floor slab (1:2:4) with BRC A142 mesh",
                unit = "m²",
                quantity = buildingAreaSqm,
                rate = r(14500.0),
                amount = buildingAreaSqm * r(14500.0)
            ),

            // SECTION 3: SUPERSTRUCTURE FRAME & CONCRETE
            BOQItemEntity(
                projectId = projectId,
                section = "3.0 SUPERSTRUCTURE FRAME",
                itemNumber = "3.01",
                description = "Reinforced concrete (1:2:4, fck=25MPa) in columns, lintels, and roof beams",
                unit = "m³",
                quantity = 22.5,
                rate = r(92000.0),
                amount = 22.5 * r(92000.0)
            ),
            BOQItemEntity(
                projectId = projectId,
                section = "3.0 SUPERSTRUCTURE FRAME",
                itemNumber = "3.02",
                description = "High tensile deformed steel bars (T16, T12, T10, T8) bent and fixed in place",
                unit = "kg",
                quantity = 2850.0,
                rate = r(1350.0),
                amount = 2850.0 * r(1350.0)
            ),
            BOQItemEntity(
                projectId = projectId,
                section = "3.0 SUPERSTRUCTURE FRAME",
                itemNumber = "3.03",
                description = "Sawn timber formwork and scaffolding to sides and soffits of beams and columns",
                unit = "m²",
                quantity = 145.0,
                rate = r(4200.0),
                amount = 145.0 * r(4200.0)
            ),

            // SECTION 4: BLOCKWORK & WALLING
            BOQItemEntity(
                projectId = projectId,
                section = "4.0 BLOCKWORK & WALLING",
                itemNumber = "4.01",
                description = "9-inch (225mm) Hollow sandcrete blockwork in external superstructure walls",
                unit = "m²",
                quantity = 220.0,
                rate = r(7800.0),
                amount = 220.0 * r(7800.0)
            ),
            BOQItemEntity(
                projectId = projectId,
                section = "4.0 BLOCKWORK & WALLING",
                itemNumber = "4.02",
                description = "6-inch (150mm) Hollow sandcrete blockwork in internal partition walls",
                unit = "m²",
                quantity = 160.0,
                rate = r(6200.0),
                amount = 160.0 * r(6200.0)
            ),

            // SECTION 5: ROOFING & CEILING
            BOQItemEntity(
                projectId = projectId,
                section = "5.0 ROOFING & CEILING",
                itemNumber = "5.01",
                description = "Treated hardwood timber roof trusses (50x100mm, 50x150mm tie beams & rafters)",
                unit = "m²",
                quantity = buildingAreaSqm * 1.3,
                rate = r(9500.0),
                amount = (buildingAreaSqm * 1.3) * r(9500.0)
            ),
            BOQItemEntity(
                projectId = projectId,
                section = "5.0 ROOFING & CEILING",
                itemNumber = "5.02",
                description = "0.55mm Stone-coated / Longspan aluminium roofing sheets with ridges and valleys",
                unit = "m²",
                quantity = buildingAreaSqm * 1.3,
                rate = r(12500.0),
                amount = (buildingAreaSqm * 1.3) * r(12500.0)
            ),
            BOQItemEntity(
                projectId = projectId,
                section = "5.0 ROOFING & CEILING",
                itemNumber = "5.03",
                description = "Plaster of Paris (POP) ceiling with recessed LED lighting troughs",
                unit = "m²",
                quantity = buildingAreaSqm,
                rate = r(7500.0),
                amount = buildingAreaSqm * r(7500.0)
            ),

            // SECTION 6: DOORS, WINDOWS & METALWORKS
            BOQItemEntity(
                projectId = projectId,
                section = "6.0 DOORS & WINDOWS",
                itemNumber = "6.01",
                description = "Armoured security steel entrance doors (1200x2100mm) complete with ironmongery",
                unit = "Nr",
                quantity = 2.0,
                rate = r(240000.0),
                amount = 2.0 * r(240000.0)
            ),
            BOQItemEntity(
                projectId = projectId,
                section = "6.0 DOORS & WINDOWS",
                itemNumber = "6.02",
                description = "Flush wooden interior doors (900x2100mm) in hardwood frames",
                unit = "Nr",
                quantity = 8.0,
                rate = r(65000.0),
                amount = 8.0 * r(65000.0)
            ),
            BOQItemEntity(
                projectId = projectId,
                section = "6.0 DOORS & WINDOWS",
                itemNumber = "6.03",
                description = "Glazed aluminium sliding windows with burglar proof bars & flyscreens",
                unit = "m²",
                quantity = 35.0,
                rate = r(38000.0),
                amount = 35.0 * r(38000.0)
            ),

            // SECTION 7: FINISHES (PLASTERING, TILES, PAINT)
            BOQItemEntity(
                projectId = projectId,
                section = "7.0 FINISHES",
                itemNumber = "7.01",
                description = "15mm Cement-sand plaster (1:4) to internal and external wall surfaces",
                unit = "m²",
                quantity = 680.0,
                rate = r(2800.0),
                amount = 680.0 * r(2800.0)
            ),
            BOQItemEntity(
                projectId = projectId,
                section = "7.0 FINISHES",
                itemNumber = "7.02",
                description = "600x600mm Vitrified porcelain floor tiles on 25mm screeded bed",
                unit = "m²",
                quantity = buildingAreaSqm * 0.9,
                rate = r(8500.0),
                amount = (buildingAreaSqm * 0.9) * r(8500.0)
            ),
            BOQItemEntity(
                projectId = projectId,
                section = "7.0 FINISHES",
                itemNumber = "7.03",
                description = "Emulsion and gloss painting (1 primer coat + 2 finishing coats)",
                unit = "m²",
                quantity = 680.0,
                rate = r(1800.0),
                amount = 680.0 * r(1800.0)
            ),

            // SECTION 8: MECHANICAL & ELECTRICAL SERVICES
            BOQItemEntity(
                projectId = projectId,
                section = "8.0 SERVICES (M&E)",
                itemNumber = "8.01",
                description = "Electrical wiring, conduit piping, distribution board, switches & light fittings",
                unit = "Item",
                quantity = 1.0,
                rate = r(1650000.0),
                amount = r(1650000.0)
            ),
            BOQItemEntity(
                projectId = projectId,
                section = "8.0 SERVICES (M&E)",
                itemNumber = "8.02",
                description = "Plumbing pipework, sanitary fittings (water closets, washbasins, showers), water tanks",
                unit = "Item",
                quantity = 1.0,
                rate = r(1450000.0),
                amount = r(1450000.0)
            ),
            BOQItemEntity(
                projectId = projectId,
                section = "8.0 SERVICES (M&E)",
                itemNumber = "8.03",
                description = "Septic tank (3.0x1.5x2.0m) and soakaway pit construction with cover slabs",
                unit = "Item",
                quantity = 1.0,
                rate = r(850000.0),
                amount = r(850000.0)
            )
        )

        val subtotal = items.sumOf { it.amount }
        val prelims = subtotal * 0.03
        val labour = subtotal * 0.15
        val wastage = subtotal * 0.05
        val overhead = subtotal * 0.10
        val contingency = subtotal * 0.05
        val vat = (subtotal + prelims + overhead + contingency) * 0.075
        val grandTotal = subtotal + prelims + overhead + contingency + vat

        val breakdown = CostBreakdown(
            subtotal = subtotal,
            preliminariesAndInsurance = prelims,
            labourAndEquipment = labour,
            materialWastageAllowance = wastage,
            contractorOverheadAndProfit = overhead,
            contingencyAllowance = contingency,
            valueAddedTaxVat = vat,
            grandTotalEstimatedCost = grandTotal,
            currencySymbol = when {
                currencyCode.contains("USD") || currencyCode.contains("$") -> "$"
                currencyCode.contains("GBP") || currencyCode.contains("£") -> "£"
                currencyCode.contains("EUR") || currencyCode.contains("€") -> "€"
                currencyCode.contains("GHS") || currencyCode.contains("GH₵") -> "GH₵"
                currencyCode.contains("KES") || currencyCode.contains("KSh") -> "KSh"
                currencyCode.contains("INR") || currencyCode.contains("₹") -> "₹"
                else -> "₦"
            }
        )

        return Pair(items, breakdown)
    }
}
