package com.example.todolist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AddTaskBottomSheet : BottomSheetDialogFragment() {

    private lateinit var taskDescriptionInput: EditText
    private lateinit var saveTaskButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.add_task_bottom_sheet, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Forzar a la ventana a expandirse al abrir
        val behavior = BottomSheetBehavior.from(view.parent as View)
        behavior.state = BottomSheetBehavior.STATE_EXPANDED

        taskDescriptionInput = view.findViewById(R.id.task_description_input)
        saveTaskButton = view.findViewById(R.id.save_task_button)

        saveTaskButton.setOnClickListener {
            val description = taskDescriptionInput.text.toString()

            if (description.isNotEmpty()) {
                // TO-DO: Aquí se agregará la lógica para guardar la nueva tarea
                println("Nueva tarea: $description")

                // Cierra la ventana emergente
                dismiss()
            }
        }
    }
}