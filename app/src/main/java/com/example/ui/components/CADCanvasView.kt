package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cad.CADDrawingState

@Composable
fun CADCanvasView(
    drawingState: CADDrawingState,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(24f) } // Pixels per meter
    var offset by remember { mutableStateOf(Offset(80f, 80f)) }

    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = modifier
            .background(Color(0xFF0F172A)) // Blueprint Dark Navy
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(10f, 80f)
                    offset = Offset(offset.x + pan.x, offset.y + pan.y)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // 1. Blueprint Grid Lines (1m intervals)
            if (drawingState.showGrid) {
                val gridSpacing = scale
                val startX = (offset.x % gridSpacing)
                val startY = (offset.y % gridSpacing)

                var x = startX
                while (x < width) {
                    drawLine(
                        color = Color(0xFF1E293B),
                        start = Offset(x, 0f),
                        end = Offset(x, height),
                        strokeWidth = 1f
                    )
                    x += gridSpacing
                }

                var y = startY
                while (y < height) {
                    drawLine(
                        color = Color(0xFF1E293B),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f
                    )
                    y += gridSpacing
                }
            }

            // 2. Render Rooms
            val titleFontSize = (scale * 0.38f).coerceIn(12f, 22f).sp
            val subFontSize = (scale * 0.28f).coerceIn(10f, 16f).sp

            for (room in drawingState.rooms) {
                val rx = offset.x + (room.xM * scale)
                val ry = offset.y + (room.yM * scale)
                val rw = room.widthM * scale
                val rh = room.lengthM * scale

                // Room background fill
                drawRect(
                    color = if (room.isWetArea) Color(0xFF0E7490).copy(alpha = 0.25f) else Color(0xFF334155).copy(alpha = 0.35f),
                    topLeft = Offset(rx, ry),
                    size = Size(rw, rh)
                )

                // Room Wall Outline
                drawRect(
                    color = if (room.isWetArea) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                    topLeft = Offset(rx, ry),
                    size = Size(rw, rh),
                    style = Stroke(width = 3.5f)
                )

                // Pure Compose Room Label Rendering
                val titleLayout = textMeasurer.measure(
                    text = room.name,
                    style = TextStyle(
                        color = Color.White,
                        fontSize = titleFontSize,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                )
                drawText(
                    textLayoutResult = titleLayout,
                    topLeft = Offset(
                        rx + (rw - titleLayout.size.width) / 2f,
                        ry + (rh / 2f) - titleLayout.size.height
                    )
                )

                val dimLayout = textMeasurer.measure(
                    text = "${String.format("%.1f", room.areaSqm)} m² (${String.format("%.1f", room.widthM)}×${String.format("%.1f", room.lengthM)}m)",
                    style = TextStyle(
                        color = Color(0xFF38BDF8),
                        fontSize = subFontSize,
                        textAlign = TextAlign.Center
                    )
                )
                drawText(
                    textLayoutResult = dimLayout,
                    topLeft = Offset(
                        rx + (rw - dimLayout.size.width) / 2f,
                        ry + (rh / 2f) + 4f
                    )
                )
            }

            // 3. Render Beams (Centerline Dashed)
            if (drawingState.showBeams) {
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                for (beam in drawingState.beams) {
                    val sx = offset.x + (beam.startX * scale)
                    val sy = offset.y + (beam.startY * scale)
                    val ex = offset.x + (beam.endX * scale)
                    val ey = offset.y + (beam.endY * scale)

                    drawLine(
                        color = Color(0xFFF59E0B), // Structural Amber
                        start = Offset(sx, sy),
                        end = Offset(ex, ey),
                        strokeWidth = 3f,
                        pathEffect = dashEffect
                    )
                }
            }

            // 4. Render Doors
            if (drawingState.showDoorsWindows) {
                for (door in drawingState.doors) {
                    val dx = offset.x + (door.xM * scale)
                    val dy = offset.y + (door.yM * scale)
                    val dw = door.widthM * scale

                    // Door leaf line
                    drawLine(
                        color = Color(0xFF10B981), // Emerald
                        start = Offset(dx, dy),
                        end = Offset(dx, dy - dw),
                        strokeWidth = 3f
                    )

                    // Door swing arc
                    drawArc(
                        color = Color(0xFF10B981).copy(alpha = 0.6f),
                        startAngle = 270f,
                        sweepAngle = 90f,
                        useCenter = false,
                        topLeft = Offset(dx - dw, dy - dw),
                        size = Size(dw * 2f, dw * 2f),
                        style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f))
                    )
                }
            }

            // 5. Render Columns
            if (drawingState.showColumns) {
                for (col in drawingState.columns) {
                    val cx = offset.x + (col.xM * scale)
                    val cy = offset.y + (col.yM * scale)
                    val cSize = (col.sizeMm / 1000f) * scale

                    // Column Square
                    drawRect(
                        color = Color(0xFFEF4444), // Reinforced Red/Orange
                        topLeft = Offset(cx - (cSize / 2f), cy - (cSize / 2f)),
                        size = Size(cSize, cSize)
                    )

                    // Column Outline
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(cx - (cSize / 2f), cy - (cSize / 2f)),
                        size = Size(cSize, cSize),
                        style = Stroke(width = 1.5f)
                    )

                    // Column Label
                    val colLayout = textMeasurer.measure(
                        text = col.label,
                        style = TextStyle(color = Color(0xFFFDE047), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    )
                    drawText(
                        textLayoutResult = colLayout,
                        topLeft = Offset(cx + (cSize / 2f) + 4f, cy - (cSize / 2f) - 2f)
                    )
                }
            }

            // 6. Technical North Arrow & Scale Bar (HUD overlay)
            drawHUD(scale, textMeasurer)
        }

        // HUD Overlay info
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .background(Color(0xCC0F172A), MaterialTheme.shapes.small)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = drawingState.title,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White
            )
            Text(
                text = "Scale ${drawingState.scaleLabel} | Plot: ${drawingState.plotWidthM.toInt()}m × ${drawingState.plotLengthM.toInt()}m | Touch to Pan/Zoom",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF38BDF8)
            )
        }
    }
}

