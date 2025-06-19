package com.example.mycymapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mycymapp.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Importa la nueva Activity para la lista de trabajadores
import com.example.mycymapp.WorkersListActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        const val PREFS_NAME = "MyCymAppPrefs"
        const val KEY_LAST_LUNCH_DATE = "last_lunch_date"
        const val KEY_LAST_DINNER_DATE = "last_dinner_date"
        // Nueva clave para pasar el tipo de comida a RegisterWorkersActivity
        const val EXTRA_MEAL_TYPE = "meal_type"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Lógica para Registrar Almuerzo
        binding.cardRegisterLunch.setOnClickListener {
            val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val lastLunchDate = sharedPreferences.getString(KEY_LAST_LUNCH_DATE, null)

            with (sharedPreferences.edit()) {
                putString(KEY_LAST_LUNCH_DATE, todayDate)
                apply()
            }

            if (lastLunchDate == todayDate) {
                Toast.makeText(this, "El almuerzo ya fue registrado hoy. Dirigiendo a registro de trabajadores.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Iniciando registro de Almuerzo.", Toast.LENGTH_SHORT).show()
            }

            val intent = Intent(this, RegisterWorkersActivity::class.java)
            intent.putExtra(EXTRA_MEAL_TYPE, "Almuerzo")
            startActivity(intent)
        }

        // Lógica para Registrar Cena
        binding.cardRegisterDinner.setOnClickListener {
            val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val lastDinnerDate = sharedPreferences.getString(KEY_LAST_DINNER_DATE, null)

            with (sharedPreferences.edit()) {
                putString(KEY_LAST_DINNER_DATE, todayDate)
                apply()
            }

            if (lastDinnerDate == todayDate) {
                Toast.makeText(this, "La cena ya fue registrada hoy. Dirigiendo a registro de trabajadores.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Iniciando registro de Cena.", Toast.LENGTH_SHORT).show()
            }

            val intent = Intent(this, RegisterWorkersActivity::class.java)
            intent.putExtra(EXTRA_MEAL_TYPE, "Cena")
            startActivity(intent)
        }

        // Navegación a Añadir Nuevo Trabajador (tu RegisterNewWorkerActivity)
        binding.cardRegisterWorker.setOnClickListener {
            Toast.makeText(this, "Navegar a Añadir Nuevo Trabajador", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, RegisterNewWorkerActivity::class.java)
            startActivity(intent)
        }

        // Navegación a Reportes
        binding.cardReports.setOnClickListener {
            Toast.makeText(this, "Navegar a Reportes", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, ReportsActivity::class.java)
            startActivity(intent)
        }

        // ¡NUEVA NAVEGACIÓN A VER TRABAJADORES!
        binding.cardViewWorkers.setOnClickListener {
            Toast.makeText(this, "Navegar a Ver Trabajadores", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, WorkersListActivity::class.java)
            startActivity(intent)
        }
    }
}