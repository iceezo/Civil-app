package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.StructuralAnalysisEngine
import com.example.engine.StructuralBeamEngine
import kotlin.math.abs

@Composable
fun BeamCrossSectionSketch(
    widthMm: Double,
    depthMm: Double,
    bottomBarsText: String,
    topBarsText: String,
    linksText: String,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = modifier
            .background(Color(0xFF0F172A), MaterialTheme.shapes.medium)
            .padding(16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "REINFORCEMENT CROSS-SECTION",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF38BDF8)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                val cx = size.width / 2f
                val cy = size.height / 2f

                val scale = (size.height * 0.75f) / depthMm.toFloat()
                val wPx = widthMm.toFloat() * scale
                val hPx = depthMm.toFloat() * scale

                val left = cx - (wPx / 2f)
                val top = cy - (hPx / 2f)

                // Concrete Hatch Fill
                drawRect(
                    color = Color(0xFF334155),
                    topLeft = Offset(left, top),
                    size = Size(wPx, hPx)
                )

                // Concrete Boundary
                drawRect(
                    color = Color(0xFF94A3B8),
                    topLeft = Offset(left, top),
                    size = Size(wPx, hPx),
                    style = Stroke(width = 3f)
                )

                // Link / Stirrup (Rectangular Tie)
                val coverPx = 25f * scale
                val tieLeft = left + coverPx
                val tieTop = top + coverPx
                val tieW = wPx - (2 * coverPx)
                val tieH = hPx - (2 * coverPx)

                drawRect(
                    color = Color(0xFFF59E0B), // Amber Link
                    topLeft = Offset(tieLeft, tieTop),
                    size = Size(tieW, tieH),
                    style = Stroke(width = 2.5f)
                )

                // 2 Top Hanger Bars (T12)
                val barRadius = 6f
                drawCircle(color = Color(0xFF38BDF8), radius = barRadius, center = Offset(tieLeft + 8f, tieTop + 8f))
                drawCircle(color = Color(0xFF38BDF8), radius = barRadius, center = Offset(tieLeft + tieW - 8f, tieTop + 8f))

                // Bottom Main Bars (3T16 or 4T16)
                val numBotBars = if (bottomBarsText.startsWith("4")) 4 else 3
                for (i in 0 until numBotBars) {
                    val bx = tieLeft + 8f + (i * ((tieW - 16f) / (numBotBars - 1)))
                    val by = tieTop + tieH - 8f
                    drawCircle(color = Color(0xFFEF4444), radius = barRadius + 2f, center = Offset(bx, by))
                    drawCircle(color = Color.White, radius = barRadius + 2f, center = Offset(bx, by), style = Stroke(width = 1f))
                }

                // Dimension annotations (Pure Compose)
                val topDimLayout = textMeasurer.measure(
                    text = "${widthMm.toInt()} mm",
                    style = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                )
                drawText(
                    textLayoutResult = topDimLayout,
                    topLeft = Offset(cx - (topDimLayout.size.width / 2f), top - topDimLayout.size.height - 4f)
                )

                val vertDimLayout = textMeasurer.measure(
                    text = "${depthMm.toInt()} mm",
                    style = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                )
                drawText(
                    textLayoutResult = vertDimLayout,
                    topLeft = Offset(left - vertDimLayout.size.width - 6f, cy - (vertDimLayout.size.height / 2f))
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Top: $topBarsText", style = MaterialTheme.typography.bodySmall, color = Color(0xFF38BDF8))
                Text("Links: $linksText", style = MaterialTheme.typography.bodySmall, color = Color(0xFFF59E0B))
                Text("Bottom: $bottomBarsText", style = MaterialTheme.typography.bodySmall, color = Color(0xFFEF4444))
            }
        }
    }
}

@Composable
fun StructuralDiagramCanvas(
    diagramPoints: List<StructuralAnalysisEngine.DiagramPoint>,
    spanM: Double,
    maxShearKn: Double,
    maxMomentKNm: Double,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0xFF0F172A), MaterialTheme.shapes.medium)
            .padding(12.dp)
    ) {
        Text(
            text = "SHEAR FORCE (SFD) & BENDING MOMENT (BMD) DIAGRAMS",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF38BDF8)
        )
        Spacer(modifier = Modifier.height(10.dp))

        // SFD Plot
        Text("Shear Force Diagram (V_max = ${String.format("%.1f", maxShearKn)} kN)", style = MaterialTheme.typography.bodySmall, color = Color(0xFF60A5FA))
        Canvas(modifier = Modifier.fillMaxWidth().height(90.dp)) {
            val w = size.width
            val h = size.height
            val midY = h / 2f

            drawLine(color = Color(0xFF475569), start = Offset(0f, midY), end = Offset(w, midY), strokeWidth = 1.5f)

            val maxV = if (maxShearKn > 0) maxShearKn.toFloat() else 1f
            val sfdPath = Path().apply {
                moveTo(0f, midY)
                for (pt in diagramPoints) {
                    val px = (pt.xM.toFloat() / spanM.toFloat()) * w
                    val py = midY - ((pt.shearKn.toFloat() / maxV) * (h * 0.40f))
                    lineTo(px, py)
                }
                lineTo(w, midY)
                close()
            }
            drawPath(sfdPath, Color(0xFF38BDF8).copy(alpha = 0.35f))
            drawPath(sfdPath, Color(0xFF38BDF8), style = Stroke(width = 2.5f))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // BMD Plot
        Text("Bending Moment Diagram (M_max = ${String.format("%.1f", maxMomentKNm)} kNm)", style = MaterialTheme.typography.bodySmall, color = Color(0xFFF59E0B))
        Canvas(modifier = Modifier.fillMaxWidth().height(90.dp)) {
            val w = size.width
            val h = size.height
            val baselineY = h * 0.20f

            drawLine(color = Color(0xFF475569), start = Offset(0f, baselineY), end = Offset(w, baselineY), strokeWidth = 1.5f)

            val maxM = if (maxMomentKNm > 0) maxMomentKNm.toFloat() else 1f
            val bmdPath = Path().apply {
                moveTo(0f, baselineY)
                for (pt in diagramPoints) {
                    val px = (pt.xM.toFloat() / spanM.toFloat()) * w
                    val py = baselineY + ((pt.momentKNm.toFloat() / maxM) * (h * 0.70f))
                    lineTo(px, py)
                }
                lineTo(w, baselineY)
                close()
            }
            drawPath(bmdPath, Color(0xFFF59E0B).copy(alpha = 0.35f))
            drawPath(bmdPath, Color(0xFFF59E0B), style = Stroke(width = 2.5f))
        }
    }
}
