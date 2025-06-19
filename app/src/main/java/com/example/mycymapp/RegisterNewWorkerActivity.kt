package com.example.mycymapp

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mycymapp.databinding.ActivityRegisterNewWorkerBinding
import com.example.mycymapp.data.AppDatabase // Importa tu clase de base de datos
import com.example.mycymapp.data.Trabajador // Importa tu entidad Trabajador
import kotlinx.coroutines.CoroutineScope // Importa CoroutineScope
import kotlinx.coroutines.Dispatchers // Importa Dispatchers
import kotlinx.coroutines.launch // Importa launch
import kotlinx.coroutines.withContext // Importa withContext

class RegisterNewWorkerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterNewWorkerBinding
    // Instancia del DAO para interactuar con la tabla de trabajadores
    private lateinit var trabajadorDao: com.example.mycymapp.data.TrabajadorDao // ¡CORRECTO! Importa la interfaz directamente.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewBinding = ActivityRegisterNewWorkerBinding.inflate(layoutInflater)
        binding = viewBinding
        setContentView(viewBinding.root)

        // Obtener la instancia de la base de datos y del DAO
        val database = AppDatabase.getDatabase(applicationContext)
        trabajadorDao = database.trabajadorDao()

        setSupportActionBar(viewBinding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        viewBinding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        viewBinding.buttonSaveWorker.setOnClickListener {
            val nombres = viewBinding.editTextNombres.text.toString().trim()
            val apellidos = viewBinding.editTextApellidos.text.toString().trim()
            val dni = viewBinding.editTextDni.text.toString().trim()
            val area = viewBinding.editTextArea.text.toString().trim()
            val puestoLabor = viewBinding.editTextPuestoLabor.text.toString().trim()
            val tipoTrabajador = viewBinding.editTextTipoTrabajador.text.toString().trim()

            if (nombres.isEmpty() || apellidos.isEmpty() || dni.isEmpty() || area.isEmpty() || puestoLabor.isEmpty() || tipoTrabajador.isEmpty()) {
                Toast.makeText(this, "Por favor, complete todos los campos.", Toast.LENGTH_SHORT).show()
            } else {
                // Iniciar una corrutina para realizar la operación de base de datos
                CoroutineScope(Dispatchers.IO).launch { // Dispatchers.IO es para operaciones de I/O (BD, red)
                    try {
                        val nuevoTrabajador = Trabajador(
                            dni = dni,
                            nombres = nombres,
                            apellidos = apellidos,
                            area = area,
                            puestoLabor = puestoLabor,
                            tipoTrabajador = tipoTrabajador
                        )
                        trabajadorDao.insert(nuevoTrabajador) // Insertar en la BD

                        // Volver al hilo principal (UI) para mostrar el Toast
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@RegisterNewWorkerActivity, "Trabajador Guardado con Éxito: $nombres $apellidos", Toast.LENGTH_LONG).show()
                            // Limpiar los campos después de guardar
                            viewBinding.editTextNombres.text?.clear()
                            viewBinding.editTextApellidos.text?.clear()
                            viewBinding.editTextDni.text?.clear()
                            viewBinding.editTextArea.text?.clear()
                            viewBinding.editTextPuestoLabor.text?.clear()
                            viewBinding.editTextTipoTrabajador.text?.clear()
                            // Volver a la pantalla anterior
                            finish()
                        }
                    } catch (e: Exception) {
                        // Manejar cualquier error durante la inserción
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@RegisterNewWorkerActivity, "Error al guardar trabajador: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                        e.printStackTrace() // Imprimir la traza del error en Logcat
                    }
                }
            }
        }
    }
}