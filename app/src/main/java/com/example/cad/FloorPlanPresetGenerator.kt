package com.example.cad

object FloorPlanPresetGenerator {

    fun generate3BedroomBungalow(): CADDrawingState {
        val rooms = listOf(
            CADRoom("R1", "Living Room", 1.0f, 1.0f, 5.5f, 4.5f, "#E0F2FE"),
            CADRoom("R2", "Dining Area", 6.5f, 1.0f, 3.5f, 3.0f, "#FEF3C7"),
            CADRoom("R3", "Kitchen & Pantry", 10.0f, 1.0f, 4.0f, 3.5f, "#FEE2E2", isWetArea = true),
            CADRoom("R4", "Master Bedroom (Ensuite)", 1.0f, 5.5f, 4.8f, 4.2f, "#E0E7FF"),
            CADRoom("R5", "Master Bath", 5.8f, 5.5f, 2.2f, 2.0f, "#CFFAFE", isWetArea = true),
            CADRoom("R6", "Bedroom 2", 8.0f, 4.5f, 3.8f, 3.6f, "#F3E8FF"),
            CADRoom("R7", "Bedroom 3", 8.0f, 8.1f, 3.8f, 3.6f, "#FCE7F3"),
            CADRoom("R8", "Common Bathroom", 5.8f, 7.5f, 2.2f, 2.0f, "#CFFAFE", isWetArea = true),
            CADRoom("R9", "Entrance Porch", 1.0f, 0.0f, 3.0f, 1.0f, "#F1F5F9")
        )

        val columns = listOf(
            CADColumn("C1", "C1", 1.0f, 1.0f),
            CADColumn("C2", "C2", 6.5f, 1.0f),
            CADColumn("C3", "C3", 10.0f, 1.0f),
            CADColumn("C4", "C4", 14.0f, 1.0f),
            CADColumn("C5", "C5", 1.0f, 5.5f),
            CADColumn("C6", "C6", 6.5f, 5.5f),
            CADColumn("C7", "C7", 10.0f, 4.5f),
            CADColumn("C8", "C8", 14.0f, 4.5f),
            CADColumn("C9", "C9", 1.0f, 9.7f),
            CADColumn("C10", "C10", 5.8f, 9.7f),
            CADColumn("C11", "C11", 8.0f, 11.7f),
            CADColumn("C12", "C12", 11.8f, 11.7f)
        )

        val beams = listOf(
            CADBeam("B1", "C1", "C2", 1.0f, 1.0f, 6.5f, 1.0f),
            CADBeam("B2", "C2", "C3", 6.5f, 1.0f, 10.0f, 1.0f),
            CADBeam("B3", "C3", "C4", 10.0f, 1.0f, 14.0f, 1.0f),
            CADBeam("B4", "C1", "C5", 1.0f, 1.0f, 1.0f, 5.5f),
            CADBeam("B5", "C2", "C6", 6.5f, 1.0f, 6.5f, 5.5f),
            CADBeam("B6", "C5", "C6", 1.0f, 5.5f, 6.5f, 5.5f),
            CADBeam("B7", "C5", "C9", 1.0f, 5.5f, 1.0f, 9.7f),
            CADBeam("B8", "C9", "C10", 1.0f, 9.7f, 5.8f, 9.7f),
            CADBeam("B9", "C7", "C8", 10.0f, 4.5f, 14.0f, 4.5f),
            CADBeam("B10", "C11", "C12", 8.0f, 11.7f, 11.8f, 11.7f)
        )

        val doors = listOf(
            CADDoor("D1", "R9", 2.2f, 1.0f, 1.0f),
            CADDoor("D2", "R4", 4.5f, 5.5f, 0.9f),
            CADDoor("D3", "R6", 8.2f, 4.5f, 0.9f),
            CADDoor("D4", "R7", 8.2f, 8.1f, 0.9f),
            CADDoor("D5", "R3", 10.2f, 3.5f, 0.9f)
        )

        val windows = listOf(
            CADWindow("W1", "R1", 2.5f, 1.0f, 1.8f),
            CADWindow("W2", "R1", 1.0f, 2.5f, 1.5f, "VERTICAL"),
            CADWindow("W3", "R2", 8.0f, 1.0f, 1.5f),
            CADWindow("W4", "R3", 12.0f, 1.0f, 1.2f),
            CADWindow("W5", "R4", 1.0f, 7.5f, 1.5f, "VERTICAL"),
            CADWindow("W6", "R7", 11.8f, 9.5f, 1.5f, "VERTICAL")
        )

        return CADDrawingState(
            title = "3-Bedroom Contemporary Bungalow",
            plotWidthM = 22f,
            plotLengthM = 16f,
            buildingWidthM = 14.5f,
            buildingLengthM = 12.5f,
            rooms = rooms,
            columns = columns,
            beams = beams,
            doors = doors,
            windows = windows
        )
    }

