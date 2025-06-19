// com.example.mycymapp.data/RegistroDiario.kt
package com.example.mycymapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "registros_diarios")
data class RegistroDiario(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val trabajadorDni: String,
    val fecha: String, // Formato YYYY-MM-DD
    val hora: String, // Formato HH:MM AM/PM
    val turno: String, // CAMBIADO: Antes era tipoComida, ahora es 'turno' ("Almuerzo" o "Cena")
    var tipoMenu: String, // Este es el tipo de menú editable (ej. "Común", "Vegetariano")
    var cantidad: Int // Puede aumentar
)