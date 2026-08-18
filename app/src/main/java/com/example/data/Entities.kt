package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val clientName: String = "",
    val location: String = "Lagos, Nigeria",
    val buildingType: String = "Residential Bungalow",
    val numFloors: Int = 1,
    val plotWidth: Double = 30.0,
    val plotLength: Double = 20.0,
    val buildingWidth: Double = 15.0,
    val buildingLength: Double = 12.0,
    val soilBearingCapacity: Double = 150.0, // kPa
    val designCode: String = "Eurocode 2 (EN 1992)",
    val currency: String = "NGN (₦)",
    val unitSystem: String = "Metric (m, mm, kN)",
    val estimatedCost: Double = 0.0,
    val progress: Int = 35,
    val revision: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "calculation_records")
data class CalculationRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long = 1,
    val calcType: String, // BEAM, COLUMN, SLAB, FOUNDATION, STAIRCASE, CONCRETE, BLOCKWORK, REBAR, ANALYSIS
    val title: String,
    val summary: String,
    val inputsJson: String,
    val resultsJson: String,
    val designCode: String,
    val status: String = "OK", // OK, WARNING, FAIL
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "drawings")
data class DrawingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long = 1,
    val title: String,
    val drawingType: String = "ARCHITECTURAL", // ARCHITECTURAL, STRUCTURAL_GRID, REINFORCEMENT
    val scale: String = "1:100",
    val roomsJson: String = "",
    val columnsJson: String = "",
    val beamsJson: String = "",
    val notes: String = "Preliminary AI design - verify on site.",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "boq_items")
data class BOQItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long = 1,
    val section: String,
    val itemNumber: String,
    val description: String,
    val unit: String,
    val quantity: Double,
    val rate: Double,
    val amount: Double
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long = 1,
    val sender: String, // "USER" or "AI"
    val agentType: String = "Civil Engineer AI", // Structural AI, Architect AI, QS AI, Civil AI
    val message: String,
    val structuredDataJson: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
