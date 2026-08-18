package com.example.cad

import androidx.compose.ui.graphics.Color

data class CADRoom(
    val id: String,
    val name: String,
    val xM: Float,
    val yM: Float,
    val widthM: Float,
    val lengthM: Float,
    val colorHex: String = "#E2E8F0",
    val isWetArea: Boolean = false
) {
    val areaSqm: Float get() = widthM * lengthM
}

data class CADColumn(
    val id: String,
    val label: String, // C1, C2, C3...
    val xM: Float,
    val yM: Float,
    val sizeMm: Float = 230f
)

data class CADBeam(
    val id: String,
    val startColumnId: String,
    val endColumnId: String,
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val widthMm: Float = 230f,
    val depthMm: Float = 450f
)

data class CADDoor(
    val id: String,
    val roomId: String,
    val xM: Float,
    val yM: Float,
    val widthM: Float = 0.9f,
    val swingAngleDeg: Float = 90f,
    val orientation: String = "HORIZONTAL" // HORIZONTAL, VERTICAL
)

data class CADWindow(
    val id: String,
    val roomId: String,
    val xM: Float,
    val yM: Float,
    val widthM: Float = 1.2f,
    val orientation: String = "HORIZONTAL"
)

data class CADGridLine(
    val label: String, // A, B, C or 1, 2, 3
    val positionM: Float,
    val isHorizontal: Boolean
)

data class CADDrawingState(
    val title: String = "Architectural & Structural Layout",
    val scaleLabel: String = "1:100",
    val plotWidthM: Float = 24f,
    val plotLengthM: Float = 18f,
    val buildingWidthM: Float = 16f,
    val buildingLengthM: Float = 13f,
    val rooms: List<CADRoom> = emptyList(),
    val columns: List<CADColumn> = emptyList(),
    val beams: List<CADBeam> = emptyList(),
    val doors: List<CADDoor> = emptyList(),
    val windows: List<CADWindow> = emptyList(),
    val showGrid: Boolean = true,
    val showColumns: Boolean = true,
    val showBeams: Boolean = true,
    val showDimensions: Boolean = true,
    val showFurniture: Boolean = true,
    val showDoorsWindows: Boolean = true
)
