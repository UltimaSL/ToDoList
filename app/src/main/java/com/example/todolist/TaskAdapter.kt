package com.example.todolist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TaskAdapter(
    private var tasks: MutableList<Task>,
    private val onTaskClick: (Task, Int) -> Unit, // <-- CAMBIADO: Añadido 'Int' para la posición
    private val onTaskLongClick: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    private var onDeleteButtonClick: ((Task) -> Unit)? = null

    fun setOnDeleteButtonClick(listener: (Task) -> Unit) {
        onDeleteButtonClick = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.task_item, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.bind(task)
        // Configurar el click listener para el ítem completo
        holder.itemView.setOnClickListener {
            onTaskClick(task, position) // <-- CAMBIADO: Pasar la posición
        }
        // Configurar el click largo listener
        holder.itemView.setOnLongClickListener {
            onTaskLongClick(task)
            true
        }
    }

    override fun getItemCount(): Int = tasks.size

    fun updateTasks(newTasks: List<Task>) {
        tasks.clear()
        tasks.addAll(newTasks)
        notifyDataSetChanged()
    }

    fun removeTask(task: Task) {
        val index = tasks.indexOfFirst { it.id == task.id }
        if (index != -1) {
            tasks.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    // Método para actualizar el estado de una tarea individual (sin recargar toda la lista)
    fun updateTaskState(position: Int, isDone: Boolean) {
        if (position >= 0 && position < tasks.size) {
            tasks[position].isDone = isDone
            notifyItemChanged(position) // Solo actualiza este ítem
        }
    }

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkIcon: ImageView = itemView.findViewById(R.id.task_check_icon)
        private val notesText: TextView = itemView.findViewById(R.id.task_notes_text)
        private val tagText: TextView = itemView.findViewById(R.id.task_tag_text)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.delete_task_button)

        fun bind(task: Task) {
            notesText.text = task.notes

            if (task.tag.isNotEmpty()) {
                tagText.text = "Etiqueta: ${task.tag}"
                tagText.visibility = View.VISIBLE
            } else {
                tagText.visibility = View.GONE
            }

            // <-- CAMBIADO: Usar task.isDone para el icono
            if (task.isDone) {
                checkIcon.setImageResource(R.drawable.ic_check_box_checked)
            } else {
                checkIcon.setImageResource(R.drawable.ic_check_box_outline_blank)
            }

            // Listener para el botón de borrar
            deleteButton.setOnClickListener { onDeleteButtonClick?.invoke(task) }
        }
    }
}