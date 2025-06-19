package com.example.mycymapp.viewmodels // O donde tengas tus viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mycymapp.data.AppDatabase
import com.example.mycymapp.data.TrabajadorDao
import com.example.mycymapp.data.Trabajador // ¡IMPORTANTE! Usaremos directamente la entidad Trabajador aquí
// import com.example.mycymapp.data.WorkerItem // Si vas a usar WorkerItem, importa desde 'data'
// import com.example.mycymapp.models.WorkerItem // <-- ELIMINAR esta línea (paquete 'models' no existe para WorkerItem)

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.collect // Para collect
import kotlinx.coroutines.launch

// Si quieres usar WorkerItem como una data class separada (a pesar de mi recomendación de usar Trabajador directamente),
// asegúrate de que su definición refleje que el DNI es la PK y no tiene 'id'.
// Si la usas, asegúrate de que esté en com.example.mycymapp.data
/*
data class WorkerItem(
    val dni: String, // DNI es el identificador único
    val nombres: String,
    val apellidos: String,
    val area: String,
    val puestoLabor: String,
    val tipoTrabajador: String
)
*/


// Usaremos directamente la entidad Trabajador en el ViewModel para simplificar.
// Si realmente necesitas una WorkerItem diferente, su definición debe reflejar los campos de Trabajador
// sin el 'id'. Pero para esta funcionalidad, Trabajador es suficiente.
class WorkerListViewModel(private val trabajadorDao: TrabajadorDao) : ViewModel() {

    // El StateFlow ahora contendrá una lista de objetos Trabajador
    private val _workerList = MutableStateFlow<List<Trabajador>>(emptyList())
    val workerList: StateFlow<List<Trabajador>> = _workerList.asStateFlow()

    init {
        loadWorkers()
    }

    private fun loadWorkers() {
        viewModelScope.launch {
            // trabjadorDao.getAllTrabajadores() ya devuelve un Flow<List<Trabajador>>
            // Ya no necesitamos .map si _workerList es de tipo List<Trabajador>
            trabajadorDao.getAllTrabajadores().collect { trabajadores -> // Asumo que este es el método correcto
                _workerList.value = trabajadores // Asignamos la lista de Trabajador directamente
            }
        }
    }

    // Si quieres añadir una función de búsqueda/filtro, podrías hacer algo así:
    // private val _searchText = MutableStateFlow("")
    // val searchText: StateFlow<String> = _searchText.asStateFlow()
    //
    // fun setSearchText(text: String) {
    //     _searchText.value = text
    // }
    //
    // Entonces tu loadWorkers() o una nueva función debería combinar el flow de la DB con el _searchText

    // Factory para instanciar el ViewModel con el DAO
    class Factory(private val database: AppDatabase) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(WorkerListViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return WorkerListViewModel(database.trabajadorDao()) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}