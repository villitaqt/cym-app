// com.example.mycymapp.data/TrabajadorDao.kt
package com.example.mycymapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow // Usaremos Flow para observar cambios en los datos

@Dao
interface TrabajadorDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) // Reemplaza si ya existe el DNI
    suspend fun insert(trabajador: Trabajador)

    @Query("SELECT * FROM trabajadores WHERE dni = :dni LIMIT 1")
    suspend fun getTrabajadorByDni(dni: String): Trabajador?

    @Query("SELECT * FROM trabajadores ORDER BY apellidos ASC")
    fun getAllTrabajadores(): Flow<List<Trabajador>> // Flow para observar la lista


    // En TrabajadorDao
    @Query("SELECT * FROM trabajadores ORDER BY nombres ASC, apellidos ASC")
    fun getAllTrabajadoresOrderedByNames(): Flow<List<Trabajador>> // Si tu entidad se llama Trabajador

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrabajador(trabajador: Trabajador)

    @Update
    suspend fun updateTrabajador(trabajador: Trabajador)

    @Delete
    suspend fun deleteTrabajador(trabajador: Trabajador)

}