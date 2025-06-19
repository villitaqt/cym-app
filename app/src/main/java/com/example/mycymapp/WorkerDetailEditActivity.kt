package com.example.mycymapp

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mycymapp.databinding.ActivityWorkerDetailEditBinding // ¡IMPORTANTE! Usar el binding del nuevo layout
import com.example.mycymapp.data.AppDatabase
import com.example.mycymapp.data.Trabajador
import com.example.mycymapp.data.TrabajadorDao // Importa el DAO directamente
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WorkerDetailEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWorkerDetailEditBinding
    private lateinit var trabajadorDao: TrabajadorDao

    private var currentWorkerDni: String? = null // Para almacenar el DNI del trabajador que estamos editando

    companion object {
        const val EXTRA_WORKER_DNI = "worker_dni" // Clave para el Intent
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityWorkerDetailEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = AppDatabase.getDatabase(applicationContext)
        trabajadorDao = database.trabajadorDao()

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Obtener el DNI del Intent (si viene de la lista de trabajadores)
        currentWorkerDni = intent.getStringExtra(EXTRA_WORKER_DNI)

        // Si hay un DNI, cargar los datos del trabajador para editar
        currentWorkerDni?.let { dni ->
            CoroutineScope(Dispatchers.IO).launch {
                val trabajador = trabajadorDao.getTrabajadorByDni(dni) // Asumo que tienes este método en tu DAO
                withContext(Dispatchers.Main) {
                    trabajador?.let {
                        // Rellenar los campos con los datos del trabajador
                        binding.editTextNombres.setText(it.nombres)
                        binding.editTextApellidos.setText(it.apellidos)
                        binding.editTextDni.setText(it.dni) // El DNI no será editable (enabled="false" en XML)
                        binding.editTextArea.setText(it.area)
                        binding.editTextPuestoLabor.setText(it.puestoLabor)
                        binding.editTextTipoTrabajador.setText(it.tipoTrabajador)

                        // Actualizar el título de la toolbar con el nombre del trabajador
                        supportActionBar?.title = "Editar: ${it.nombres} ${it.apellidos}"
                    } ?: run {
                        // Si no se encuentra el trabajador, mostrar un error y cerrar
                        Toast.makeText(this@WorkerDetailEditActivity, "Trabajador no encontrado.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
        } ?: run {
            // Si no se pasó DNI, esto es un error en esta Activity (no debería abrirse sin un DNI para editar)
            Toast.makeText(this, "Error: No se ha especificado el trabajador a editar.", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Lógica para el botón de Guardar/Actualizar
        binding.buttonSaveWorker.setOnClickListener {
            val nombres = binding.editTextNombres.text.toString().trim()
            val apellidos = binding.editTextApellidos.text.toString().trim()
            val dni = binding.editTextDni.text.toString().trim() // El DNI no se debería modificar, pero lo recuperamos
            val area = binding.editTextArea.text.toString().trim()
            val puestoLabor = binding.editTextPuestoLabor.text.toString().trim()
            val tipoTrabajador = binding.editTextTipoTrabajador.text.toString().trim()

            if (nombres.isEmpty() || apellidos.isEmpty() || dni.isEmpty() || area.isEmpty() || puestoLabor.isEmpty() || tipoTrabajador.isEmpty()) {
                Toast.makeText(this, "Por favor, complete todos los campos.", Toast.LENGTH_SHORT).show()
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // Crear el objeto Trabajador a actualizar
                        val trabajadorActualizado = Trabajador(
                            dni = dni, // Usamos el DNI original para la actualización
                            nombres = nombres,
                            apellidos = apellidos,
                            area = area,
                            puestoLabor = puestoLabor,
                            tipoTrabajador = tipoTrabajador
                        )
                        trabajadorDao.updateTrabajador(trabajadorActualizado) // Usar el método update del DAO

                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@WorkerDetailEditActivity, "Trabajador Actualizado con Éxito: $nombres $apellidos", Toast.LENGTH_LONG).show()
                            finish() // Cierra la Activity y regresa a la lista
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@WorkerDetailEditActivity, "Error al actualizar trabajador: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun showDeleteConfirmationDialog(dniToDelete: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Confirmar Eliminación")
            .setMessage("¿Estás seguro de que deseas eliminar este trabajador? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { dialog, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val trabajador = trabajadorDao.getTrabajadorByDni(dniToDelete)
                        trabajador?.let {
                            trabajadorDao.deleteTrabajador(it) // Asumo que tienes un método delete que recibe un Trabajador
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@WorkerDetailEditActivity, "Trabajador eliminado.", Toast.LENGTH_SHORT).show()
                                finish() // Cierra la Activity después de eliminar
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@WorkerDetailEditActivity, "Error al eliminar trabajador: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                        e.printStackTrace()
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}