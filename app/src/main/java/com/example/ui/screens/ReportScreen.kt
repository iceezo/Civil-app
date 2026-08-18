package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.engine.CalculationLogEntry
import com.example.engine.LogLevel
import com.example.reports.EngineeringReportEngine
import com.example.ui.MainViewModel

@Composable
fun ReportScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val project by viewModel.activeProject.collectAsState()
    val beamResult by viewModel.beamResult.collectAsState()
    val cadState by viewModel.cadDrawing.collectAsState()
    val boqItems by viewModel.boqItems.collectAsState()
    val costBreakdown by viewModel.costBreakdown.collectAsState()
    val calculationLogs by viewModel.calculationLogs.collectAsState()

    val context = LocalContext.current
    var showCopiedToast by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Dossier, 1: Debug Logs

    val reportText = remember(project, beamResult, cadState, boqItems, costBreakdown) {
        EngineeringReportEngine.generateComprehensiveEngineeringReport(
            project = project,
            beamResult = beamResult,
            drawingState = cadState,
            boqItems = boqItems,
            costBreakdown = costBreakdown
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val contentToCopy = if (selectedTab == 0) {
                        reportText
                    } else {
                        calculationLogs.joinToString("\n\n") { log ->
                            "[${log.timestamp}] [${log.module}] ${log.title}\n${log.details}${log.formula?.let { "\nFormula: $it" } ?: ""}"
                        }
                    }
                    val clip = ClipData.newPlainText(if (selectedTab == 0) "Engineering Dossier" else "Debug Logs", contentToCopy)
                    clipboard.setPrimaryClip(clip)
                    toastMessage = if (selectedTab == 0) "Complete technical engineering calculation report copied." else "Calculation debug log copied."
                    showCopiedToast = true
                },
                icon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                text = { Text(if (selectedTab == 0) "Copy Dossier" else "Copy Debug Logs") },
                containerColor = Color(0xFF2563EB),
                contentColor = Color.White
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header & Tabs
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "ENGINEERING AUDIT & DOCUMENTATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${project.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Official Dossier", fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.Description, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Calculation & Debug Logs (${calculationLogs.size})", fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.BugReport, contentDescription = null) }
                    )
                }
            }

            if (selectedTab == 0) {
                // Official Technical Dossier View
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = reportText,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFFE2E8F0)
                                )
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            } else {
                // Real-time Calculation & Debug Logs View
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(calculationLogs, key = { it.id }) { log ->
                        CalculationLogCard(log = log)
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showCopiedToast) {
        AlertDialog(
            onDismissRequest = { showCopiedToast = false },
            title = { Text("Clipboard") },
            text = { Text(toastMessage) },
            confirmButton = {
                Button(onClick = { showCopiedToast = false }) { Text("OK") }
            }
        )
    }
}

@Composable
private fun CalculationLogCard(log: CalculationLogEntry) {
    val badgeColor = when (log.level) {
        LogLevel.INFO -> Color(0xFF38BDF8)
        LogLevel.SUCCESS -> Color(0xFF10B981)
        LogLevel.WARNING -> Color(0xFFF59E0B)
        LogLevel.CALCULATION -> Color(0xFF818CF8)
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = badgeColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = log.module,
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = log.timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF94A3B8)
                    )
                }

                Surface(
                    color = if (log.isSafetyCompliant) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (log.isSafetyCompliant) "COMPLIANT" else "CHECK REQ",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (log.isSafetyCompliant) Color(0xFF10B981) else Color(0xFFEF4444),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = log.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = log.details,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFCBD5E1)
            )

            if (log.formula != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = log.formula,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF38BDF8),
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

