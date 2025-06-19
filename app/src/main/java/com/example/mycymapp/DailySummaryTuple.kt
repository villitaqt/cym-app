// com.example.mycymapp.data/DailySummaryTuple.kt
package com.example.mycymapp.data

// Data class auxiliar para el resultado de la consulta de resúmenes diarios
data class DailySummaryTuple(
    val fecha: String, // Formato YYYY-MM-DD
    val totalLunches: Int,
    val totalDinners: Int
)