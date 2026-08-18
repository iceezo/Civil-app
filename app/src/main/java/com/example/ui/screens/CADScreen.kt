package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.MainViewModel
import com.example.ui.components.CADCanvasView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CADScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val drawingState by viewModel.cadDrawing.collectAsState()
    val context = LocalContext.current
    var showDxfDialog by remember { mutableStateOf(false) }
    var dxfContent by remember { mutableStateOf("") }
    var snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Preset Bar & Export Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "2D ARCHITECTURAL & STRUCTURAL CAD",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = drawingState.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        dxfContent = viewModel.getDXFExportString()
                        showDxfDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.FileDownload, contentDescription = "Export DXF", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export DXF")
                }
            }

            // Presets row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = drawingState.rooms.size == 9,
                        onClick = { viewModel.loadCADPreset(1) },
                        label = { Text("3-Bed Bungalow") },
                        leadingIcon = { Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
                item {
                    FilterChip(
                        selected = drawingState.rooms.size == 7,
                        onClick = { viewModel.loadCADPreset(2) },
                        label = { Text("4-Bed Duplex") },
                        leadingIcon = { Icon(Icons.Default.Apartment, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
                item {
                    FilterChip(
                        selected = drawingState.rooms.size == 5,
                        onClick = { viewModel.loadCADPreset(3) },
                        label = { Text("2-Bed Flat") },
                        leadingIcon = { Icon(Icons.Default.MeetingRoom, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }

            // Layer Visibility Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    AssistChip(
                        onClick = { viewModel.toggleCADLayer("GRID") },
                        label = { Text("Grid: ${if (drawingState.showGrid) "ON" else "OFF"}", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.GridOn, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp)) }
                    )
                }
                item {
                    AssistChip(
                        onClick = { viewModel.toggleCADLayer("COLUMNS") },
                        label = { Text("Columns: ${if (drawingState.showColumns) "ON" else "OFF"}", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.ViewColumn, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp)) }
                    )
                }
                item {
                    AssistChip(
                        onClick = { viewModel.toggleCADLayer("BEAMS") },
                        label = { Text("Beams: ${if (drawingState.showBeams) "ON" else "OFF"}", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.LineStyle, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp)) }
                    )
                }
                item {
                    AssistChip(
                        onClick = { viewModel.toggleCADLayer("DOORS_WINDOWS") },
                        label = { Text("Doors & Openings", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.SensorDoor, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp)) }
                    )
                }
            }

            // Interactive CAD Canvas
            Box(modifier = Modifier.weight(1f)) {
                CADCanvasView(
                    drawingState = drawingState,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    // DXF Export Dialog
    if (showDxfDialog) {
        AlertDialog(
            onDismissRequest = { showDxfDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AutoCAD DXF Ready")
                }
            },
            text = {
                Column {
                    Text("Standard ASCII DXF (AutoCAD / Revit compatible) generated with layers: A-WALL, S-COLS, S-BEAM, A-DOOR, A-TEXT.")
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Size: ${dxfContent.length} bytes | Entities: ${drawingState.rooms.size + drawingState.columns.size + drawingState.beams.size} CAD items", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("DXF CAD Data", dxfContent)
                        clipboard.setPrimaryClip(clip)
                        showDxfDialog = false
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy DXF Code")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDxfDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
