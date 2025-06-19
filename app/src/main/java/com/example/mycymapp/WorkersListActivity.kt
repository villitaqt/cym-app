package com.example.mycymapp // ¡MUY IMPORTANTE que este paquete sea correcto!

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.lifecycle.lifecycleScope // Para coroutines con ciclo de vida
import com.example.mycymapp.data.AppDatabase // Tu base de datos
import com.example.mycymapp.data.TrabajadorDao // Tu DAO de Trabajador
import com.example.mycymapp.data.Trabajador // Tu entidad Trabajador
import com.example.mycymapp.databinding.ActivityWorkersListBinding // Binding de tu Activity
import com.example.mycymapp.adapters.WorkersListAdapter // ¡Importa tu adaptador desde el paquete 'adapters'!
import kotlinx.coroutines.Dispatchers // Para despachadores de coroutines
import kotlinx.coroutines.flow.collectLatest // Para recolectar Flows
import kotlinx.coroutines.launch // Para lanzar coroutines
import kotlinx.coroutines.withContext // Para cambiar de contexto en coroutines

// Importa tu Activity de registro de trabajadores (RegisterNewWorkerActivity)
// Suponiendo que está en el mismo paquete 'com.example.mycymapp'
import com.example.mycymapp.RegisterNewWorkerActivity


class WorkersListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWorkersListBinding
    private lateinit var trabajadorDao: TrabajadorDao
    private lateinit var workersListAdapter: WorkersListAdapter // Instancia de tu adaptador

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar el View Binding para esta Activity
        binding = ActivityWorkersListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar DAO de la base de datos
        val database = AppDatabase.getDatabase(applicationContext)
        trabajadorDao = database.trabajadorDao()

        // Configurar la Toolbar
        setSupportActionBar(binding.toolbarWorkers)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Habilita el botón de regreso
        supportActionBar?.title = "Trabajadores Registrados" // Título de la Toolbar
        binding.toolbarWorkers.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed() // Acción al hacer clic en el botón de regreso
        }

        // Configurar RecyclerView
        // Inicializar el adaptador. El lambda define qué hacer al hacer clic en un ítem de la lista.
        workersListAdapter = WorkersListAdapter(mutableListOf()) { trabajador -> // Recibe Trabajador
            // Iniciar la Activity para ver/editar detalles del trabajador
            val intent = Intent(this, WorkerDetailEditActivity::class.java).apply {
                putExtra(WorkerDetailEditActivity.EXTRA_WORKER_DNI, trabajador.dni) // Pasa el DNI
            }
            startActivity(intent)
            // Ya no necesitas el Toast de "funcionalidad no implementada" aquí
            // Toast.makeText(this, "Funcionalidad de detalle/edición no implementada aún para ${trabajador.nombres}", Toast.LENGTH_SHORT).show()
        }
        binding.recyclerViewWorkers.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewWorkers.adapter = workersListAdapter

        // Observar los trabajadores de la base de datos utilizando Kotlin Coroutines y Flow
        lifecycleScope.launch {
            // trabjadorDao.getAllTrabajadores() debe devolver un Flow<List<Trabajador>>
            trabajadorDao.getAllTrabajadores().collectLatest { trabajadores ->
                // Actualiza la lista en el adaptador en el hilo principal (UI)
                withContext(Dispatchers.Main) {
                    workersListAdapter.updateList(trabajadores)
                }
            }
        }

        // Configurar el FloatingActionButton para añadir nuevos trabajadores
        binding.fabAddWorker.setOnClickListener {
            // Inicia la Activity para registrar un nuevo trabajador
            val intent = Intent(this, RegisterNewWorkerActivity::class.java) // ¡Usa tu Activity existente!
            startActivity(intent)
        }
    }
}