private fun DrawScope.drawHUD(
    scale: Float,
    textMeasurer: TextMeasurer
) {
    // North Arrow at Top-Right
    val nx = size.width - 50f
    val ny = 50f

    // North Compass Circle
    drawCircle(
        color = Color(0xFF1E293B),
        radius = 24f,
        center = Offset(nx, ny)
    )
    drawCircle(
        color = Color(0xFF38BDF8),
        radius = 24f,
        center = Offset(nx, ny),
        style = Stroke(width = 1.5f)
    )

    // North Needle
    val northPath = Path().apply {
        moveTo(nx, ny - 20f)
        lineTo(nx + 6f, ny + 8f)
        lineTo(nx, ny + 2f)
        close()
    }
    drawPath(northPath, Color(0xFFEF4444))

    val southPath = Path().apply {
        moveTo(nx, ny - 20f)
        lineTo(nx - 6f, ny + 8f)
        lineTo(nx, ny + 2f)
        close()
    }
    drawPath(southPath, Color(0xFF94A3B8))

    val nLayout = textMeasurer.measure(
        text = "N",
        style = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    )
    drawText(
        textLayoutResult = nLayout,
        topLeft = Offset(nx - (nLayout.size.width / 2f), ny - 26f)
    )

    // Scale Bar at Bottom-Right
    val barLenM = 5f
    val barPx = barLenM * scale
    val bx = size.width - barPx - 30f
    val by = size.height - 30f

    drawLine(
        color = Color.White,
        start = Offset(bx, by),
        end = Offset(bx + barPx, by),
        strokeWidth = 4f
    )
    drawLine(color = Color.White, start = Offset(bx, by - 6f), end = Offset(bx, by + 6f), strokeWidth = 2f)
    drawLine(color = Color.White, start = Offset(bx + barPx, by - 6f), end = Offset(bx + barPx, by + 6f), strokeWidth = 2f)

    val scaleLayout = textMeasurer.measure(
        text = "${barLenM.toInt()}m",
        style = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    )
    drawText(
        textLayoutResult = scaleLayout,
        topLeft = Offset(bx + (barPx - scaleLayout.size.width) / 2f, by - 18f)
    )
}
