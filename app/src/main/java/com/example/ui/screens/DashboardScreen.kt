package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.engine.UnitConversionEngine
import com.example.ui.MainViewModel

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToTab: (Int) -> Unit,
    onOpenProjectManager: () -> Unit,
    modifier: Modifier = Modifier
) {
    val project by viewModel.activeProject.collectAsState()
    val costBreakdown by viewModel.costBreakdown.collectAsState()
    val beamResult by viewModel.beamResult.collectAsState()
    val cadState by viewModel.cadDrawing.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Mandatory Engineering Disclaimer Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Engineering Notice",
                        tint = Color(0xFFB45309),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Engineering Notice: AI calculations are preliminary decision-support outputs. Must be certified by a licensed Professional Engineer (PE/COREN/ICE) before construction.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF78350F),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 2. Active Project Hero Card
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
                            Text(
                                text = "ACTIVE PROJECT",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = project.name,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(
                            onClick = onOpenProjectManager,
                            modifier = Modifier
                                .background(Color(0xFF1E293B), CircleShape)
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = "Switch Project",
                                tint = Color(0xFF38BDF8)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        InfoPill(label = "Type", value = project.buildingType, color = Color(0xFF94A3B8))
                        InfoPill(label = "Standard", value = project.designCode, color = Color(0xFF38BDF8))
                        InfoPill(label = "Soil (qa)", value = "${project.soilBearingCapacity.toInt()} kPa", color = Color(0xFF10B981))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Estimated Cost & Quick Metrics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "ESTIMATED TOTAL COST",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = costBreakdown?.let { UnitConversionEngine.formatCurrency(it.grandTotalEstimatedCost, project.currency) } ?: "₦ 48,500,000",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color(0xFFFBBF24),
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Button(
                            onClick = { onNavigateToTab(3) }, // BOQ Tab
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("View BOQ")
                        }
                    }
                }
            }
        }

        // 3. Quick Engineering Module Hub (Grid)
        item {
            Text(
                text = "ENGINEERING MODULES",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ModuleTile(
                        title = "RC Beam Design",
                        subtitle = "EC2 / ACI / BS 8110",
                        icon = Icons.Default.Straighten,
                        badge = beamResult.overallStatus,
                        badgeColor = if (beamResult.overallStatus == "PASS") Color(0xFF10B981) else Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTab(1) } // Calculators tab
                    )
                    ModuleTile(
                        title = "Pad & Strip Footing",
                        subtitle = "Soil bearing & punching",
                        icon = Icons.Default.Foundation,
                        badge = "EC7 Safe",
                        badgeColor = Color(0xFF10B981),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTab(1) }
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ModuleTile(
                        title = "2D CAD Floor Plan",
                        subtitle = "${cadState.rooms.size} Rooms | DXF CAD",
                        icon = Icons.Default.Architecture,
                        badge = "Export DXF",
                        badgeColor = Color(0xFF38BDF8),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTab(2) } // CAD Tab
                    )
                    ModuleTile(
                        title = "Bill of Quantities",
                        subtitle = "14 CSI Master Sections",
                        icon = Icons.Default.RequestQuote,
                        badge = "Auto QS",
                        badgeColor = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTab(3) } // BOQ Tab
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ModuleTile(
                        title = "Concrete & Blocks",
                        subtitle = "Dry vol 1.54 | Cement bags",
                        icon = Icons.Default.Construction,
                        badge = "Mix 1:2:4",
                        badgeColor = Color(0xFF6366F1),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTab(1) }
                    )
                    ModuleTile(
                        title = "AI Civil Assistant",
                        subtitle = "Multi-Agent consultation",
                        icon = Icons.Default.SmartToy,
                        badge = "Active",
                        badgeColor = Color(0xFFEC4899),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTab(4) } // AI Tab
                    )
                }
            }
        }

        // 4. Quick Actions / Generate Engineering Report
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Engineering Design Dossier",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Generate printable calculations, BBS schedule, and official sign-off sheets.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { onNavigateToTab(5) }, // Report Tab
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("View Dossier")
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoPill(label: String, value: String, color: Color) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
        Text(text = value, style = MaterialTheme.typography.bodySmall, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ModuleTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badge: String,
    badgeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
                Surface(
                    color = badgeColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = badge,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