    fun generate4BedroomDuplex(): CADDrawingState {
        val rooms = listOf(
            CADRoom("R1", "Main Living Room", 1.0f, 1.0f, 6.0f, 5.0f, "#E0F2FE"),
            CADRoom("R2", "Formal Dining", 7.0f, 1.0f, 4.0f, 3.5f, "#FEF3C7"),
            CADRoom("R3", "Chef's Kitchen & Island", 11.0f, 1.0f, 4.5f, 4.0f, "#FEE2E2", isWetArea = true),
            CADRoom("R4", "Guest Bedroom Ensuite", 1.0f, 6.0f, 4.5f, 4.0f, "#E0E7FF"),
            CADRoom("R5", "Dogleg Staircase Hall", 5.5f, 6.0f, 3.0f, 4.0f, "#F1F5F9"),
            CADRoom("R6", "Ante Room & Visitors WC", 8.5f, 6.0f, 3.5f, 3.0f, "#CFFAFE", isWetArea = true),
            CADRoom("R7", "Laundry & Utility", 12.0f, 6.0f, 3.5f, 3.0f, "#F3E8FF", isWetArea = true)
        )

        val columns = listOf(
            CADColumn("C1", "C1", 1.0f, 1.0f),
            CADColumn("C2", "C2", 7.0f, 1.0f),
            CADColumn("C3", "C3", 11.0f, 1.0f),
            CADColumn("C4", "C4", 15.5f, 1.0f),
            CADColumn("C5", "C5", 1.0f, 6.0f),
            CADColumn("C6", "C6", 5.5f, 6.0f),
            CADColumn("C7", "C7", 8.5f, 6.0f),
            CADColumn("C8", "C8", 12.0f, 6.0f),
            CADColumn("C9", "C9", 15.5f, 6.0f),
            CADColumn("C10", "C10", 1.0f, 10.0f),
            CADColumn("C11", "C11", 5.5f, 10.0f),
            CADColumn("C12", "C12", 8.5f, 10.0f),
            CADColumn("C13", "C13", 15.5f, 10.0f)
        )

        val beams = listOf(
            CADBeam("B1", "C1", "C2", 1f, 1f, 7f, 1f),
            CADBeam("B2", "C2", "C3", 7f, 1f, 11f, 1f),
            CADBeam("B3", "C3", "C4", 11f, 1f, 15.5f, 1f),
            CADBeam("B4", "C1", "C5", 1f, 1f, 1f, 6f),
            CADBeam("B5", "C5", "C6", 1f, 6f, 5.5f, 6f),
            CADBeam("B6", "C6", "C7", 5.5f, 6f, 8.5f, 6f),
            CADBeam("B7", "C7", "C8", 8.5f, 6f, 12f, 6f),
            CADBeam("B8", "C8", "C9", 12f, 6f, 15.5f, 6f),
            CADBeam("B9", "C5", "C10", 1f, 6f, 1f, 10f),
            CADBeam("B10", "C10", "C11", 1f, 10f, 5.5f, 10f),
            CADBeam("B11", "C11", "C12", 5.5f, 10f, 8.5f, 10f),
            CADBeam("B12", "C12", "C13", 8.5f, 10f, 15.5f, 10f)
        )

        return CADDrawingState(
            title = "4-Bedroom Luxury Duplex (Ground Floor)",
            plotWidthM = 25f,
            plotLengthM = 20f,
            buildingWidthM = 16f,
            buildingLengthM = 11f,
            rooms = rooms,
            columns = columns,
            beams = beams
        )
    }

    fun generateCompact2Bedroom(): CADDrawingState {
        val rooms = listOf(
            CADRoom("R1", "Living & Dining", 1.0f, 1.0f, 4.5f, 4.0f, "#E0F2FE"),
            CADRoom("R2", "Kitchen", 5.5f, 1.0f, 3.0f, 3.0f, "#FEE2E2", isWetArea = true),
            CADRoom("R3", "Master Bedroom", 1.0f, 5.0f, 4.0f, 3.5f, "#E0E7FF"),
            CADRoom("R4", "Bedroom 2", 5.0f, 5.0f, 3.5f, 3.5f, "#F3E8FF"),
            CADRoom("R5", "Bathroom & WC", 5.5f, 4.0f, 3.0f, 1.8f, "#CFFAFE", isWetArea = true)
        )

        val columns = listOf(
            CADColumn("C1", "C1", 1.0f, 1.0f),
            CADColumn("C2", "C2", 5.5f, 1.0f),
            CADColumn("C3", "C3", 8.5f, 1.0f),
            CADColumn("C4", "C4", 1.0f, 5.0f),
            CADColumn("C5", "C5", 5.0f, 5.0f),
            CADColumn("C6", "C6", 8.5f, 5.0f),
            CADColumn("C7", "C7", 1.0f, 8.5f),
            CADColumn("C8", "C8", 5.0f, 8.5f),
            CADColumn("C9", "C9", 8.5f, 8.5f)
        )

        val beams = listOf(
            CADBeam("B1", "C1", "C2", 1f, 1f, 5.5f, 1f),
            CADBeam("B2", "C2", "C3", 5.5f, 1f, 8.5f, 1f),
            CADBeam("B3", "C1", "C4", 1f, 1f, 1f, 5f),
            CADBeam("B4", "C4", "C5", 1f, 5f, 5f, 5f),
            CADBeam("B5", "C5", "C6", 5f, 5f, 8.5f, 5f),
            CADBeam("B6", "C4", "C7", 1f, 5f, 1f, 8.5f),
            CADBeam("B7", "C7", "C8", 1f, 8.5f, 5f, 8.5f),
            CADBeam("B8", "C8", "C9", 5f, 8.5f, 8.5f, 8.5f)
        )

        return CADDrawingState(
            title = "2-Bedroom Starter Flat / Annex",
            plotWidthM = 16f,
            plotLengthM = 14f,
            buildingWidthM = 9.5f,
            buildingLengthM = 9.5f,
            rooms = rooms,
            columns = columns,
            beams = beams
        )
    }
}
