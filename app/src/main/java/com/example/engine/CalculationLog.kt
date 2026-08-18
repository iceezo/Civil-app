package com.example.engine

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LogLevel {
    INFO,
    SUCCESS,
    WARNING,
    CALCULATION
}

data class CalculationLogEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: String = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date()),
    val module: String,
    val level: LogLevel = LogLevel.INFO,
    val title: String,
    val details: String,
    val formula: String? = null,
    val isSafetyCompliant: Boolean = true
)
