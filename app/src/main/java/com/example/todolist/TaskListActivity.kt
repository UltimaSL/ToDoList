package com.example.todolist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class TaskListActivity : AppCompatActivity() {

    private lateinit var addTaskFab: FloatingActionButton
    private lateinit var tasksRecyclerView: RecyclerView
    private lateinit var noTasksText: TextView // Para el mensaje "No hay tareas"
    private lateinit var taskAdapter: TaskAdapter
    private var currentUserId: String? = null

    // Cliente OkHttp
    private val client = OkHttpClient()
    // URL base de tu API de Flask (asegúrate de que sea la correcta, local o de Render)
    private val API_BASE_URL = "https://todo-list-api-oekd.onrender.com" // 10.0.2.2 para emuladores de Android

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_list)

        // Obtener el ID del usuario actual de SharedPreferences
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        currentUserId = sharedPref.getString("logged_in_user_id", null)

        if (currentUserId == null) {
            // Si no hay ID de usuario logueado, redirigir a la pantalla de login
            Toast.makeText(this, "Sesión expirada. Por favor, inicia sesión de nuevo.", Toast.LENGTH_LONG).show()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        addTaskFab = findViewById(R.id.add_task_fab)
        tasksRecyclerView = findViewById(R.id.tasks_list_recycler_view)
        noTasksText = findViewById(R.id.no_tasks_text)

        // Configurar RecyclerView
        tasksRecyclerView.layoutManager = LinearLayoutManager(this)
        taskAdapter = TaskAdapter(mutableListOf(), { task ->
            // Lógica para clic en una tarea (ej. marcar como completada)
            Toast.makeText(this, "Clic en tarea: ${task.notes}", Toast.LENGTH_SHORT).show()
            // Aquí puedes llamar a un endpoint para actualizar el estado de la tarea
            // Por ejemplo, task.isCompleted = !task.isCompleted y luego llamar a la API
        }, { task ->
            // Lógica para clic largo en una tarea (ej. para borrar)
            Toast.makeText(this, "Clic largo para borrar: ${task.notes}", Toast.LENGTH_SHORT).show()
            // Aquí llamarías a la API para borrar la tarea
            deleteTaskFromApi(task)
        })
        tasksRecyclerView.adapter = taskAdapter

        addTaskFab.setOnClickListener {
            currentUserId?.let { userId ->
                val bottomSheet = AddTaskBottomSheet.newInstance(userId)
                bottomSheet.show(supportFragmentManager, bottomSheet.tag)
            } ?: run {
                Toast.makeText(this, "Error: No se encontró el ID de usuario logueado.", Toast.LENGTH_LONG).show()
            }
        }

        // Cargar las tareas al iniciar la actividad
        fetchTasks()
    }

    override fun onResume() {
        super.onResume()
        // Recargar las tareas cada vez que la actividad vuelve a estar en primer plano
        // Esto asegura que se muestren las tareas recién añadidas o modificadas
        fetchTasks()
    }

    private fun fetchTasks() {
        currentUserId?.let { userId ->
            val request = Request.Builder()
                .url("$API_BASE_URL/api/tasks/user/$userId") // Endpoint para obtener tareas por user_id
                .get()
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(this@TaskListActivity, "Error de red al cargar tareas: ${e.message}", Toast.LENGTH_LONG).show()
                        tasksRecyclerView.visibility = View.GONE
                        noTasksText.visibility = View.VISIBLE
                        noTasksText.text = "Error al cargar tareas: ${e.message}"
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body?.string()
                    runOnUiThread {
                        if (response.isSuccessful) {
                            try {
                                val jsonArray = JSONArray(responseBody)
                                val fetchedTasks = mutableListOf<Task>()
                                for (i in 0 until jsonArray.length()) {
                                    val taskJson = jsonArray.getJSONObject(i)
                                    fetchedTasks.add(Task.fromJson(taskJson))
                                }

                                if (fetchedTasks.isNotEmpty()) {
                                    taskAdapter.updateTasks(fetchedTasks)
                                    tasksRecyclerView.visibility = View.VISIBLE
                                    noTasksText.visibility = View.GONE
                                } else {
                                    tasksRecyclerView.visibility = View.GONE
                                    noTasksText.visibility = View.VISIBLE
                                    noTasksText.text = "No hay tareas. ¡Agrega una!"
                                }
                            } catch (e: Exception) {
                                Toast.makeText(this@TaskListActivity, "Error al parsear tareas: ${e.message}", Toast.LENGTH_LONG).show()
                                tasksRecyclerView.visibility = View.GONE
                                noTasksText.visibility = View.VISIBLE
                                noTasksText.text = "Error al parsear tareas."
                            }
                        } else {
                            val errorMsg = try {
                                JSONObject(responseBody).getString("message")
                            } catch (e: Exception) {
                                "Error al cargar tareas"
                            }
                            Toast.makeText(this@TaskListActivity, "Error: $errorMsg (${response.code})", Toast.LENGTH_LONG).show()
                            tasksRecyclerView.visibility = View.GONE
                            noTasksText.visibility = View.VISIBLE
                            noTasksText.text = "Error al cargar: $errorMsg"
                        }
                    }
                }
            })
        } ?: run {
            Toast.makeText(this, "ID de usuario no disponible para cargar tareas.", Toast.LENGTH_LONG).show()
        }
    }

    private fun deleteTaskFromApi(task: Task) {
        val request = Request.Builder()
            .url("$API_BASE_URL/api/tasks/${task.id}") // Endpoint para borrar tarea
            .delete()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@TaskListActivity, "Error de red al borrar: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@TaskListActivity, "Tarea eliminada: ${task.notes}", Toast.LENGTH_SHORT).show()
                        taskAdapter.removeTask(task) // Actualiza la UI
                        if (taskAdapter.itemCount == 0) { // Si ya no quedan tareas
                            tasksRecyclerView.visibility = View.GONE
                            noTasksText.visibility = View.VISIBLE
                            noTasksText.text = "No hay tareas. ¡Agrega una!"
                        }
                    } else {
                        val errorMsg = try {
                            JSONObject(responseBody).getString("message")
                        } catch (e: Exception) {
                            "Error al eliminar tarea"
                        }
                        Toast.makeText(this@TaskListActivity, "Error: $errorMsg (${response.code})", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }
}