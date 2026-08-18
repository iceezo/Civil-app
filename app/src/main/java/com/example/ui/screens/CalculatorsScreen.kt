package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.engine.*
import com.example.ui.MainViewModel
import com.example.ui.components.BeamCrossSectionSketch
import com.example.ui.components.StructuralDiagramCanvas

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("RC Beam", "Column", "Footing", "Slab", "Concrete", "Blockwork", "BBS", "Analysis")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

        when (selectedTab) {
            0 -> BeamCalculatorTab(viewModel)
            1 -> ColumnCalculatorTab(viewModel)
            2 -> FootingCalculatorTab(viewModel)
            3 -> SlabCalculatorTab(viewModel)
            4 -> ConcreteCalculatorTab(viewModel)
            5 -> BlockworkCalculatorTab(viewModel)
            6 -> BBSCalculatorTab(viewModel)
            7 -> AnalysisCalculatorTab(viewModel)
        }
    }
}

// -------------------------------------------------------------
// TAB 1: RC BEAM DESIGN
// -------------------------------------------------------------
@Composable
private fun BeamCalculatorTab(viewModel: MainViewModel) {
    val input by viewModel.beamInput.collectAsState()
    val result by viewModel.beamResult.collectAsState()

    var spanText by remember { mutableStateOf(input.spanM.toString()) }
    var widthText by remember { mutableStateOf(input.widthMm.toInt().toString()) }
    var depthText by remember { mutableStateOf(input.depthMm.toInt().toString()) }
    var deadLoadText by remember { mutableStateOf(input.deadLoadGkKnM.toString()) }
    var liveLoadText by remember { mutableStateOf(input.liveLoadQkKnM.toString()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "BEAM DESIGN PARAMETERS (Eurocode 2 / ACI 318)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = spanText,
                            onValueChange = {
                                spanText = it
                                it.toDoubleOrNull()?.let { v -> viewModel.updateBeam(input.copy(spanM = v)) }
                            },
                            label = { Text("Span (m)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = widthText,
                            onValueChange = {
                                widthText = it
                                it.toDoubleOrNull()?.let { v -> viewModel.updateBeam(input.copy(widthMm = v)) }
                            },
                            label = { Text("Width b (mm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = depthText,
                            onValueChange = {
                                depthText = it
                                it.toDoubleOrNull()?.let { v -> viewModel.updateBeam(input.copy(depthMm = v)) }
                            },
                            label = { Text("Depth h (mm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = deadLoadText,
                            onValueChange = {
                                deadLoadText = it
                                it.toDoubleOrNull()?.let { v -> viewModel.updateBeam(input.copy(deadLoadGkKnM = v)) }
                            },
                            label = { Text("Dead Gk (kN/m)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = liveLoadText,
                            onValueChange = {
                                liveLoadText = it
                                it.toDoubleOrNull()?.let { v -> viewModel.updateBeam(input.copy(liveLoadQkKnM = v)) }
                            },
                            label = { Text("Live Qk (kN/m)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Cross Section Sketch
        item {
            BeamCrossSectionSketch(
                widthMm = input.widthMm,
                depthMm = input.depthMm,
                bottomBarsText = result.suggestedBottomBars,
                topBarsText = result.suggestedTopHangerBars,
                linksText = result.suggestedLinks
            )
        }

        // Calculation Results Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("DESIGN CALCULATION OUTPUTS", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Surface(
                            color = if (result.overallStatus == "PASS") Color(0xFF10B981) else Color(0xFFEF4444),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = result.overallStatus,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    ResultRow("Design Load (wd)", "${String.format("%.2f", result.designUdlKnM)} kN/m")
                    ResultRow("Design Bending Moment (M_Ed)", "${String.format("%.2f", result.maxMomentMedKNm)} kNm")
                    ResultRow("Design Shear Force (V_Ed)", "${String.format("%.2f", result.maxShearVedKn)} kN")
                    ResultRow("Effective Depth (d)", "${result.effectiveDepthDMm.toInt()} mm")
                    ResultRow("Section K-factor", "${String.format("%.4f", result.kValue)} (K' limit = ${result.kPrime})")
                    ResultRow("Required Steel Area (As,req)", "${result.requiredSteelAreaAsReqMm2.toInt()} mm²")
                    ResultRow("Suggested Main Rebar", result.suggestedBottomBars, highlight = true)
                    ResultRow("Shear Links (Asw/s)", result.suggestedLinks)
                    ResultRow("Deflection Span/d", result.deflectionStatus)

                    if (result.warningMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "⚠️ ${result.warningMessage}",
                            color = Color(0xFFDC2626),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Governing Equations
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("MATHEMATICAL DERIVATIONS & CODE FORMULAS", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    result.governingEquations.forEach { eq ->
                        Text(text = eq, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 2: RC COLUMN DESIGN
// -------------------------------------------------------------
@Composable
private fun ColumnCalculatorTab(viewModel: MainViewModel) {
    val input by viewModel.columnInput.collectAsState()
    val result by viewModel.columnResult.collectAsState()

    var widthText by remember { mutableStateOf(input.widthMm.toInt().toString()) }
    var depthText by remember { mutableStateOf(input.depthMm.toInt().toString()) }
    var heightText by remember { mutableStateOf(input.clearHeightM.toString()) }
    var axialGkText by remember { mutableStateOf(input.axialDeadLoadGkKn.toInt().toString()) }
    var axialQkText by remember { mutableStateOf(input.axialLiveLoadQkKn.toInt().toString()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("COLUMN SECTION & LOAD PARAMETERS", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = widthText,
                            onValueChange = {
                                widthText = it
                                it.toDoubleOrNull()?.let { v -> viewModel.updateColumn(input.copy(widthMm = v)) }
                            },
                            label = { Text("Width b (mm)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = depthText,
                            onValueChange = {
                                depthText = it
                                it.toDoubleOrNull()?.let { v -> viewModel.updateColumn(input.copy(depthMm = v)) }
                            },
                            label = { Text("Depth h (mm)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = heightText,
                            onValueChange = {
                                heightText = it
                                it.toDoubleOrNull()?.let { v -> viewModel.updateColumn(input.copy(clearHeightM = v)) }
                            },
                            label = { Text("Height L (m)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = axialGkText,
                            onValueChange = {
                                axialGkText = it
                                it.toDoubleOrNull()?.let { v -> viewModel.updateColumn(input.copy(axialDeadLoadGkKn = v)) }
                            },
                            label = { Text("Axial Dead Gk (kN)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = axialQkText,
                            onValueChange = {
                                axialQkText = it
                                it.toDoubleOrNull()?.let { v -> viewModel.updateColumn(input.copy(axialLiveLoadQkKn = v)) }
                            },
                            label = { Text("Axial Live Qk (kN)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("COLUMN CAPACITY & REBAR PROPOSAL", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    ResultRow("Factored Axial Load (N_Ed)", "${String.format("%.1f", result.designAxialLoadNedKn)} kN")
                    ResultRow("Effective Slenderness (λ)", "${String.format("%.1f", result.slendernessRatioLambda)} (${if (result.isSlender) "Slender" else "Short / Stocky"})")
                    ResultRow("Required Steel (Asc,req)", "${result.requiredSteelAscMm2.toInt()} mm² (Min: ${result.minSteelAscMinMm2.toInt()} mm²)")
                    ResultRow("Suggested Longitudinal Steel", result.suggestedLongitudinalBars, highlight = true)
                    ResultRow("Lateral Ties / Links", result.suggestedLinks)
                    ResultRow("Ultimate Axial Capacity (N_Rd)", "${String.format("%.1f", result.axialCapacityNrdKn)} kN (Status: ${result.status})")
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 3: PAD & STRIP FOOTING
// -------------------------------------------------------------
@Composable
private fun FootingCalculatorTab(viewModel: MainViewModel) {
    val input by viewModel.foundationInput.collectAsState()
    val result by viewModel.foundationResult.collectAsState()

    var loadGkText by remember { mutableStateOf(input.columnLoadGkKn.toInt().toString()) }
    var loadQkText by remember { mutableStateOf(input.columnLoadQkKn.toInt().toString()) }
    var soilBearingText by remember { mutableStateOf(input.soilBearingCapacityQaKpa.toInt().toString()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("FOUNDATION GEOTECHNICAL SIZING", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = loadGkText,
                            onValueChange = {
                                loadGkText = it
                                it.toDoubleOrNull()?.let { v -> viewModel.updateFoundation(input.copy(columnLoadGkKn = v)) }
                            },
                            label = { Text("Col Load Gk (kN)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = loadQkText,
                            onValueChange = {
                                loadQkText = it
                                it.toDoubleOrNull()?.let { v -> viewModel.updateFoundation(input.copy(columnLoadQkKn = v)) }
                            },
                            label = { Text("Col Load Qk (kN)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = soilBearingText,
                        onValueChange = {
                            soilBearingText = it
                            it.toDoubleOrNull()?.let { v -> viewModel.updateFoundation(input.copy(soilBearingCapacityQaKpa = v)) }
                        },
                        label = { Text("Soil Bearing Capacity qa (kPa / kN/m²)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("PAD FOOTING DESIGN SUMMARY", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    ResultRow("Required Plan Area", "${String.format("%.2f", result.requiredAreaSqm)} m²")
                    ResultRow("Provided Base Dimensions", "${result.providedLengthM}m × ${result.providedWidthM}m × ${result.providedThicknessMm.toInt()}mm", highlight = true)
                    ResultRow("Actual Soil Pressure", "${String.format("%.1f", result.actualBearingPressureKpa)} kPa ≤ ${input.soilBearingCapacityQaKpa.toInt()} kPa")
                    ResultRow("Critical Bending Moment", "${String.format("%.1f", result.bendingMomentKNm)} kNm")
                    ResultRow("Bottom Rebar Mesh", result.suggestedMesh, highlight = true)
                    ResultRow("Punching Shear Check", result.punchingStatus)
                    ResultRow("Excavation Volume", "${String.format("%.2f", result.excavationVolumeM3)} m³")
                    ResultRow("Concrete Volume", "${String.format("%.2f", result.footingConcreteM3)} m³ (Blinding: ${String.format("%.2f", result.blindingConcreteM3)} m³)")
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 4: FLOOR SLAB DESIGN
// -------------------------------------------------------------
@Composable
private fun SlabCalculatorTab(viewModel: MainViewModel) {
    val input by viewModel.slabInput.collectAsState()
    val result by viewModel.slabResult.collectAsState()

    var lxText by remember { mutableStateOf(input.shortSpanLxM.toString()) }
    var lyText by remember { mutableStateOf(input.longSpanLyM.toString()) }
    var thicknessText by remember { mutableStateOf(input.slabThicknessMm.toInt().toString()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("SLAB SPAN & THICKNESS", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = lxText,
                            onValueChange = {
                                lxText = it
                                it.toDoubleOrNull()?.let { v -> viewModel.updateSlab(input.copy(shortSpanLxM = v)) }
                            },
                            label = { Text("Short Span Lx (m)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = lyText,
                            onValueChange = {
                                lyText = it
                                it.toDoubleOrNull()?.let { v -> viewModel.updateSlab(input.copy(longSpanLyM = v)) }
                            },
                            label = { Text("Long Span Ly (m)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = thicknessText,
                        onValueChange = {
                            thicknessText = it
                            it.toDoubleOrNull()?.let { v -> viewModel.updateSlab(input.copy(slabThicknessMm = v)) }
                        },
                        label = { Text("Slab Thickness h (mm)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("SLAB DESIGN & QUANTITIES", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    ResultRow("Slab Classification", "${result.slabType.label} (Ly/Lx = ${String.format("%.2f", result.aspectRatioLyLx)})")
                    ResultRow("Design Load (wd)", "${String.format("%.2f", result.designLoadWdKnM2)} kN/m²")
                    ResultRow("Short Span Steel (Bottom)", result.suggestedRebarShortSpan, highlight = true)
                    ResultRow("Long Span Steel (Transverse)", result.suggestedRebarLongSpan)
                    ResultRow("Concrete Volume", "${String.format("%.2f", result.concreteVolumeM3)} m³")
                    ResultRow("Formwork Area", "${String.format("%.1f", result.formworkAreaSqm)} m²")
                    ResultRow("Total Steel Weight", "${String.format("%.1f", result.totalSteelWeightKg)} kg")
                    ResultRow("Deflection Check", result.deflectionStatus)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 5: CONCRETE MIX BATCHING
// -------------------------------------------------------------
@Composable
private fun ConcreteCalculatorTab(viewModel: MainViewModel) {
    val vol by viewModel.concreteVolumeM3.collectAsState()
    val mix by viewModel.concreteMixRatio.collectAsState()
    val result by viewModel.concreteResult.collectAsState()

    var volText by remember { mutableStateOf(vol.toString()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("CONCRETE BATCHING VOLUME & MIX RATIO", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = volText,
                        onValueChange = {
                            volText = it
                            it.toDoubleOrNull()?.let { v -> viewModel.updateConcrete(v, mix) }
                        },
                        label = { Text("Wet Concrete Volume (m³)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Select Concrete Mix Ratio:", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))

                    ConcreteEngine.PRESET_MIXES.forEach { preset ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(
                                selected = mix.name == preset.name,
                                onClick = { viewModel.updateConcrete(volText.toDoubleOrNull() ?: 10.0, preset) }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(preset.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(preset.recommendedUsage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("MATERIAL QUANTITIES (Dry Volume Factor = 1.54)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    ResultRow("Dry Concrete Volume", "${String.format("%.2f", result.dryVolumeM3)} m³")
                    ResultRow("Portland Cement (50kg Bags)", "${result.cementBags50kg} Bags (${String.format("%.0f", result.cementWeightKg)} kg)", highlight = true)
                    ResultRow("Fine Aggregate (Sand)", "${String.format("%.2f", result.sandVolumeM3)} m³ (${String.format("%.2f", result.sandWeightTonnes)} tonnes)")
                    ResultRow("Coarse Aggregate (Granite)", "${String.format("%.2f", result.aggregateVolumeM3)} m³ (${String.format("%.2f", result.aggregateWeightTonnes)} tonnes)")
                    ResultRow("Estimated Mixing Water", "${String.format("%.0f", result.estimatedWaterLitres)} Litres (w/c = 0.50)")
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 6: BLOCKWORK & MORTAR
// -------------------------------------------------------------
@Composable
private fun BlockworkCalculatorTab(viewModel: MainViewModel) {
    val length by viewModel.blockworkLengthM.collectAsState()
    val height by viewModel.blockworkHeightM.collectAsState()
    val blockType by viewModel.blockType.collectAsState()
    val result by viewModel.blockworkResult.collectAsState()

    var lengthText by remember { mutableStateOf(length.toString()) }
    var heightText by remember { mutableStateOf(height.toString()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("WALL DIMENSIONS & BLOCK SIZES", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = lengthText,
                            onValueChange = {
                                lengthText = it
                                it.toDoubleOrNull()?.let { v -> viewModel.updateBlockwork(v, height, blockType) }
                            },
                            label = { Text("Wall Length (m)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = heightText,
                            onValueChange = {
                                heightText = it
                                it.toDoubleOrNull()?.let { v -> viewModel.updateBlockwork(length, v, blockType) }
                            },
                            label = { Text("Wall Height (m)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Select Block Type:", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))

                    BlockworkEngine.PRESET_BLOCKS.forEach { block ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(
                                selected = blockType.name == block.name,
                                onClick = { viewModel.updateBlockwork(lengthText.toDoubleOrNull() ?: 25.0, heightText.toDoubleOrNull() ?: 3.0, block) }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(block.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(block.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("MASONRY & MORTAR QUANTITIES", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    ResultRow("Net Wall Area", "${String.format("%.2f", result.netWallAreaSqm)} m²")
                    ResultRow("Total Blocks Required (+5% Waste)", "${result.totalBlocksWithWastage} Units", highlight = true)
                    ResultRow("Mortar Volume", "${String.format("%.3f", result.mortarVolumeM3)} m³")
                    ResultRow("Cement for Mortar (1:6)", "${result.cementBagsForMortar50kg} Bags (50kg)")
                    ResultRow("Sand for Mortar", "${String.format("%.2f", result.sandForMortarM3)} m³ (${String.format("%.2f", result.sandForMortarTonnes)} tonnes)")
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 7: BAR BENDING SCHEDULE (BBS)
// -------------------------------------------------------------
@Composable
private fun BBSCalculatorTab(viewModel: MainViewModel) {
    val bbs by viewModel.bbsResult.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("BAR BENDING SCHEDULE TOTALS", style = MaterialTheme.typography.labelSmall, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                    Text(
                        text = "${String.format("%.2f", bbs.totalWeightTonnes)} TONNES REBAR",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color(0xFFFBBF24),
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Total 12m Standard Commercial Lengths Required: ${bbs.standardCommercial12mBarsReq} Bars", style = MaterialTheme.typography.bodySmall, color = Color.White)
                    Text("Lap Lengths: 40d = ${bbs.lapLength40dMm}mm | 50d = ${bbs.lapLength50dMm}mm", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                }
            }
        }

        item {
            Text("ITEMIZED BAR SCHEDULE", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }

        items(bbs.items.size) { i ->
            val item = bbs.items[i]
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Mark ${item.barMark} : ${item.memberType}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("T${item.diameterMm} | Shape ${item.shapeCode} | Cut L = ${String.format("%.2f", item.totalCuttingLengthM)}m", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${item.totalBars} Bars", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("${String.format("%.1f", item.totalWeightKg)} kg", style = MaterialTheme.typography.bodySmall, color = Color(0xFF10B981))
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 8: STRUCTURAL ANALYSIS (SFD & BMD)
// -------------------------------------------------------------
@Composable
private fun AnalysisCalculatorTab(viewModel: MainViewModel) {
    val input by viewModel.analysisInput.collectAsState()
    val result by viewModel.analysisResult.collectAsState()

    var spanText by remember { mutableStateOf(input.spanM.toString()) }
    var udlText by remember { mutableStateOf(input.udlKnM.toString()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("BEAM ANALYSIS SOLVER", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = spanText,
                            onValueChange = {
                                spanText = it
                                it.toDoubleOrNull()?.let { v -> viewModel.updateAnalysis(input.copy(spanM = v)) }
                            },
                            label = { Text("Span L (m)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = udlText,
                            onValueChange = {
                                udlText = it
                                it.toDoubleOrNull()?.let { v -> viewModel.updateAnalysis(input.copy(udlKnM = v)) }
                            },
                            label = { Text("UDL (kN/m)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // SFD & BMD Interactive Diagrams
        item {
            StructuralDiagramCanvas(
                diagramPoints = result.diagramPoints,
                spanM = input.spanM,
                maxShearKn = result.maxShearKn,
                maxMomentKNm = result.maxMomentKNm
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("SUPPORT REACTIONS & INTERNAL FORCES", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    ResultRow("Reaction R_A", "${String.format("%.2f", result.reactionA_Kn)} kN")
                    ResultRow("Reaction R_B", "${String.format("%.2f", result.reactionB_Kn)} kN")
                    ResultRow("Max Bending Moment |M_max|", "${String.format("%.2f", result.maxMomentKNm)} kNm", highlight = true)
                    ResultRow("Max Shear Force |V_max|", "${String.format("%.2f", result.maxShearKn)} kN")
                    ResultRow("Max Elastic Deflection (δ_max)", "${String.format("%.2f", result.maxDeflectionMm)} mm (L/250 = ${String.format("%.1f", input.spanM * 1000 / 250)} mm)")
                }
            }
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String, highlight: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (highlight) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
