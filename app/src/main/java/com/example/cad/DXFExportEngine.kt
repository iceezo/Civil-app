package com.example.cad

import java.util.Locale

object DXFExportEngine {

    fun generateDXF(state: CADDrawingState): String {
        val sb = StringBuilder()

        // 1. DXF Header
        sb.append("0\nSECTION\n2\nHEADER\n")
        sb.append("9\n\$ACADVER\n1\nAC1015\n") // AutoCAD 2000 format
        sb.append("9\n\$INSUNITS\n70\n6\n") // Meters
        sb.append("0\nENDSEC\n")

        // 2. DXF Tables (Layers)
        sb.append("0\nSECTION\n2\nTABLES\n")
        sb.append("0\nTABLE\n2\nLAYER\n70\n4\n")

        // Layer: A-WALL (Cyan)
        sb.append("0\nLAYER\n2\nA-WALL\n70\n0\n62\n4\n6\nCONTINUOUS\n")
        // Layer: S-COLS (Red)
        sb.append("0\nLAYER\n2\nS-COLS\n70\n0\n62\n1\n6\nCONTINUOUS\n")
        // Layer: S-BEAM (Magenta)
        sb.append("0\nLAYER\n2\nS-BEAM\n70\n0\n62\n6\n6\nDASHED\n")
        // Layer: A-DOOR (Green)
        sb.append("0\nLAYER\n2\nA-DOOR\n70\n0\n62\n3\n6\nCONTINUOUS\n")
        // Layer: A-TEXT (White)
        sb.append("0\nLAYER\n2\nA-TEXT\n70\n0\n62\n7\n6\nCONTINUOUS\n")

        sb.append("0\nENDTAB\n")
        sb.append("0\nENDSEC\n")

        // 3. DXF Entities
        sb.append("0\nSECTION\n2\nENTITIES\n")

        // Render Rooms as LWPOLYLINE (Walls)
        for (room in state.rooms) {
            val x1 = room.xM
            val y1 = room.yM
            val x2 = room.xM + room.widthM
            val y2 = room.yM + room.lengthM

            sb.append("0\nLWPOLYLINE\n")
            sb.append("8\nA-WALL\n")
            sb.append("90\n4\n")
            sb.append("70\n1\n") // Closed polyline
            sb.append("43\n0.15\n") // Constant width (wall thickness ~ 150mm)

            sb.append(String.format(Locale.US, "10\n%.3f\n20\n%.3f\n", x1, y1))
            sb.append(String.format(Locale.US, "10\n%.3f\n20\n%.3f\n", x2, y1))
            sb.append(String.format(Locale.US, "10\n%.3f\n20\n%.3f\n", x2, y2))
            sb.append(String.format(Locale.US, "10\n%.3f\n20\n%.3f\n", x1, y2))

            // Room Name Text
            val centerX = room.xM + (room.widthM / 2.0)
            val centerY = room.yM + (room.lengthM / 2.0)
            sb.append("0\nTEXT\n")
            sb.append("8\nA-TEXT\n")
            sb.append(String.format(Locale.US, "10\n%.3f\n20\n%.3f\n30\n0.0\n", centerX, centerY))
            sb.append("40\n0.30\n") // Text height
            sb.append("1\n${room.name} (${String.format(Locale.US, "%.1f", room.areaSqm)}m2)\n")
        }

        // Render Columns
        for (col in state.columns) {
            val half = (col.sizeMm / 2000.0)
            val x1 = col.xM - half
            val y1 = col.yM - half
            val x2 = col.xM + half
            val y2 = col.yM + half

            sb.append("0\nLWPOLYLINE\n")
            sb.append("8\nS-COLS\n")
            sb.append("90\n4\n70\n1\n")
            sb.append(String.format(Locale.US, "10\n%.3f\n20\n%.3f\n", x1, y1))
            sb.append(String.format(Locale.US, "10\n%.3f\n20\n%.3f\n", x2, y1))
            sb.append(String.format(Locale.US, "10\n%.3f\n20\n%.3f\n", x2, y2))
            sb.append(String.format(Locale.US, "10\n%.3f\n20\n%.3f\n", x1, y2))

            // Column Label
            sb.append("0\nTEXT\n8\nS-COLS\n")
            sb.append(String.format(Locale.US, "10\n%.3f\n20\n%.3f\n30\n0.0\n", col.xM + 0.2, col.yM + 0.2))
            sb.append("40\n0.20\n")
            sb.append("1\n${col.label}\n")
        }

        // Render Beams as Centerlines
        for (beam in state.beams) {
            sb.append("0\nLINE\n")
            sb.append("8\nS-BEAM\n")
            sb.append(String.format(Locale.US, "10\n%.3f\n20\n%.3f\n30\n0.0\n", beam.startX, beam.startY))
            sb.append(String.format(Locale.US, "11\n%.3f\n21\n%.3f\n31\n0.0\n", beam.endX, beam.endY))
        }

        sb.append("0\nENDSEC\n")
        sb.append("0\nEOF\n")

        return sb.toString()
    }
}
