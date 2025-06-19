package com.example.mycymapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts

// Importaciones para las clases del diálogo de edición
import android.app.AlertDialog
import android.widget.EditText
import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.util.Log // Para Log.d()

// Importaciones para Gson
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

import com.example.mycymapp.databinding.ActivityRegisterWorkersBinding
import com.example.mycymapp.databinding.ItemWorkerRecordBinding
import com.example.mycymapp.data.AppDatabase
import com.example.mycymapp.data.Trabajador
import com.example.mycymapp.data.RegistroDiario
import com.example.mycymapp.data.TrabajadorDao
import com.example.mycymapp.data.RegistroDiarioDao
import com.example.mycymapp.data.RegistroDiarioConTrabajador
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Data class para representar un trabajador registrado en la lista
data class WorkerRecord(
    val id: Int, // ID del RegistroDiario para actualizar en DB
    val dni: String,
    val nombres: String,
    val apellidos: String,
    val area: String,
    val puestoLabor: String,
    val tipoTrabajador: String,
    var cantidad: Int,
    val turno: String, // "Almuerzo" o "Cena"
    var tipoMenu: String, // "Común", "Vegetariano", etc. (editable por el usuario)
    val hora: String,
    val fecha: String // Formato DD/MM/YYYY para mostrar
)

class RegisterWorkersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterWorkersBinding
    private val currentRegisteredWorkers = mutableListOf<WorkerRecord>()
    private lateinit var workerAdapter: WorkerAdapter

    private lateinit var trabajadorDao: TrabajadorDao
    private lateinit var registroDiarioDao: RegistroDiarioDao
    private lateinit var sharedPreferences: SharedPreferences // Para la lista global de tipos de menú

    private var currentMealType: String = "" // "Almuerzo" o "Cena" (tipo de comida de la sesión)
    private lateinit var currentDateStringDB: String // Fecha del día actual en formato YYYY-MM-DD para DB
    private lateinit var currentDateStringDisplay: String // Fecha del día actual en formato DD/MM/YYYY para mostrar

    // Lista global de opciones de Tipo de Menú
    private val tipoMenuOptionsList = mutableListOf<String>()
    companion object {
        const val PREFS_TIPO_MENU_OPTIONS = "tipo_menu_options"
        const val KEY_TIPO_MENU_LIST = "key_tipo_menu_list"
        val DEFAULT_TIPO_MENU_OPTIONS = listOf("Común", "Vegetariano", "Especial")
        private const val TAG = "WorkerActivityDebug" // Etiqueta para los logs
    }

    // --- Launchers de ActivityResult API ---
    // Mover estas declaraciones aquí arriba, después de Companion Object, para que sean visibles
    // por las funciones que las llaman (como startQrScannerActivity)
    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permiso concedido, iniciar la actividad del escáner
                startQrScannerActivity()
            } else {
                // Permiso denegado, informar al usuario
                Toast.makeText(this, "Permiso de cámara denegado. No se puede escanear.", Toast.LENGTH_LONG).show()
            }
        }

    private val startQrScannerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val scannedDni = result.data?.getStringExtra("SCANNED_DNI")
                if (!scannedDni.isNullOrEmpty()) {
                    registerWorkerConsumption(scannedDni)
                } else {
                    Toast.makeText(this, "Escaneo cancelado o no se detectó DNI.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Escaneo cancelado.", Toast.LENGTH_SHORT).show()
            }
        }

    // --- Funciones de Lógica de Escáner y Permisos ---
    private fun startQrScannerActivity() {
        Log.d(TAG, "Iniciando escáner QR...")
        val intent = Intent(this, QrScannerActivity::class.java)
        startQrScannerLauncher.launch(intent)
    }

    private fun checkCameraPermissionAndStartScanner() {
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startQrScannerActivity()
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    // --- Fin Funciones de Lógica de Escáner y Permisos ---
// Nueva función para manejar el registro de consumo de un trabajador
    private fun registerWorkerConsumption(dni: String) {
        val initialTipoMenu = DEFAULT_TIPO_MENU_OPTIONS[0] // Por defecto "Común"

        // Iniciar una corrutina en el ámbito de la Activity, con un Dispatcher de IO para operaciones de BD
        lifecycleScope.launch(Dispatchers.IO) {
            val existingTrabajador = trabajadorDao.getTrabajadorByDni(dni)

            if (existingTrabajador == null) {
                // Si el trabajador no existe, mostrar un Toast en el hilo principal
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@RegisterWorkersActivity, "Trabajador con DNI $dni no encontrado. Por favor, regístrelo primero.", Toast.LENGTH_LONG).show()
                }
            } else {
                // Si el trabajador existe, intentar obtener su registro diario para el día y turno actuales
                val existingRegistro = registroDiarioDao.getRegistroDiario(dni, currentDateStringDB, currentMealType)
                val currentTime = SimpleDateFormat("HH:mm a", Locale.US).format(Date())

                if (existingRegistro != null) {
                    // El trabajador ya se registró para este turno hoy, sumar cantidad
                    existingRegistro.cantidad += 1 // Incrementar la cantidad
                    try {
                        registroDiarioDao.update(existingRegistro) // Actualizar en la base de datos
                        // Mostrar Toast de éxito en el hilo principal
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@RegisterWorkersActivity, "${existingTrabajador.nombres} ${existingTrabajador.apellidos} ya se registró para ${currentMealType} hoy. Sumando +1.", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al actualizar registro diario en DB: ${e.message}", e)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@RegisterWorkersActivity, "Error al sumar cantidad para ${existingTrabajador.nombres}: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    // Es el primer registro del trabajador para este turno hoy, insertar uno nuevo
                    val nuevoRegistro = RegistroDiario(
                        trabajadorDni = dni,
                        fecha = currentDateStringDB,
                        hora = currentTime,
                        turno = currentMealType, // "Almuerzo" o "Cena"
                        tipoMenu = initialTipoMenu, // Valor por defecto "Común"
                        cantidad = 1 // Cantidad inicial 1
                    )
                    try {
                        registroDiarioDao.insert(nuevoRegistro) // Insertar nuevo registro
                        // Mostrar Toast de éxito en el hilo principal
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@RegisterWorkersActivity, "Registrado: ${existingTrabajador.nombres} ${existingTrabajador.apellidos} para ${currentMealType}.", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al insertar nuevo registro diario en DB: ${e.message}", e)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@RegisterWorkersActivity, "Error al registrar ${existingTrabajador.nombres}: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewBinding = ActivityRegisterWorkersBinding.inflate(layoutInflater)
        binding = viewBinding
        setContentView(viewBinding.root)

        currentMealType = intent.getStringExtra(MainActivity.EXTRA_MEAL_TYPE) ?: "Desconocido"
        currentDateStringDB = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        currentDateStringDisplay = SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES")).format(Date())

        val database = AppDatabase.getDatabase(applicationContext)
        trabajadorDao = database.trabajadorDao()
        registroDiarioDao = database.registroDiarioDao()
        sharedPreferences = getSharedPreferences(PREFS_TIPO_MENU_OPTIONS, Context.MODE_PRIVATE)

        // Cargar opciones de tipo de menú al iniciar
        loadTipoMenuOptions()

        setSupportActionBar(viewBinding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        viewBinding.toolbar.title = "Registro de Trabajadores (${currentMealType})"
        viewBinding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Configurar RecyclerView
        workerAdapter = WorkerAdapter(currentRegisteredWorkers, tipoMenuOptionsList,
            onTipoMenuClick = { position ->
                Log.d(TAG, "Clic corto en posición: $position")
                val currentRecord = currentRegisteredWorkers[position]
                Log.d(TAG, "Registro actual (antes de iterar): $currentRecord")

                val currentIndex = tipoMenuOptionsList.indexOf(currentRecord.tipoMenu)
                Log.d(TAG, "Índice actual en opciones: $currentIndex")

                val nextIndex = if (currentIndex != -1 && currentIndex < tipoMenuOptionsList.size - 1) {
                    currentIndex + 1
                } else {
                    0 // Volver al inicio o default
                }
                val newTipoMenu = tipoMenuOptionsList[nextIndex]
                Log.d(TAG, "Nuevo Tipo Menú calculado: $newTipoMenu")

                updateTipoMenuForRegistroDiario(position, newTipoMenu)
            },
            onTipoMenuLongClick = { position ->
                Log.d(TAG, "Clic largo en posición: $position")
                showEditTipoMenuDialog(position)
            }
        )
        viewBinding.recyclerViewWorkers.layoutManager = LinearLayoutManager(this)
        viewBinding.recyclerViewWorkers.adapter = workerAdapter

        // Observar cambios en los registros diarios
        lifecycleScope.launch {
            registroDiarioDao.getRegistrosDiariosConTrabajador(currentDateStringDB, currentMealType)
                .collectLatest { registrosConTrabajador ->
                    val updatedList = mutableListOf<WorkerRecord>()
                    for (registro in registrosConTrabajador) {
                        updatedList.add(
                            WorkerRecord(
                                id = registro.id,
                                dni = registro.trabajadorDni,
                                nombres = registro.nombres,
                                apellidos = registro.apellidos,
                                area = registro.area,
                                puestoLabor = registro.puestoLabor,
                                tipoTrabajador = registro.tipoTrabajador,
                                cantidad = registro.cantidad,
                                turno = registro.turno,
                                tipoMenu = registro.tipoMenu, // Este es el tipo de menú del usuario
                                hora = registro.hora,
                                fecha = SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES")).format(SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(registro.fecha)!!)
                            )
                        )
                    }
                    withContext(Dispatchers.Main) {
                        currentRegisteredWorkers.clear()
                        currentRegisteredWorkers.addAll(updatedList)
                        workerAdapter.notifyDataSetChanged()
                    }
                }
        }

        viewBinding.buttonScanQr.setOnClickListener {
            checkCameraPermissionAndStartScanner() // Llama a la nueva función de verificación de permisos
        }

        viewBinding.buttonViewReports.setOnClickListener {
            Toast.makeText(this, "Navegar a la pantalla de Reportes.", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, ReportsActivity::class.java)
            startActivity(intent)
        }
    }

    // --- Lógica para la lista global de Tipo de Menú ---
    private fun loadTipoMenuOptions() {
        val json = sharedPreferences.getString(KEY_TIPO_MENU_LIST, null)
        val type = object : TypeToken<List<String>>() {}.type
        val storedList: List<String>? = Gson().fromJson(json, type)

        tipoMenuOptionsList.clear()
        if (storedList.isNullOrEmpty()) {
            tipoMenuOptionsList.addAll(DEFAULT_TIPO_MENU_OPTIONS)
            Log.d(TAG, "Cargando opciones de menú por defecto: $DEFAULT_TIPO_MENU_OPTIONS")
        } else {
            tipoMenuOptionsList.addAll(storedList)
            Log.d(TAG, "Cargando opciones de menú desde SharedPreferences: $storedList")
        }
    }

    private fun saveTipoMenuOptions() {
        val json = Gson().toJson(tipoMenuOptionsList)
        with(sharedPreferences.edit()) {
            putString(KEY_TIPO_MENU_LIST, json)
            apply()
        }
        Log.d(TAG, "Opciones de menú guardadas en SharedPreferences: $tipoMenuOptionsList")
    }

    private fun addTipoMenuOption(newOption: String) {
        if (!tipoMenuOptionsList.contains(newOption)) {
            tipoMenuOptionsList.add(newOption)
            saveTipoMenuOptions()
            Log.d(TAG, "Nueva opción '$newOption' añadida. Lista actualizada: $tipoMenuOptionsList")
            Toast.makeText(this, "Opción '$newOption' añadida a la lista de tipos de menú.", Toast.LENGTH_SHORT).show()
        } else {
            Log.d(TAG, "La opción '$newOption' ya existe en la lista. No se añadió.")
        }
    }

    // --- Lógica de actualización de Tipo de Menú para un registro específico ---
    private fun updateTipoMenuForRegistroDiario(position: Int, newTipoMenu: String) {
        val workerRecord = currentRegisteredWorkers[position]
        Log.d(TAG, "Actualizando tipo de menú para: ${workerRecord.nombres} a $newTipoMenu")

        lifecycleScope.launch(Dispatchers.IO) {
            val registroDiario = registroDiarioDao.getRegistroDiario(workerRecord.dni, currentDateStringDB, workerRecord.turno)
            if (registroDiario == null) {
                Log.e(TAG, "Error: Registro diario no encontrado para DNI: ${workerRecord.dni}, Fecha: ${currentDateStringDB}, Turno: ${workerRecord.turno}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@RegisterWorkersActivity, "Error: No se pudo encontrar el registro diario para actualizar.", Toast.LENGTH_LONG).show()
                }
            } else {
                Log.d(TAG, "Registro diario encontrado. ID: ${registroDiario.id}, Tipo Menú actual: ${registroDiario.tipoMenu}")
                registroDiario.tipoMenu = newTipoMenu
                try {
                    registroDiarioDao.update(registroDiario)
                    Log.d(TAG, "Registro diario actualizado en DB: ID ${registroDiario.id}, Nuevo Tipo Menú: ${registroDiario.tipoMenu}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@RegisterWorkersActivity, "Tipo de menú de ${workerRecord.nombres} actualizado a '$newTipoMenu'", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error al actualizar registro diario en DB: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@RegisterWorkersActivity, "Error al guardar el tipo de menú: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    // --- Diálogo para editar el Tipo de Menú ---
    private fun showEditTipoMenuDialog(position: Int) {
        val currentRecord = currentRegisteredWorkers[position]
        val editText = EditText(this)
        editText.setText(currentRecord.tipoMenu)
        editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        editText.imeOptions = EditorInfo.IME_ACTION_DONE

        val dialog = AlertDialog.Builder(this)
            .setTitle("Editar Tipo de Menú")
            .setView(editText)
            .setPositiveButton("Guardar") { dialog, _ ->
                val newText = editText.text.toString().trim()
                if (newText.isNotEmpty()) {
                    addTipoMenuOption(newText)
                    updateTipoMenuForRegistroDiario(position, newText)
                } else {
                    Toast.makeText(this, "El tipo de menú no puede estar vacío.", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.cancel()
            }
            .create()

        dialog.show()

        editText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
                true
            } else {
                false
            }
        }
    }
}

// Adaptador para el RecyclerView
class WorkerAdapter(
    private val currentRegisteredWorkers: MutableList<WorkerRecord>,
    private val tipoMenuOptions: List<String>,
    private val onTipoMenuClick: (position: Int) -> Unit,
    private val onTipoMenuLongClick: (position: Int) -> Unit
) : RecyclerView.Adapter<WorkerAdapter.WorkerViewHolder>() {

    private val TAG = "WorkerAdapterDebug" // Etiqueta para los logs del Adapter

    class WorkerViewHolder(private val binding: ItemWorkerRecordBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val TAG = "ViewHolderDebug" // Etiqueta para los logs del ViewHolder
        fun bind(
            worker: WorkerRecord,
            onTipoMenuClick: (position: Int) -> Unit,
            onTipoMenuLongClick: (position: Int) -> Unit
        ) {
            Log.d(TAG, "Binding worker: ${worker.nombres}, Tipo Menú: ${worker.tipoMenu}")

            // Campos principales que SÍ se muestran
            binding.textViewWorkerName.text = "${worker.nombres} ${worker.apellidos} (${worker.cantidad})"
            binding.textViewWorkerTime.text = worker.hora

            // Campo "Tipo Menú" interactivo
            binding.textViewTipoMenuButton.text = worker.tipoMenu
            binding.textViewTipoMenuButton.setOnClickListener { onTipoMenuClick(adapterPosition) }
            binding.textViewTipoMenuButton.setOnLongClickListener {
                onTipoMenuLongClick(adapterPosition)
                true
            }

            // Los campos detallados del GridLayout se han eliminado del XML
            // Estas líneas fueron eliminadas del código previamente (están comentadas en tu código real)
            // binding.textViewDni.text = worker.dni
            // binding.textViewTipoTrabajador.text = worker.tipoTrabajador
            // binding.textViewArea.text = worker.area
            // binding.textViewPuestoLabor.text = worker.puestoLabor
            // binding.textViewCantidad.text = worker.cantidad.toString()
            // binding.textViewFecha.text = worker.fecha
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkerViewHolder {
        Log.d(TAG, "Creando nuevo ViewHolder")
        val binding = ItemWorkerRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WorkerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WorkerViewHolder, position: Int) {
        Log.d(TAG, "Binding ViewHolder en posición: $position")
        holder.bind(currentRegisteredWorkers[position], onTipoMenuClick, onTipoMenuLongClick)
    }

    override fun getItemCount(): Int {
        Log.d(TAG, "getItemCount: ${currentRegisteredWorkers.size}")
        return currentRegisteredWorkers.size
    }
}