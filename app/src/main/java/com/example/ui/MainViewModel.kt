package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AIEngineerService
import com.example.cad.CADDrawingState
import com.example.cad.DXFExportEngine
import com.example.cad.FloorPlanPresetGenerator
import com.example.data.*
import com.example.engine.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val projectDao = db.projectDao()
    private val calcDao = db.calculationDao()
    private val drawingDao = db.drawingDao()
    private val boqDao = db.boqDao()
    private val chatDao = db.chatDao()

    val allProjects = projectDao.getAllProjects().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _activeProjectId = MutableStateFlow<Long>(1L)
    val activeProjectId: StateFlow<Long> = _activeProjectId.asStateFlow()

    private val _activeProject = MutableStateFlow(
        ProjectEntity(
            id = 1L,
            name = "Lekki Residential Bungalow",
            clientName = "Horizon Estates Ltd",
            location = "Lekki Phase 2, Lagos",
            buildingType = "3-Bedroom Contemporary Bungalow",
            numFloors = 1,
            plotWidth = 30.0,
            plotLength = 20.0,
            buildingWidth = 14.5,
            buildingLength = 12.5,
            soilBearingCapacity = 150.0,
            designCode = "Eurocode 2 (EN 1992)",
            currency = "NGN (₦)",
            unitSystem = "Metric (m, mm, kN)",
            progress = 65
        )
    )
    val activeProject: StateFlow<ProjectEntity> = _activeProject.asStateFlow()

    // Calculation states
    private val _beamInput = MutableStateFlow(StructuralBeamEngine.BeamInput())
    val beamInput = _beamInput.asStateFlow()
    private val _beamResult = MutableStateFlow(StructuralBeamEngine.calculateBeam(StructuralBeamEngine.BeamInput()))
    val beamResult = _beamResult.asStateFlow()

    private val _columnInput = MutableStateFlow(ColumnDesignEngine.ColumnInput())
    val columnInput = _columnInput.asStateFlow()
    private val _columnResult = MutableStateFlow(ColumnDesignEngine.calculateColumn(ColumnDesignEngine.ColumnInput()))
    val columnResult = _columnResult.asStateFlow()

    private val _foundationInput = MutableStateFlow(FoundationEngine.FoundationInput())
    val foundationInput = _foundationInput.asStateFlow()
    private val _foundationResult = MutableStateFlow(FoundationEngine.calculateFoundation(FoundationEngine.FoundationInput()))
    val foundationResult = _foundationResult.asStateFlow()

    private val _slabInput = MutableStateFlow(SlabDesignEngine.SlabInput())
    val slabInput = _slabInput.asStateFlow()
    private val _slabResult = MutableStateFlow(SlabDesignEngine.calculateSlab(SlabDesignEngine.SlabInput()))
    val slabResult = _slabResult.asStateFlow()

    private val _stairInput = MutableStateFlow(StaircaseEngine.StairInput())
    val stairInput = _stairInput.asStateFlow()
    private val _stairResult = MutableStateFlow(StaircaseEngine.calculateStaircase(StaircaseEngine.StairInput()))
    val stairResult = _stairResult.asStateFlow()

    private val _concreteVolumeM3 = MutableStateFlow(10.0)
    val concreteVolumeM3 = _concreteVolumeM3.asStateFlow()
    private val _concreteMixRatio = MutableStateFlow(ConcreteEngine.PRESET_MIXES[0])
    val concreteMixRatio = _concreteMixRatio.asStateFlow()
    private val _concreteResult = MutableStateFlow(ConcreteEngine.calculateConcrete(10.0, 1.0, 1.0))
    val concreteResult = _concreteResult.asStateFlow()

    private val _blockworkLengthM = MutableStateFlow(25.0)
    val blockworkLengthM = _blockworkLengthM.asStateFlow()
    private val _blockworkHeightM = MutableStateFlow(3.0)
    val blockworkHeightM = _blockworkHeightM.asStateFlow()
    private val _blockType = MutableStateFlow(BlockworkEngine.PRESET_BLOCKS[0])
    val blockType = _blockType.asStateFlow()
    private val _blockworkResult = MutableStateFlow(BlockworkEngine.calculateBlockwork(25.0, 3.0))
    val blockworkResult = _blockworkResult.asStateFlow()

    private val _bbsResult = MutableStateFlow(RebarEngine.generateStandardProjectBBS())
    val bbsResult = _bbsResult.asStateFlow()

    private val _analysisInput = MutableStateFlow(StructuralAnalysisEngine.AnalysisInput())
    val analysisInput = _analysisInput.asStateFlow()
    private val _analysisResult = MutableStateFlow(StructuralAnalysisEngine.solveBeam(StructuralAnalysisEngine.AnalysisInput()))
    val analysisResult = _analysisResult.asStateFlow()

    // CAD Drawing state
    private val _cadDrawing = MutableStateFlow(FloorPlanPresetGenerator.generate3BedroomBungalow())
    val cadDrawing = _cadDrawing.asStateFlow()

    // BOQ & Costing
    private val _boqItems = MutableStateFlow<List<BOQItemEntity>>(emptyList())
    val boqItems = _boqItems.asStateFlow()

    private val _costBreakdown = MutableStateFlow<BOQGeneratorEngine.CostBreakdown?>(null)
    val costBreakdown = _costBreakdown.asStateFlow()

    // AI Chat
    private val _chatMessages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    val chatMessages = _chatMessages.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking = _isAiThinking.asStateFlow()

    private val _selectedAiRole = MutableStateFlow(AIEngineerService.AIAgentRole.CIVIL_GENERAL)
    val selectedAiRole = _selectedAiRole.asStateFlow()

    // Real-time Engineering Calculation & Audit Logs
    private val _calculationLogs = MutableStateFlow<List<CalculationLogEntry>>(
        listOf(
            CalculationLogEntry(
                module = "SYSTEM_INITIALIZER",
                level = LogLevel.INFO,
                title = "CiviAI Engineering Runtime Initialized",
                details = "Initialized Eurocode 2 (EN 1992) / BS 8110 / ACI 318 deterministic calculation kernels. Safety factor checks active."
            ),
            CalculationLogEntry(
                module = "BEAM_SOLVER",
                level = LogLevel.CALCULATION,
                title = "Singly Reinforced Beam Analysis Executed",
                details = "Span: 4.50m, gk: 12.0 kN/m, qk: 8.0 kN/m -> Design Moment Med = 61.3 kNm, Ast required = 458 mm² (Selected 3T16 = 603 mm²).",
                formula = "Med = 1.35*Gk + 1.5*Qk; K = Med/(b*d²*fck); z = d*(0.5 + sqrt(0.25 - K/1.134)); Ast = Med/(0.87*fyk*z)",
                isSafetyCompliant = true
            ),
            CalculationLogEntry(
                module = "COLUMN_SOLVER",
                level = LogLevel.CALCULATION,
                title = "Short Axially Loaded Column Verification",
                details = "Section: 230x230mm, NEd = 450 kN, fck = 25 MPa, fyk = 500 MPa -> Selected 4T16 (804 mm²). Utilization = 61.2% (PASS).",
                formula = "NRd = 0.567*fck*Ac + 0.87*fyk*Asc >= NEd",
                isSafetyCompliant = true
            ),
            CalculationLogEntry(
                module = "FOUNDATION_SOLVER",
                level = LogLevel.CALCULATION,
                title = "Pad Footing Geotechnical Bearing Check",
                details = "Service Load = 450 kN, Allowable Soil Bearing qa = 150 kPa -> Area Req = 3.00 m² (Provided 1.80m × 1.80m = 3.24 m²). Bearing Check: 138.9 kPa < 150 kPa (PASS).",
                formula = "Area_req = P_service / qa; Punching Shear v_Ed = V_Ed / (u1 * d)",
                isSafetyCompliant = true
            )
        )
    )
    val calculationLogs = _calculationLogs.asStateFlow()

    // Snackbar / Toast event
    private val _userMessage = MutableSharedFlow<String>()
    val userMessage = _userMessage.asSharedFlow()

    init {
        initDatabase()
        refreshBOQ()
    }

    fun addLog(entry: CalculationLogEntry) {
        _calculationLogs.value = listOf(entry) + _calculationLogs.value.take(99)
    }

    fun clearLogs() {
        _calculationLogs.value = emptyList()
    }

    private fun initDatabase() {
        viewModelScope.launch {
            val existing = projectDao.getProjectByIdDirect(1L)
            if (existing == null) {
                val initialProj = _activeProject.value
                projectDao.insertProject(initialProj)
            } else {
                _activeProject.value = existing
            }

            // Seed initial welcome message
            val welcomeMsg = ChatMessageEntity(
                projectId = 1L,
                sender = "AI",
                agentType = "CiviAI Principal Engineer",
                message = "Welcome to CiviAI Engineer! I am your AI-powered civil & structural engineering assistant. I can help you design reinforced concrete beams, columns, foundations, calculate concrete batching and blockwork quantities, generate complete BOQs, and draft 2D floor plans with DXF CAD export. How can I assist with your project today?"
            )
            _chatMessages.value = listOf(welcomeMsg)
        }
    }

    fun selectProject(project: ProjectEntity) {
        _activeProject.value = project
        _activeProjectId.value = project.id
        refreshBOQ()
    }

    fun createProject(name: String, client: String, location: String, buildingType: String, soilBearing: Double, designCode: String, currency: String) {
        viewModelScope.launch {
            val newProj = ProjectEntity(
                name = name,
                clientName = client,
                location = location,
                buildingType = buildingType,
                soilBearingCapacity = soilBearing,
                designCode = designCode,
                currency = currency
            )
            val newId = projectDao.insertProject(newProj)
            val inserted = newProj.copy(id = newId)
            _activeProject.value = inserted
            _activeProjectId.value = newId
            refreshBOQ()
            _userMessage.emit("Project '$name' created successfully.")
        }
    }

    // Calculations triggers
    fun updateBeam(input: StructuralBeamEngine.BeamInput) {
        _beamInput.value = input
        val result = StructuralBeamEngine.calculateBeam(input)
        _beamResult.value = result
        val isPassed = result.overallStatus == "PASS"
        addLog(
            CalculationLogEntry(
                module = "BEAM_SOLVER",
                level = if (isPassed) LogLevel.CALCULATION else LogLevel.WARNING,
                title = "Beam Analysis: Span ${input.spanM}m",
                details = "Moment Med: ${String.format("%.1f", result.maxMomentMedKNm)} kNm, Shear Ved: ${String.format("%.1f", result.maxShearVedKn)} kN. Ast Req: ${result.requiredSteelAreaAsReqMm2.toInt()} mm², Provided: ${result.providedBottomAreaMm2.toInt()} mm² (${result.suggestedBottomBars}). Deflection: ${result.deflectionStatus}.",
                formula = "Med = 1.35*Gk + 1.5*Qk; Ast = Med / (0.87 * fyk * z)",
                isSafetyCompliant = isPassed
            )
        )
    }

    fun updateColumn(input: ColumnDesignEngine.ColumnInput) {
        _columnInput.value = input
        val result = ColumnDesignEngine.calculateColumn(input)
        _columnResult.value = result
        val isPassed = result.status == "PASS"
        addLog(
            CalculationLogEntry(
                module = "COLUMN_SOLVER",
                level = if (isPassed) LogLevel.CALCULATION else LogLevel.WARNING,
                title = "Column Check: ${input.widthMm.toInt()}x${input.depthMm.toInt()} mm",
                details = "Load NEd: ${result.designAxialLoadNedKn.toInt()} kN. Section: ${input.widthMm.toInt()}x${input.depthMm.toInt()} mm. Capacity NRd: ${result.axialCapacityNrdKn.toInt()} kN. Steel: ${result.suggestedLongitudinalBars}.",
                formula = "NRd = 0.567*fck*Ac + 0.87*fyk*Asc >= NEd",
                isSafetyCompliant = isPassed
            )
        )
    }

    fun updateFoundation(input: FoundationEngine.FoundationInput) {
        _foundationInput.value = input
        val result = FoundationEngine.calculateFoundation(input)
        _foundationResult.value = result
        val isPassed = result.actualBearingPressureKpa <= input.soilBearingCapacityQaKpa
        addLog(
            CalculationLogEntry(
                module = "FOUNDATION_SOLVER",
                level = if (isPassed) LogLevel.CALCULATION else LogLevel.WARNING,
                title = "Foundation Check: ${input.type.label}",
                details = "Provided: ${String.format("%.2f", result.providedWidthM)}m × ${String.format("%.2f", result.providedLengthM)}m. Actual Soil Stress: ${String.format("%.1f", result.actualBearingPressureKpa)} kPa vs Allowable: ${input.soilBearingCapacityQaKpa} kPa. Punching: ${result.punchingStatus}.",
                formula = "Area_req = P_service / qa; Punching Shear = Ved / (u1 * d)",
                isSafetyCompliant = isPassed
            )
        )
    }

    fun updateSlab(input: SlabDesignEngine.SlabInput) {
        _slabInput.value = input
        val result = SlabDesignEngine.calculateSlab(input)
        _slabResult.value = result
        val isPassed = result.deflectionStatus == "PASS"
        addLog(
            CalculationLogEntry(
                module = "SLAB_SOLVER",
                level = if (isPassed) LogLevel.CALCULATION else LogLevel.WARNING,
                title = "Slab Design: ${input.shortSpanLxM}m × ${input.longSpanLyM}m",
                details = "Type: ${result.slabType.label}. Thickness: ${input.slabThicknessMm.toInt()} mm. Short Span Rebar: ${result.suggestedRebarShortSpan}, Long Span: ${result.suggestedRebarLongSpan}. Deflection: ${result.deflectionStatus}.",
                formula = "ly/lx ratio check; Ast = M / (0.87 * fyk * z)",
                isSafetyCompliant = isPassed
            )
        )
    }

    fun updateStaircase(input: StaircaseEngine.StairInput) {
        _stairInput.value = input
        val result = StaircaseEngine.calculateStaircase(input)
        _stairResult.value = result
        val isComfortable = result.formulaComfortCheck2RplusG in 580.0..660.0
        val comfortLabel = if (isComfortable) "OPTIMAL" else "CHECK DIMENSIONS"
        addLog(
            CalculationLogEntry(
                module = "STAIR_SOLVER",
                level = if (isComfortable) LogLevel.CALCULATION else LogLevel.WARNING,
                title = "Staircase Geometry Check",
                details = "Riser: ${String.format("%.1f", result.actualRiserMm)} mm, Going: ${String.format("%.1f", result.actualGoingMm)} mm, Angle: ${String.format("%.1f", result.pitchAngleDeg)}°. Comfort: $comfortLabel (2R+G = ${result.formulaComfortCheck2RplusG.toInt()} mm). Steel: ${result.suggestedMainSteel}.",
                formula = "2R + G = 600..640 mm (Building Code Standard)",
                isSafetyCompliant = isComfortable
            )
        )
    }

    fun updateConcrete(volumeM3: Double, mix: ConcreteEngine.MixRatio, wastage: Double = 5.0) {
        _concreteVolumeM3.value = volumeM3
        _concreteMixRatio.value = mix
        val result = ConcreteEngine.calculateConcrete(volumeM3, 1.0, 1.0, mix, wastage)
        _concreteResult.value = result
        addLog(
            CalculationLogEntry(
                module = "BATCHING_SOLVER",
                level = LogLevel.CALCULATION,
                title = "Concrete Batching Matrix Calculated",
                details = "Volume: $volumeM3 m³ (${mix.name}). Cement: ${result.cementBags50kg} bags (50kg), Sand: ${String.format("%.2f", result.sandWeightTonnes)} tons, Granite: ${String.format("%.2f", result.aggregateWeightTonnes)} tons, Water: ${result.estimatedWaterLitres.toInt()} L.",
                formula = "Dry Volume = Wet Volume * 1.54; Batch proportions = ratio_i / sum(ratios) * Dry Vol",
                isSafetyCompliant = true
            )
        )
    }

    fun updateBlockwork(lengthM: Double, heightM: Double, block: BlockworkEngine.BlockType, wastage: Double = 5.0) {
        _blockworkLengthM.value = lengthM
        _blockworkHeightM.value = heightM
        _blockType.value = block
        val result = BlockworkEngine.calculateBlockwork(lengthM, heightM, block, emptyList(), wastage)
        _blockworkResult.value = result
        addLog(
            CalculationLogEntry(
                module = "BLOCKWORK_SOLVER",
                level = LogLevel.CALCULATION,
                title = "Masonry Quantity Takeoff Calculated",
                details = "Wall: ${lengthM}m × ${heightM}m (${block.name}). Units: ${result.totalBlocksWithWastage} blocks, Mortar: ${result.cementBagsForMortar50kg} cement bags, ${String.format("%.2f", result.sandForMortarTonnes)} tons sand.",
                formula = "Blocks/m² = 10; Mortar Volume = Wall Area * Joint Thickness * Joint Width",
                isSafetyCompliant = true
            )
        )
    }

    fun updateAnalysis(input: StructuralAnalysisEngine.AnalysisInput) {
        _analysisInput.value = input
        val result = StructuralAnalysisEngine.solveBeam(input)
        _analysisResult.value = result
        addLog(
            CalculationLogEntry(
                module = "FINITE_ANALYSIS",
                level = LogLevel.CALCULATION,
                title = "Beam Finite Element & Shear/Moment Analysis",
                details = "Span: ${input.spanM}m (${input.supportType.label}), Support Reactions: R_A = ${String.format("%.1f", result.reactionA_Kn)} kN, R_B = ${String.format("%.1f", result.reactionB_Kn)} kN. Max Shear: ${String.format("%.1f", result.maxShearKn)} kN, Max Moment: ${String.format("%.1f", result.maxMomentKNm)} kNm.",
                formula = "Equilibrium: Sum(Fy) = 0, Sum(M_A) = 0 -> V(x) = RA - Integral(w(x)dx), M(x) = Integral(V(x)dx)",
                isSafetyCompliant = true
            )
        )
    }

    // CAD functions
    fun loadCADPreset(presetType: Int) {
        val state = when (presetType) {
            1 -> FloorPlanPresetGenerator.generate3BedroomBungalow()
            2 -> FloorPlanPresetGenerator.generate4BedroomDuplex()
            3 -> FloorPlanPresetGenerator.generateCompact2Bedroom()
            else -> FloorPlanPresetGenerator.generate3BedroomBungalow()
        }
        _cadDrawing.value = state
    }

    fun toggleCADLayer(layerName: String) {
        val current = _cadDrawing.value
        _cadDrawing.value = when (layerName) {
            "GRID" -> current.copy(showGrid = !current.showGrid)
            "COLUMNS" -> current.copy(showColumns = !current.showColumns)
            "BEAMS" -> current.copy(showBeams = !current.showBeams)
            "DIMENSIONS" -> current.copy(showDimensions = !current.showDimensions)
            "FURNITURE" -> current.copy(showFurniture = !current.showFurniture)
            "DOORS_WINDOWS" -> current.copy(showDoorsWindows = !current.showDoorsWindows)
            else -> current
        }
    }

    fun getDXFExportString(): String {
        return DXFExportEngine.generateDXF(_cadDrawing.value)
    }

    // BOQ functions
    fun refreshBOQ() {
        val project = _activeProject.value
        val (items, breakdown) = BOQGeneratorEngine.generateStandardProjectBOQ(
            projectId = project.id,
            buildingType = project.buildingType,
            plotAreaSqm = project.plotWidth * project.plotLength,
            buildingAreaSqm = project.buildingWidth * project.buildingLength,
            currencyCode = project.currency
        )
        _boqItems.value = items
        _costBreakdown.value = breakdown
    }

    fun updateBOQItemRate(itemNumber: String, newRate: Double) {
        val updated = _boqItems.value.map { item ->
            if (item.itemNumber == itemNumber) {
                item.copy(rate = newRate, amount = item.quantity * newRate)
            } else item
        }
        _boqItems.value = updated
        val subtotal = updated.sumOf { it.amount }
        val currency = _costBreakdown.value?.currencySymbol ?: "₦"
        _costBreakdown.value = BOQGeneratorEngine.CostBreakdown(
            subtotal = subtotal,
            preliminariesAndInsurance = subtotal * 0.03,
            labourAndEquipment = subtotal * 0.25,
            materialWastageAllowance = subtotal * 0.05,
            contractorOverheadAndProfit = subtotal * 0.10,
            contingencyAllowance = subtotal * 0.05,
            valueAddedTaxVat = (subtotal * 1.18) * 0.075,
            grandTotalEstimatedCost = subtotal * 1.25,
            currencySymbol = currency
        )
    }

    // AI Engineering Chat
    fun selectAiRole(role: AIEngineerService.AIAgentRole) {
        _selectedAiRole.value = role
    }

    fun sendAiPrompt(userText: String) {
        if (userText.isBlank()) return

        val userMsg = ChatMessageEntity(
            projectId = _activeProjectId.value,
            sender = "USER",
            agentType = _selectedAiRole.value.badge,
            message = userText
        )
        _chatMessages.value = _chatMessages.value + userMsg
        _isAiThinking.value = true

        viewModelScope.launch {
            val projectSummary = "Project: ${_activeProject.value.name}, Type: ${_activeProject.value.buildingType}, Standard: ${_activeProject.value.designCode}, Soil Qa: ${_activeProject.value.soilBearingCapacity} kPa"
            val aiResponse = AIEngineerService.consultEngineer(
                userQuery = userText,
                agentRole = _selectedAiRole.value,
                contextProjectSummary = projectSummary
            )

            val aiMsg = ChatMessageEntity(
                projectId = _activeProjectId.value,
                sender = "AI",
                agentType = aiResponse.role.title,
                message = aiResponse.text
            )

            _chatMessages.value = _chatMessages.value + aiMsg
            _isAiThinking.value = false
        }
    }
}
