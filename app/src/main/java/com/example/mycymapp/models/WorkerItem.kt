package com.example.mycymapp.data // O el paquete donde tengas tus data classes

data class WorkerItem(
    val dni: String, // Ahora el DNI es el identificador único
    val nombres: String,
    val apellidos: String,
    val area: String,
    val puestoLabor: String,
    val tipoTrabajador: String
)