package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.BOQItemEntity
import com.example.engine.UnitConversionEngine
import com.example.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BOQScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val items by viewModel.boqItems.collectAsState()
    val breakdown by viewModel.costBreakdown.collectAsState()
    val project by viewModel.activeProject.collectAsState()
    val context = LocalContext.current

    var selectedItemToEdit by remember { mutableStateOf<BOQItemEntity?>(null) }
    var editRateText by remember { mutableStateOf("") }
    var showExportConfirm by remember { mutableStateOf(false) }

    val currencies = listOf("NGN (₦)", "USD ($)", "GBP (£)", "EUR (€)", "GHS (GH₵)", "KES (KSh)", "INR (₹)")

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card with Grand Total
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("BILL OF QUANTITIES (BOQ)", style = MaterialTheme.typography.labelSmall, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                                Text(project.name, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            IconButton(
                                onClick = {
                                    // Copy CSV to clipboard
                                    val csv = generateCsv(items, breakdown)
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("BOQ CSV", csv)
                                    clipboard.setPrimaryClip(clip)
                                    showExportConfirm = true
                                }
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Export CSV", tint = Color(0xFF38BDF8))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = breakdown?.let { UnitConversionEngine.formatCurrency(it.grandTotalEstimatedCost, project.currency) } ?: "₦ 0.00",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color(0xFFFBBF24),
                            fontWeight = FontWeight.ExtraBold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Multi-currency Switcher
                        Text("Active Currency:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(currencies.size) { i ->
                                val cur = currencies[i]
                                SuggestionChip(
                                    onClick = {
                                        viewModel.createProject(
                                            name = project.name,
                                            client = project.clientName,
                                            location = project.location,
                                            buildingType = project.buildingType,
                                            soilBearing = project.soilBearingCapacity,
                                            designCode = project.designCode,
                                            currency = cur
                                        )
                                    },
                                    label = { Text(cur, color = if (project.currency == cur) Color(0xFF38BDF8) else Color.White) }
                                )
                            }
                        }
                    }
                }
            }

            // Financial Breakdown Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("COST BREAKDOWN & STATUTORY MARKUPS", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(10.dp))

                        breakdown?.let { b ->
                            CostRow("Direct Works Subtotal", UnitConversionEngine.formatCurrency(b.subtotal, project.currency))
                            CostRow("Preliminaries & Insurance (3%)", UnitConversionEngine.formatCurrency(b.preliminariesAndInsurance, project.currency))
                            CostRow("Labour & Equipment (25%)", UnitConversionEngine.formatCurrency(b.labourAndEquipment, project.currency))
                            CostRow("Material Wastage Allowance (5%)", UnitConversionEngine.formatCurrency(b.materialWastageAllowance, project.currency))
                            CostRow("Contractor Overhead & Margin (10%)", UnitConversionEngine.formatCurrency(b.contractorOverheadAndProfit, project.currency))
                            CostRow("Contingency Allowance (5%)", UnitConversionEngine.formatCurrency(b.contingencyAllowance, project.currency))
                            CostRow("Value Added Tax (7.5%)", UnitConversionEngine.formatCurrency(b.valueAddedTaxVat, project.currency))
                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                            CostRow("Grand Total Estimated Cost", UnitConversionEngine.formatCurrency(b.grandTotalEstimatedCost, project.currency), isTotal = true)
                        }
                    }
                }
            }

            // Sectioned Items
            val grouped = items.groupBy { it.section }
            grouped.forEach { (sectionName, sectionItems) ->
                item {
                    Text(
                        text = sectionName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(sectionItems.size) { index ->
                    val item = sectionItems[index]
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedItemToEdit = item
                                editRateText = item.rate.toString()
                            }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${item.itemNumber} - ${item.description}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = UnitConversionEngine.formatCurrency(item.amount, project.currency),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Qty: ${String.format("%.1f", item.quantity)} ${item.unit} @ ${UnitConversionEngine.formatCurrency(item.rate, project.currency)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Tap to edit rate",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Rate Edit Dialog
    selectedItemToEdit?.let { item ->
        AlertDialog(
            onDismissRequest = { selectedItemToEdit = null },
            title = { Text("Edit Unit Rate") },
            text = {
                Column {
                    Text(item.description, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editRateText,
                        onValueChange = { editRateText = it },
                        label = { Text("Unit Rate (${project.currency} per ${item.unit})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        editRateText.toDoubleOrNull()?.let { newRate ->
                            viewModel.updateBOQItemRate(item.itemNumber, newRate)
                        }
                        selectedItemToEdit = null
                    }
                ) {
                    Text("Save Rate")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedItemToEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showExportConfirm) {
        AlertDialog(
            onDismissRequest = { showExportConfirm = false },
            title = { Text("BOQ Exported") },
            text = { Text("Complete Bill of Quantities table has been copied to clipboard in spreadsheet CSV format.") },
            confirmButton = {
                Button(onClick = { showExportConfirm = false }) { Text("OK") }
            }
        )
    }
}

@Composable
private fun CostRow(label: String, amount: String, isTotal: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = if (isTotal) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = amount,
            style = if (isTotal) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (isTotal) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun generateCsv(items: List<BOQItemEntity>, breakdown: com.example.engine.BOQGeneratorEngine.CostBreakdown?): String {
    val sb = StringBuilder()
    sb.append("Item,Section,Description,Unit,Quantity,Rate,Amount\n")
    for (i in items) {
        sb.append("\"${i.itemNumber}\",\"${i.section}\",\"${i.description.replace("\"", "\"\"")}\",\"${i.unit}\",${i.quantity},${i.rate},${i.amount}\n")
    }
    if (breakdown != null) {
        sb.append("\n\"\",\"SUBTOTAL\",\"Direct Works\",,,,\"${breakdown.subtotal}\"\n")
        sb.append("\"\",\"PRELIMS\",\"Preliminaries (3%)\",,,,\"${breakdown.preliminariesAndInsurance}\"\n")
        sb.append("\"\",\"OVERHEAD\",\"Contractor Margin (10%)\",,,,\"${breakdown.contractorOverheadAndProfit}\"\n")
        sb.append("\"\",\"CONTINGENCY\",\"Contingency (5%)\",,,,\"${breakdown.contingencyAllowance}\"\n")
        sb.append("\"\",\"VAT\",\"Tax VAT (7.5%)\",,,,\"${breakdown.valueAddedTaxVat}\"\n")
        sb.append("\"\",\"GRAND TOTAL\",\"Estimated Total\",,,,\"${breakdown.grandTotalEstimatedCost}\"\n")
    }
    return sb.toString()
}
