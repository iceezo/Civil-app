package com.example.reports

import com.example.cad.CADDrawingState
import com.example.data.BOQItemEntity
import com.example.data.ProjectEntity
import com.example.engine.BOQGeneratorEngine
import com.example.engine.StructuralBeamEngine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object EngineeringReportEngine {

    fun generateComprehensiveEngineeringReport(
        project: ProjectEntity,
        beamResult: StructuralBeamEngine.BeamResult?,
        drawingState: CADDrawingState?,
        boqItems: List<BOQItemEntity>,
        costBreakdown: BOQGeneratorEngine.CostBreakdown?
    ): String {
        val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.US)
        val currentDate = dateFormat.format(Date())

        val sb = StringBuilder()

        sb.append("================================================================================\n")
        sb.append("                       CIVIAI STRUCTURAL & CIVIL ENGINEERING REPORT            \n")
        sb.append("                             PRELIMINARY DESIGN DOSSIER                        \n")
        sb.append("================================================================================\n\n")

        sb.append("1.0 PROJECT INFORMATION\n")
        sb.append("--------------------------------------------------------------------------------\n")
        sb.append("Project Title:       ${project.name}\n")
        sb.append("Client Name:         ${if (project.clientName.isBlank()) "Private Developer" else project.clientName}\n")
        sb.append("Project Location:    ${project.location}\n")
        sb.append("Building Typology:   ${project.buildingType} (${project.numFloors} Storey)\n")
        sb.append("Governing Standard:  ${project.designCode}\n")
        sb.append("Unit System:         ${project.unitSystem}\n")
        sb.append("Date of Dossier:     $currentDate\n")
        sb.append("Plot Sizing:         ${project.plotWidth}m × ${project.plotLength}m (${project.plotWidth * project.plotLength} m²)\n")
        sb.append("Building Footprint:  ${project.buildingWidth}m × ${project.buildingLength}m (${project.buildingWidth * project.buildingLength} m²)\n")
        sb.append("Soil Bearing (q_a):  ${project.soilBearingCapacity} kPa\n\n")

        sb.append("2.0 DESIGN BASIS & CODES OF PRACTICE\n")
        sb.append("--------------------------------------------------------------------------------\n")
        sb.append("- Concrete Design:   Eurocode 2: Design of concrete structures (BS EN 1992-1-1)\n")
        sb.append("- Actions on Struct: Eurocode 1: Actions on structures (BS EN 1991-1-1)\n")
        sb.append("- Foundation Design: Eurocode 7: Geotechnical design (BS EN 1997-1)\n")
        sb.append("- Materials Grade:   Concrete f_ck = 25.0 MPa (C20/25), Steel f_yk = 500.0 MPa\n")
        sb.append("- Partial Factors:   γ_G = 1.35 (Unfavourable Permanent), γ_Q = 1.50 (Variable)\n")
        sb.append("- Material Factors:  γ_c = 1.50 (Concrete), γ_s = 1.15 (Reinforcing Steel)\n\n")

        if (beamResult != null) {
            sb.append("3.0 STRUCTURAL BEAM DESIGN SUMMARY\n")
            sb.append("--------------------------------------------------------------------------------\n")
            sb.append("Section Dimensions:  230 mm × 450 mm (Effective Depth d = ${beamResult.effectiveDepthDMm.toInt()} mm)\n")
            sb.append("Design Factored UDL: ${String.format(Locale.US, "%.2f", beamResult.designUdlKnM)} kN/m\n")
            sb.append("Design Bending M_Ed: ${String.format(Locale.US, "%.2f", beamResult.maxMomentMedKNm)} kNm\n")
            sb.append("Design Shear V_Ed:   ${String.format(Locale.US, "%.2f", beamResult.maxShearVedKn)} kN\n")
            sb.append("Required Steel Area: ${beamResult.requiredSteelAreaAsReqMm2.toInt()} mm² (Min: ${beamResult.minSteelAreaAsMinMm2.toInt()} mm²)\n")
            sb.append("Tension Rebar (Bot): ${beamResult.suggestedBottomBars}\n")
            sb.append("Hanger Rebar (Top):  ${beamResult.suggestedTopHangerBars}\n")
            sb.append("Shear Links (Ties):  ${beamResult.suggestedLinks}\n")
            sb.append("Concrete Shear Cap:  V_Rd,c = ${String.format(Locale.US, "%.2f", beamResult.shearConcreteCapacityVrdcKn)} kN (${beamResult.shearStatus})\n")
            sb.append("Deflection Check:    ${beamResult.deflectionStatus}\n")
            sb.append("Design Status:       [ ${beamResult.overallStatus} ]\n\n")
        }

        if (drawingState != null && drawingState.rooms.isNotEmpty()) {
            sb.append("4.0 ARCHITECTURAL SPACE & COLUMN SCHEDULE\n")
            sb.append("--------------------------------------------------------------------------------\n")
            sb.append("Total Rooms Configured: ${drawingState.rooms.size} spaces\n")
            for (room in drawingState.rooms) {
                sb.append(String.format(Locale.US, "- %-25s : %4.1fm × %4.1fm (%5.1f m²)\n", room.name, room.widthM, room.lengthM, room.areaSqm))
            }
            sb.append("Columns Positioned:     ${drawingState.columns.size} Grid Columns (230x230mm)\n")
            sb.append("Main Beam Centerlines:  ${drawingState.beams.size} Spans\n\n")
        }

        if (costBreakdown != null) {
            sb.append("5.0 BILL OF QUANTITIES (BOQ) & FINANCIAL SUMMARY\n")
            sb.append("--------------------------------------------------------------------------------\n")
            sb.append(String.format(Locale.US, "Direct Materials & Works Subtotal:   %s %,.2f\n", costBreakdown.currencySymbol, costBreakdown.subtotal))
            sb.append(String.format(Locale.US, "Preliminaries & Site Setup (3%%):     %s %,.2f\n", costBreakdown.currencySymbol, costBreakdown.preliminariesAndInsurance))
            sb.append(String.format(Locale.US, "Estimated Labour & Plant (25%%):      %s %,.2f\n", costBreakdown.currencySymbol, costBreakdown.labourAndEquipment))
            sb.append(String.format(Locale.US, "Material Wastage Buffer (5%%):        %s %,.2f\n", costBreakdown.currencySymbol, costBreakdown.materialWastageAllowance))
            sb.append(String.format(Locale.US, "Contractor Overhead & Margin (10%%):  %s %,.2f\n", costBreakdown.currencySymbol, costBreakdown.contractorOverheadAndProfit))
            sb.append(String.format(Locale.US, "Contingency Allowance (5%%):          %s %,.2f\n", costBreakdown.currencySymbol, costBreakdown.contingencyAllowance))
            sb.append(String.format(Locale.US, "Value Added Tax / Statutory (7.5%%):  %s %,.2f\n", costBreakdown.currencySymbol, costBreakdown.valueAddedTaxVat))
            sb.append("--------------------------------------------------------------------------------\n")
            sb.append(String.format(Locale.US, "GRAND TOTAL ESTIMATED PROJECT COST:  %s %,.2f\n", costBreakdown.currencySymbol, costBreakdown.grandTotalEstimatedCost))
            sb.append("================================================================================\n\n")
        }

        sb.append("6.0 STATUTORY ENGINEERING DISCLAIMER & STAMP\n")
        sb.append("--------------------------------------------------------------------------------\n")
        sb.append("THIS REPORT WAS PRODUCED WITH CIVIAI ASSISTANT AS A PRELIMINARY DECISION-SUPPORT\n")
        sb.append("DESIGN TOOL. ALL CALCULATIONS, SECTION DIMENSIONS, REINFORCEMENT DETAILS, AND\n")
        sb.append("COST PROJECTIONS MUST BE INDEPENDENTLY VETTED, CERTIFIED, AND SEALED BY A LICENSED\n")
        sb.append("REGISTERED STRUCTURAL/CIVIL ENGINEER (PE / COREN / ICE) BEFORE ISSUANCE FOR\n")
        sb.append("CONSTRUCTION OR REGULATORY BUILDING APPROVAL.\n\n")
        sb.append("[ SEAL / SIGNATURE PLACEHOLDER ] _______________________________\n")
        sb.append("Engineer of Record: Engr. CiviAI Automated System, MICE, MNSE\n")

        return sb.toString()
    }
}
