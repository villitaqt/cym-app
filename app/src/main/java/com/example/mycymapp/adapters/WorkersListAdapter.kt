package com.example.mycymapp.adapters // ¡MUY IMPORTANTE que este paquete sea correcto!

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mycymapp.databinding.ItemWorkerBinding // Tu binding para item_worker.xml
import com.example.mycymapp.data.Trabajador // Tu entidad Trabajador

class WorkersListAdapter( // <-- La clase debe llamarse WorkersListAdapter como en WorkersListActivity
    private val workersList: MutableList<Trabajador>, // Lista de tu entidad Trabajador
    // El lambda onItemClick ahora recibe un objeto Trabajador completo
    private val onItemClick: (Trabajador) -> Unit
) : RecyclerView.Adapter<WorkersListAdapter.WorkerListViewHolder>() {

    class WorkerListViewHolder(private val binding: ItemWorkerBinding) : // Binding para el item_worker.xml
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Trabajador, onItemClick: (Trabajador) -> Unit) {
            // Asignar los datos a los TextViews del layout
            binding.tvWorkerName.text = "${item.nombres} ${item.apellidos}"
            binding.tvWorkerDni.text = "DNI: ${item.dni}"
            binding.tvWorkerType.text = "Tipo: ${item.tipoTrabajador}"
            binding.tvWorkerAreaPuesto.text = "Área: ${item.area} - Puesto: ${item.puestoLabor}"

            // Configurar el click listener para el ítem completo (card)
            itemView.setOnClickListener {
                onItemClick(item)
            }
            // Puedes configurar un click listener para el icono de editar si quieres que haga algo diferente
            binding.ivEditWorker.setOnClickListener {
                // Si ivEditWorker hace lo mismo que el card, puedes llamar a onItemClick(item)
                // O puedes crear otro lambda en el constructor del adapter para esto
                onItemClick(item) // Por ejemplo, click en editar activa la misma acción
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkerListViewHolder {
        val binding = ItemWorkerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WorkerListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WorkerListViewHolder, position: Int) {
        holder.bind(workersList[position], onItemClick)
    }

    override fun getItemCount(): Int = workersList.size

    // Método para actualizar la lista de trabajadores
    fun updateList(newList: List<Trabajador>) {
        workersList.clear()
        workersList.addAll(newList)
        notifyDataSetChanged() // Puedes optimizar esto con DiffUtil para animaciones
    }
}