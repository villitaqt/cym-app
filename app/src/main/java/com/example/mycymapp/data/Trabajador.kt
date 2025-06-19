// com.example.mycymapp.data/Trabajador.kt
package com.example.mycymapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trabajadores")
data class Trabajador(
    @PrimaryKey
    val dni: String, // DNI como clave primaria
    val nombres: String,
    val apellidos: String,
    val area: String,
    val puestoLabor: String, // Nuevo campo
    val tipoTrabajador: String // Nuevo campo
)