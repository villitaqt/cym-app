package com.example.mycymapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mycymapp.databinding.ActivityReportsBinding
import com.example.mycymapp.databinding.ItemReportSummaryBinding // Binding para el item de la lista
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar
import com.example.mycymapp.data.AppDatabase // Importar tu clase de base de datos
import com.example.mycymapp.data.RegistroDiarioDao // Importar tu DAO de RegistroDiario
import kotlinx.coroutines.launch // Importar launch
import kotlinx.coroutines.flow.collectLatest // Importar collectLatest
import kotlinx.coroutines.withContext // Importar withContext
import kotlinx.coroutines.Dispatchers // Importar Dispatchers
import androidx.lifecycle.lifecycleScope // Importar lifecycleScope
import android.net.Uri // Necesario para compartir archivos
import androidx.core.content.FileProvider // Necesario para compartir archivos
import java.io.File // Para manejar archivos
import java.io.FileOutputStream // Para escribir archivos
import java.io.OutputStreamWriter // Para escribir CSV
import android.util.Log
import com.example.mycymapp.data.RegistroDiarioConTrabajador


// Data class ReportSummary (ya la tienes, solo para referencia)
data class ReportSummary(
    val date: String, // Para display (Ej. "sábado, 20 de julio de 2024")
    val dateDBFormat: String, // Para pasar a la BD (Ej. "2024-07-20")
    val totalLunches: Int,
    val totalDinners: Int,
    var isSelected: Boolean = false
)

class ReportsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportsBinding
    private lateinit var reportAdapter: ReportAdapter
    private val reportList = mutableListOf<ReportSummary>()

    private lateinit var registroDiarioDao: RegistroDiarioDao

    companion object {
        const val EXTRA_REPORT_DATE = "report_date"
        private const val REPORT_FILE_AUTHORITY = "com.example.mycymapp.fileprovider" // Debe coincidir con AndroidManifest.xml
    }
    // Formateadores de fecha
    private val displayDateFormat = SimpleDateFormat("MMM d,yyyy", Locale.US)
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewBinding = ActivityReportsBinding.inflate(layoutInflater)
        binding = viewBinding
        setContentView(viewBinding.root)

        val database = AppDatabase.getDatabase(applicationContext)
        registroDiarioDao = database.registroDiarioDao()

        setSupportActionBar(viewBinding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        viewBinding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Configurar Spinner de Selección Rápida
        val quickRangeOptions = resources.getStringArray(R.array.quick_date_ranges)
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, quickRangeOptions)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        viewBinding.spinnerQuickSelection.adapter = spinnerAdapter

        viewBinding.spinnerQuickSelection.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val selectedOption = quickRangeOptions[position]
                val endDate = Calendar.getInstance()
                val startDate = Calendar.getInstance()

                when (selectedOption) {
                    "Últimos 7 días" -> startDate.add(Calendar.DAY_OF_YEAR, -6)
                    "Últimos 30 días" -> startDate.add(Calendar.DAY_OF_YEAR, -29)
                    "Este Mes" -> startDate.set(Calendar.DAY_OF_MONTH, 1)
                    "Mes Anterior" -> {
                        startDate.add(Calendar.MONTH, -1)
                        startDate.set(Calendar.DAY_OF_MONTH, 1)
                        endDate.set(Calendar.MONTH, endDate.get(Calendar.MONTH) - 1)
                        endDate.set(Calendar.DAY_OF_MONTH, endDate.getActualMaximum(Calendar.DAY_OF_MONTH))
                    }
                    "Todo el Tiempo" -> {
                        startDate.set(1970, Calendar.JANUARY, 1)
                    }
                }
                viewBinding.editTextDateRange.setText("${displayDateFormat.format(startDate.time)} - ${displayDateFormat.format(endDate.time)}")
                // Aquí se llamaría a una función para filtrar el RecyclerView (futuro)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })

        viewBinding.editTextDateRange.setOnClickListener {
            Toast.makeText(this, "¡Clic en Rango de Fechas detectado!", Toast.LENGTH_SHORT).show()
            showDateRangePicker()
        }

        // Configurar RecyclerView principal de Reportes
        reportAdapter = ReportAdapter(reportList,
            onCheckboxChanged = { position, isChecked ->
                reportList[position].isSelected = isChecked
                // Actualizar visibilidad de la barra de acciones inferior
                updateBottomActionBarVisibility()
            },
            onViewDetailsClick = { dateString ->
                val intent = Intent(this, DailyReportDetailsActivity::class.java)
                intent.putExtra(EXTRA_REPORT_DATE, dateString)
                startActivity(intent)
            }
        )
        viewBinding.recyclerViewReportSummaries.layoutManager = LinearLayoutManager(this)
        viewBinding.recyclerViewReportSummaries.adapter = reportAdapter

        // ¡ELIMINADO loadSampleReportData()! Ahora cargamos desde la DB.
        // loadSampleReportData()

        // Observar la base de datos
        lifecycleScope.launch {
            registroDiarioDao.getDailySummaries().collectLatest { dailySummaries ->
                val updatedReportList = mutableListOf<ReportSummary>()
                val dateFormatDisplay = SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", Locale("es", "ES"))
                val dateFormatDB = SimpleDateFormat("yyyy-MM-dd", Locale.US)

                for (summaryTuple in dailySummaries) {
                    val dateObj = dateFormatDB.parse(summaryTuple.fecha)
                    val dateForDisplay = dateFormatDisplay.format(dateObj)

                    // Preservar el estado de selección si el elemento ya existía
                    val existingSummary = reportList.find { it.dateDBFormat == summaryTuple.fecha }
                    val isSelected = existingSummary?.isSelected ?: false

                    updatedReportList.add(
                        ReportSummary(
                            date = dateForDisplay,
                            dateDBFormat = summaryTuple.fecha,
                            totalLunches = summaryTuple.totalLunches,
                            totalDinners = summaryTuple.totalDinners,
                            isSelected = isSelected // Mantiene el estado de selección
                        )
                    )
                }
                withContext(Dispatchers.Main) {
                    reportList.clear()
                    reportList.addAll(updatedReportList)
                    reportAdapter.notifyDataSetChanged()
                    updateBottomActionBarVisibility() // Actualizar visibilidad después de cargar/actualizar la lista
                    if (reportList.isEmpty()) {
                        Toast.makeText(this@ReportsActivity, "No hay reportes disponibles.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        viewBinding.toggleButtonGroup.addOnButtonCheckedListener { toggleGroup, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.button_filter_by_shift -> Toast.makeText(this, "Filtrar por Turnos", Toast.LENGTH_SHORT).show()
                    R.id.button_filter_by_worker -> Toast.makeText(this, "Filtrar por Trabajadores", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Listener para los botones de Descargar y Compartir
        viewBinding.buttonDownloadReport.setOnClickListener {
            downloadSelectedReports()
        }
        viewBinding.buttonShareReport.setOnClickListener {
            shareSelectedReports()
        }
    }

    // -- Lógica para mostrar/ocultar la barra de acciones inferior --
    private fun updateBottomActionBarVisibility() {
        val selectedCount = reportList.count { it.isSelected }
        if (selectedCount > 0) {
            binding.bottomActionBar.visibility = android.view.View.VISIBLE
        } else {
            binding.bottomActionBar.visibility = android.view.View.GONE
        }
    }

    // -- Lógica para generar y descargar/compartir el reporte CSV --
    private val CREATE_FILE_REQUEST_CODE = 1
    private fun generateReportCsv(): String {
        val selectedSummaries = reportList.filter { it.isSelected }.sortedBy { it.dateDBFormat }
        val csvBuilder = StringBuilder()

        val header = "ITEM;DNI;APELLIDOS, NOMBRE;TIPO;ÁREA;PUESTO/LABOR;CANTIDAD;TIPO MENÚ;FECHA"
        val dateFormatForExport = SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES")) // Formato DD/MM/YYYY para la exportación

        for (summary in selectedSummaries) {
            // Obtener todos los registros del día para almuerzo y cena
            lifecycleScope.launch(Dispatchers.IO) { // Ejecutar DB ops en IO
                val almuerzos = registroDiarioDao.getRegistrosDiariosConTrabajador(summary.dateDBFormat, "Almuerzo").collectLatest { records ->
                    // Escribir sección de Almuerzo
                    csvBuilder.append("ALMUERZO\n")
                    csvBuilder.append(header).append("\n")
                    var itemCounter = 1
                    for (record in records.sortedBy { it.hora }) {
                        val nombresCompletos = "${record.apellidos}, ${record.nombres}"
                        val fechaExport = SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES")).format(SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(record.fecha)!!)
                        csvBuilder.append("$itemCounter;${record.trabajadorDni};\"$nombresCompletos\";${record.tipoTrabajador};${record.area};${record.puestoLabor};${record.cantidad};${record.tipoMenu};$fechaExport\n")
                        itemCounter++
                    }
                    csvBuilder.append("\n\n\n") // 3 filas de separación

                    // Escribir sección de Cena (similares, pero necesitaríamos otra collectLatest o combinar)
                    // Esto es un poco más complejo con Flows. Lo simplificamos a una sola consulta para exportación
                }
            }
        }
        // El problema es que collectLatest es asíncrono y no se puede esperar su resultado para devolver un String.
        // Necesitamos una consulta suspendida que devuelva los datos directamente.

        // ¡REDISEÑO DE GENERATEReportCsv!
        // No podemos usar collectLatest aquí directamente, necesitamos una consulta suspendida que devuelva la lista completa.
        // Y debemos hacerlo síncrono para la generación del CSV.
        // Vamos a reestructurar esta función.
        val combinedRecords = mutableListOf<RegistroDiarioConTrabajador>()

        // Para cada día seleccionado, obtener todos sus registros
        for (summary in selectedSummaries) {
            lifecycleScope.launch(Dispatchers.IO) { // Esto es un problema, launch es asíncrono.
                val recordsAlmuerzo = registroDiarioDao.getRegistrosDiariosConTrabajador(summary.dateDBFormat, "Almuerzo").collectLatest { combinedRecords.addAll(it) } // No funciona así
                val recordsCena = registroDiarioDao.getRegistrosDiariosConTrabajador(summary.dateDBFormat, "Cena").collectLatest { combinedRecords.addAll(it) } // No funciona así
            }
        }
        // Este enfoque con collectLatest dentro de un bucle no funciona para construir un String de retorno.
        // Necesitamos un método en el DAO que simplemente devuelva List<...> (suspend fun).
        // Modificaré el DAO y luego esta función.
        return "" // Placeholder
    }

    // --- Versión corregida de generateReportCsv - REQUIERE NUEVO DAO MÉTODO ---
    private suspend fun generateReportCsvContent(): String {
        val selectedSummaries = reportList.filter { it.isSelected }.sortedBy { it.dateDBFormat }
        val csvBuilder = StringBuilder()

        val header = "ITEM;DNI;APELLIDOS, NOMBRE;TIPO;ÁREA;PUESTO/LABOR;CANTIDAD;TIPO MENÚ;FECHA"
        val dateFormatForExport = SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES"))

        for (summary in selectedSummaries) {
            // Obtener registros para Almuerzo y Cena de forma síncrona (suspend fun)
            val almuerzoRecords = registroDiarioDao.getRegistrosByDateAndTurnoSingle(summary.dateDBFormat, "Almuerzo")
            val cenaRecords = registroDiarioDao.getRegistrosByDateAndTurnoSingle(summary.dateDBFormat, "Cena")

            // Escribir sección de Almuerzo
            if (almuerzoRecords.isNotEmpty()) {
                csvBuilder.append("ALMUERZO\n")
                csvBuilder.append(header).append("\n")
                var itemCounter = 1
                for (record in almuerzoRecords.sortedBy { it.hora }) {
                    val nombresCompletos = "${record.apellidos}, ${record.nombres}"
                    val fechaExport = dateFormatForExport.format(SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(record.fecha)!!)
                    csvBuilder.append("$itemCounter;${record.trabajadorDni};\"$nombresCompletos\";${record.tipoTrabajador};${record.area};${record.puestoLabor};${record.cantidad};${record.tipoMenu};$fechaExport\n")
                    itemCounter++
                }
            }

            // Separación entre secciones (si hay una sección de Almuerzo y/o si hay Cena)
            if (almuerzoRecords.isNotEmpty() || cenaRecords.isNotEmpty()) {
                csvBuilder.append("\n\n\n") // 3 filas de separación
            }

            // Escribir sección de Cena
            if (cenaRecords.isNotEmpty()) {
                csvBuilder.append("CENA\n")
                csvBuilder.append(header).append("\n")
                var itemCounter = 1
                for (record in cenaRecords.sortedBy { it.hora }) {
                    val nombresCompletos = "${record.apellidos}, ${record.nombres}"
                    val fechaExport = dateFormatForExport.format(SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(record.fecha)!!)
                    csvBuilder.append("$itemCounter;${record.trabajadorDni};\"$nombresCompletos\";${record.tipoTrabajador};${record.area};${record.puestoLabor};${record.cantidad};${record.tipoMenu};$fechaExport\n")
                    itemCounter++
                }
            }
        }
        return csvBuilder.toString()
    }

    // --- Lógica de Descarga y Compartir ---
    private fun downloadSelectedReports() {
        // Usa Storage Access Framework (SAF) para permitir al usuario elegir dónde guardar
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/csv"
            val fileName = "Reporte_CYM_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.csv"
            putExtra(Intent.EXTRA_TITLE, fileName)
        }
        startActivityForResult(intent, CREATE_FILE_REQUEST_CODE)
    }
    private fun shareSelectedReports() {
        lifecycleScope.launch {
            val csvContent = withContext(Dispatchers.IO) {
                // Generar el contenido del CSV en un hilo de IO
                generateReportCsvContent()
            }

            if (csvContent.isEmpty()) {
                Toast.makeText(this@ReportsActivity, "No hay datos seleccionados para compartir.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            // Crear un archivo temporal para compartir
            val tempFile = File(cacheDir, "reporte_temporal.csv")
            try {
                FileOutputStream(tempFile).use { fos ->
                    OutputStreamWriter(fos).use { writer ->
                        writer.write(csvContent)
                    }
                }

                // Usar FileProvider para obtener una URI segura
                val uri: Uri = FileProvider.getUriForFile(
                    this@ReportsActivity,
                    REPORT_FILE_AUTHORITY,
                    tempFile
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(shareIntent, "Compartir reporte CSV"))

            } catch (e: Exception) {
                Log.e("ReportsActivity", "Error al compartir archivo CSV: ${e.message}", e)
                Toast.makeText(this@ReportsActivity, "Error al preparar el archivo para compartir.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    // Manejar el resultado de ACTION_CREATE_DOCUMENT
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CREATE_FILE_REQUEST_CODE && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                lifecycleScope.launch {
                    val csvContent = withContext(Dispatchers.IO) {
                        generateReportCsvContent()
                    }
                    if (csvContent.isEmpty()) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@ReportsActivity, "No hay datos seleccionados para descargar.", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }
                    try {
                        contentResolver.openOutputStream(uri)?.use { outputStream ->
                            OutputStreamWriter(outputStream).use { writer ->
                                writer.write(csvContent)
                            }
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@ReportsActivity, "Reporte descargado correctamente.", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Log.e("ReportsActivity", "Error al guardar archivo CSV: ${e.message}", e)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@ReportsActivity, "Error al descargar el reporte: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }
    // Función para mostrar el selector de rango de fechas (sin cambios)
    private fun showDateRangePicker() {
        val builder = MaterialDatePicker.Builder.dateRangePicker()
        val picker = builder.build()

        picker.addOnPositiveButtonClickListener { selection ->
            val startDateMillis = selection.first ?: return@addOnPositiveButtonClickListener
            val endDateMillis = selection.second ?: return@addOnPositiveButtonClickListener

            val startDate = Date(startDateMillis)
            val endDate = Date(endDateMillis)

            binding.editTextDateRange.setText("${displayDateFormat.format(startDate)} - ${displayDateFormat.format(endDate)}")
            Toast.makeText(this, "Rango Seleccionado: ${displayDateFormat.format(startDate)} a ${displayDateFormat.format(endDate)}", Toast.LENGTH_LONG).show()
        }

        picker.addOnNegativeButtonClickListener {
            Toast.makeText(this, "Selección de rango cancelada", Toast.LENGTH_SHORT).show()
        }
        picker.addOnCancelListener {
            Toast.makeText(this, "Selección de rango cancelada (al cerrar)", Toast.LENGTH_SHORT).show()
        }

        picker.show(supportFragmentManager, picker.toString())
    }

}

// Adaptador para el RecyclerView de Reportes (sin cambios en esta clase)
class ReportAdapter(
    private val reportList: MutableList<ReportSummary>,
    private val onCheckboxChanged: (position: Int, isChecked: Boolean) -> Unit,
    private val onViewDetailsClick: (date: String) -> Unit
) : RecyclerView.Adapter<ReportAdapter.ReportViewHolder>() {

    class ReportViewHolder(private val binding: ItemReportSummaryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(summary: ReportSummary, onCheckboxChanged: (position: Int, isChecked: Boolean) -> Unit, onViewDetailsClick: (date: String) -> Unit) {
            binding.textViewReportDate.text = summary.date
            binding.textViewTotalLunches.text = summary.totalLunches.toString()
            binding.textViewTotalDinners.text = summary.totalDinners.toString()
            binding.checkboxSelectReport.isChecked = summary.isSelected

            binding.checkboxSelectReport.setOnCheckedChangeListener { _, isChecked ->
                onCheckboxChanged(adapterPosition, isChecked)
            }

            binding.textViewViewDetails.setOnClickListener {
                onViewDetailsClick(summary.dateDBFormat)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val binding = ItemReportSummaryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReportViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        holder.bind(reportList[position], onCheckboxChanged, onViewDetailsClick)
    }

    override fun getItemCount(): Int = reportList.size
}