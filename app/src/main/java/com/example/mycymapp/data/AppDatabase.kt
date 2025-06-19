// com.example.mycymapp.data/AppDatabase.kt
package com.example.mycymapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Trabajador::class, RegistroDiario::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun trabajadorDao(): TrabajadorDao
    abstract fun registroDiarioDao(): RegistroDiarioDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cym_app_database"
                )
                    .fallbackToDestructiveMigration() // Importante para desarrollo
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}