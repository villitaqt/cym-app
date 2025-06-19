// com.example.mycymapp.data/RegistroDiarioDao.kt
package com.example.mycymapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RegistroDiarioDao {
    @Insert
    suspend fun insert(registroDiario: RegistroDiario)

    @Update
    suspend fun update(registroDiario: RegistroDiario)

    @Query("SELECT * FROM registros_diarios WHERE trabajadorDni = :dni AND fecha = :fecha AND turno = :turno LIMIT 1")
    suspend fun getRegistroDiario(dni: String, fecha: String, turno: String): RegistroDiario?

    // Obtener todos los registros diarios para una fecha y TURNO (para la lista de trabajadores registrados)
    @Query("""
        SELECT
            rd.id,
            rd.trabajadorDni,
            rd.fecha,
            rd.hora,
            rd.turno,
            rd.tipoMenu,
            rd.cantidad,
            t.nombres,
            t.apellidos,
            t.area,               -- ¡AÑADIDO!
            t.puestoLabor,        -- ¡AÑADIDO!
            t.tipoTrabajador      -- ¡AÑADIDO!
        FROM registros_diarios rd
        INNER JOIN trabajadores t ON rd.trabajadorDni = t.dni
        WHERE rd.fecha = :fecha AND rd.turno = :turno
        ORDER BY rd.hora ASC
    """)
    fun getRegistrosDiariosConTrabajador(fecha: String, turno: String): Flow<List<RegistroDiarioConTrabajador>>

    @Query("SELECT fecha, SUM(CASE WHEN turno = 'Almuerzo' THEN cantidad ELSE 0 END) as totalLunches, SUM(CASE WHEN turno = 'Cena' THEN cantidad ELSE 0 END) as totalDinners FROM registros_diarios GROUP BY fecha ORDER BY fecha DESC")
    fun getDailySummaries(): Flow<List<DailySummaryTuple>>

    // ¡NUEVO MÉTODO PARA OBTENER REGISTROS DE UN DÍA Y TURNO PARA EXPORTACIÓN!
    @Query("""
        SELECT
            rd.id,
            rd.trabajadorDni,
            rd.fecha,
            rd.hora,
            rd.turno,
            rd.tipoMenu,
            rd.cantidad,
            t.nombres,
            t.apellidos,
            t.area,
            t.puestoLabor,
            t.tipoTrabajador
        FROM registros_diarios rd
        INNER JOIN trabajadores t ON rd.trabajadorDni = t.dni
        WHERE rd.fecha = :fecha AND rd.turno = :turno
        ORDER BY rd.hora ASC
    """)
    suspend fun getRegistrosByDateAndTurnoSingle(fecha: String, turno: String): List<RegistroDiarioConTrabajador>
}

// Data class auxiliar para el resultado de la consulta JOIN
// ¡AÑADIDO: puestoLabor y tipoTrabajador! (Estos ya los tenías aquí correctamente)
data class RegistroDiarioConTrabajador(
    val id: Int,
    val trabajadorDni: String,
    val fecha: String,
    val hora: String,
    val turno: String,
    val tipoMenu: String,
    val cantidad: Int,
    val nombres: String,
    val apellidos: String,
    val area: String,
    val puestoLabor: String,
    val tipoTrabajador: String
)