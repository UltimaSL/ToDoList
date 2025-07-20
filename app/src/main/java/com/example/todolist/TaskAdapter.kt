package com.example.todolist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.w3c.dom.Text

// Adaptador para el RecyclerView de la lista de tareas
class TaskAdapter(
    private var tasks: MutableList<Task>, // Usamos MutableList para permitir cambios
    private val onTaskClick: (Task) -> Unit, // Listener para cuando se hace clic en una tarea
    private val onTaskLongClick: (Task) -> Unit // Listener para cuando se hace un clic largo (ej. para borrar)
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    // Clase interna para mantener las vistas de cada ítem de la lista
    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkIcon: ImageView = itemView.findViewById(R.id.task_check_icon)
        val notesText: TextView = itemView.findViewById(R.id.task_notes_text)
        val tagText: TextView = itemView.findViewById(R.id.task_tag_text)
    }

    // Se llama cuando RecyclerView necesita un nuevo ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.task_item, parent, false)
        return TaskViewHolder(view)
    }

    // Se llama para mostrar los datos en una posición específica
    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]

        holder.notesText.text = task.notes

        // Mostrar u ocultar la etiqueta
        if (task.tag.isNotEmpty()) {
            holder.tagText.text = "Etiqueta: ${task.tag}"
            holder.tagText.visibility = View.VISIBLE
        } else {
            holder.tagText.visibility = View.GONE
        }

        // Configurar el ícono de verificación basado en la lógica (si tu tarea tiene un estado 'done')
        // Por ahora, solo muestra el ícono de desmarcado por defecto o de marcado si la etiqueta es 'Finalizado'
        // Esto es una simplificación; en una app real, la Task tendría una propiedad 'isDone'
        if (task.tag.equals("Finalizado", ignoreCase = true)) { // Ejemplo simple de estado "hecho"
            holder.checkIcon.setImageResource(R.drawable.ic_check_box_checked)
        } else {
            holder.checkIcon.setImageResource(R.drawable.ic_check_box_outline_blank)
        }

        // Configurar el click listener para el ítem completo
        holder.itemView.setOnClickListener {
            onTaskClick(task)
        }

        // Configurar el click largo listener para el ítem completo
        holder.itemView.setOnLongClickListener {
            onTaskLongClick(task)
            true // Indica que el evento fue consumido
        }
    }

    // Devuelve el número total de ítems en la lista
    override fun getItemCount(): Int {
        return tasks.size
    }

    // Método para actualizar la lista de tareas en el adaptador
    fun updateTasks(newTasks: List<Task>) {
        tasks.clear()
        tasks.addAll(newTasks)
        notifyDataSetChanged() // Notifica al RecyclerView que los datos han cambiado
    }

    // Método para eliminar una tarea de la lista (útil después de un borrado exitoso)
    fun removeTask(task: Task) {
        val position = tasks.indexOf(task)
        if (position != -1) {
            tasks.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}