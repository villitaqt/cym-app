package com.example.mycymapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mycymapp.databinding.ActivityDailyReportDetailsBinding
import com.example.mycymapp.data.AppDatabase
import com.example.mycymapp.data.RegistroDiarioDao
import com.example.mycymapp.data.RegistroDiarioConTrabajador
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.Locale

// ¡IMPORTACIONES FALTANTES AQUÍ ABAJO!
import android.widget.Toast // <-- ¡IMPORTACIÓN NECESARIA!
import android.view.LayoutInflater // <-- ¡NECESARIO PARA ADAPTER!
import android.view.ViewGroup // <-- ¡NECESARIO PARA ADAPTER!
import androidx.recyclerview.widget.RecyclerView // <-- ¡ASEGÚRATE DE QUE ESTÉ!
import com.example.mycymapp.databinding.ItemDailyReportDetailBinding // <-- ¡NECESARIO PARA ADAPTER BINDING!


// Data class para representar un ítem en la lista de detalles del reporte diario
data class DailyReportDetailItem(
    val id: Int,
    val dni: String,
    val nombres: String,
    val apellidos: String,
    val area: String,
    val puestoLabor: String,
    val tipoTrabajador: String,
    val cantidad: Int,
    val turno: String, // "Almuerzo" o "Cena"
    val tipoMenu: String, // "Común", "Vegetariano", etc.
    val hora: String,
    val fechaDisplay: String // Fecha en formato DD/MM/YYYY para mostrar
)

class DailyReportDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDailyReportDetailsBinding
    private lateinit var registroDiarioDao: RegistroDiarioDao
    private val reportDetailsList = mutableListOf<DailyReportDetailItem>()
    private lateinit var reportDetailsAdapter: DailyReportDetailsAdapter

    private lateinit var selectedDateDBFormat: String // Fecha en formato YYYY-MM-DD (recibida del Intent)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewBinding = ActivityDailyReportDetailsBinding.inflate(layoutInflater)
        binding = viewBinding
        setContentView(viewBinding.root)

        // Obtener la fecha del Intent
        selectedDateDBFormat = intent.getStringExtra(ReportsActivity.EXTRA_REPORT_DATE) ?: ""

        if (selectedDateDBFormat.isEmpty()) {
            Toast.makeText(this, "Error: No se ha especificado la fecha del reporte.", Toast.LENGTH_SHORT).show()
            finish() // Cierra la actividad si no hay fecha
            return
        }

        // Inicializar DAO
        val database = AppDatabase.getDatabase(applicationContext)
        registroDiarioDao = database.registroDiarioDao()

        // Configurar la Toolbar
        setSupportActionBar(viewBinding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        // Formatear la fecha para el título del Toolbar (ej. "sábado, 20 de julio de 2024")
        val displayDate = try {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(selectedDateDBFormat)
            // Corrección: Asegura que el formato del año sea correcto ("yyyy" no "yyy")
            SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", Locale("es", "ES")).format(date)
        } catch (e: Exception) {
            selectedDateDBFormat // Si hay error en formato, muestra la fecha original
        }
        viewBinding.toolbar.title = "Detalles del Reporte - $displayDate"
        viewBinding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Configurar RecyclerView
        reportDetailsAdapter = DailyReportDetailsAdapter(reportDetailsList)
        viewBinding.recyclerViewDailyReportDetails.layoutManager = LinearLayoutManager(this)
        viewBinding.recyclerViewDailyReportDetails.adapter = reportDetailsAdapter

        // Observar los registros diarios para la fecha seleccionada (Almuerzos y Cenas)
        lifecycleScope.launch {
            // Se obtendrán los registros de almuerzo
            registroDiarioDao.getRegistrosDiariosConTrabajador(selectedDateDBFormat, "Almuerzo")
                .collectLatest { almuerzos ->
                    // Se obtendrán los registros de cena
                    registroDiarioDao.getRegistrosDiariosConTrabajador(selectedDateDBFormat, "Cena")
                        .collectLatest { cenas ->
                            val combinedList = mutableListOf<RegistroDiarioConTrabajador>()
                            combinedList.addAll(almuerzos)
                            combinedList.addAll(cenas)

                            val updatedDisplayList = mutableListOf<DailyReportDetailItem>()
                            for (registro in combinedList.sortedBy { it.hora }) { // Ordenar por hora
                                updatedDisplayList.add(
                                    DailyReportDetailItem(
                                        id = registro.id,
                                        dni = registro.trabajadorDni,
                                        nombres = registro.nombres,
                                        apellidos = registro.apellidos,
                                        area = registro.area,
                                        puestoLabor = registro.puestoLabor,
                                        tipoTrabajador = registro.tipoTrabajador,
                                        cantidad = registro.cantidad,
                                        turno = registro.turno,
                                        tipoMenu = registro.tipoMenu,
                                        hora = registro.hora,
                                        fechaDisplay = SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES")).format(SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(registro.fecha)!!)
                                    )
                                )
                            }
                            withContext(Dispatchers.Main) {
                                reportDetailsList.clear()
                                reportDetailsList.addAll(updatedDisplayList)
                                reportDetailsAdapter.notifyDataSetChanged() // <-- notifyDataSetChanged funciona aquí
                            }
                        }
                }
        }
    }
}

class DailyReportDetailsAdapter(
    private val reportDetailsList: MutableList<DailyReportDetailItem>
) : RecyclerView.Adapter<DailyReportDetailsAdapter.DailyReportDetailViewHolder>() {

    class DailyReportDetailViewHolder(private val binding: ItemDailyReportDetailBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DailyReportDetailItem) {
            // Nombre y Apellidos
            binding.textViewNameDisplay.text = "${item.nombres} ${item.apellidos}"
            binding.textViewCantidadDisplay.text = "(${item.cantidad})"
            // El label "cantidad" en el XML es textViewCantidadLabel, no se llena con datos

            // DNI y Tipo de Trabajador
            binding.textViewDni.text = item.dni // Aquí textViewDni es el valor
            // El label "DNI:" en el XML es tvDniLabel, no se llena con datos
            binding.textViewTipoTrabajador.text = item.tipoTrabajador // Aquí textViewTipoTrabajador es el valor
            // El label "Tipo:" en el XML es tvTipoTrabajadorLabel, no se llena con datos

            // Área y Puesto/Labor
            binding.textViewArea.text = item.area // Aquí textViewArea es el valor
            // El label "Área:" en el XML es tvAreaLabel, no se llena con datos
            binding.textViewPuestoLabor.text = item.puestoLabor // Aquí textViewPuestoLabor es el valor
            // El label "Puesto:" en el XML es tvPuestoLaborLabel, no se llena con datos

            // Tipo Menú (usuario), Turno (Almuerzo/Cena) y Hora
            binding.textViewTipoMenu.text = item.tipoMenu // Aquí textViewTipoMenu es el valor
            // El label "Menú:" en el XML es tvTipoMenuLabel, no se llena con datos
            binding.textViewTurno.text = item.turno // Aquí textViewTurno es el valor
            // El label "Turno:" en el XML es tvTurnoLabel, no se llena con datos
            binding.textViewHora.text = item.hora // Aquí textViewHora es el valor

            // Si decidiste agregar la fecha de display en el item layout:
            // binding.textViewFechaDisplay.text = item.fechaDisplay
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DailyReportDetailViewHolder {
        val binding = ItemDailyReportDetailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DailyReportDetailViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DailyReportDetailViewHolder, position: Int) {
        holder.bind(reportDetailsList[position])
    }

    override fun getItemCount(): Int = reportDetailsList.size
}