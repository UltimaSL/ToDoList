package com.example.todolist

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton

class TaskListActivity : AppCompatActivity() {

    private lateinit var addTaskFab: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_list)

        // Inicializa el FloatingActionButton
        addTaskFab = findViewById(R.id.add_task_fab)

        // Establece el listener de clic para abrir la ventana emergente
        addTaskFab.setOnClickListener {
            val bottomSheet = AddTaskBottomSheet()
            bottomSheet.show(supportFragmentManager, bottomSheet.tag)
        }
    }
}