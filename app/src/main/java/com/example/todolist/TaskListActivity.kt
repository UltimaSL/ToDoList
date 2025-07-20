package com.example.todolist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class TaskListActivity : AppCompatActivity() {

    private lateinit var addTaskFab: FloatingActionButton
    private lateinit var tasksRecyclerView: RecyclerView
    private lateinit var noTasksText: TextView
    private lateinit var taskAdapter: TaskAdapter
    private var currentUserId: String? = null

    private val client = OkHttpClient()
    private val JSON = "application/json; charset=utf-8".toMediaType() // Necesario para solicitudes POST/PUT
    private val API_BASE_URL = "https://todo-list-api-oekd.onrender.com/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_list)

        val toolbar: Toolbar = findViewById(R.id.main_toolbar)
        setSupportActionBar(toolbar)

        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        currentUserId = sharedPref.getString("logged_in_user_id", null)

        if (currentUserId == null) {
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

        // <-- CAMBIADO: onTaskClick ahora recibe la posición
        tasksRecyclerView.layoutManager = LinearLayoutManager(this)
        taskAdapter = TaskAdapter(mutableListOf(), { task, position ->
            // Lógica para clic en una tarea: Toggling del estado 'completada'
            toggleTaskStatus(task, position)
        }, { task ->
            // Clic largo: sigue siendo para borrar (se mantiene igual)
            Toast.makeText(this, "Clic largo para borrar: ${task.notes}", Toast.LENGTH_SHORT).show()
            deleteTaskFromApi(task)
        })

        taskAdapter.setOnDeleteButtonClick { task ->
            Toast.makeText(this, "Borrar tarea: ${task.notes}", Toast.LENGTH_SHORT).show()
            deleteTaskFromApi(task)
        }

        tasksRecyclerView.adapter = taskAdapter

        addTaskFab.setOnClickListener {
            currentUserId?.let { userId ->
                val bottomSheet = AddTaskBottomSheet.newInstance(userId)
                bottomSheet.show(supportFragmentManager, bottomSheet.tag)
            } ?: run {
                Toast.makeText(this, "Error: No se encontró el ID de usuario logueado.", Toast.LENGTH_LONG).show()
            }
        }

        fetchTasks()
    }

    override fun onResume() {
        super.onResume()
        fetchTasks()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                fetchTasks()
                true
            }
            R.id.action_logout -> {
                logoutUser()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun fetchTasks() {
        currentUserId?.let { userId ->
            val request = Request.Builder()
                .url("$API_BASE_URL/api/tasks/user/$userId")
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

    private fun toggleTaskStatus(task: Task, position: Int) {
        val newStatus = !task.isDone
        val updatedTask = task.copy(isDone = newStatus) // Crea una copia con el nuevo estado

        val jsonObject = JSONObject().apply {
            put("id_usuario", updatedTask.userId)
            put("id_tarea", updatedTask.id) // ID de la tarea a actualizar
            put("notas_usuario", updatedTask.notes)
            put("etiqueta", updatedTask.tag)
            put("is_done", updatedTask.isDone) // Enviar el nuevo estado
        }
        val requestBody = jsonObject.toString().toRequestBody(JSON)

        val request = Request.Builder()
            .url("$API_BASE_URL/api/tasks") // Reutilizamos el endpoint POST de guardar/actualizar
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@TaskListActivity, "Error de red al actualizar tarea: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                runOnUiThread {
                    if (response.isSuccessful) {
                        // Si la actualización en la API fue exitosa, actualizamos solo el ítem en la UI
                        taskAdapter.updateTaskState(position, newStatus) // <-- CAMBIADO: Actualizar solo este ítem
                        Toast.makeText(this@TaskListActivity, "Tarea ${if (newStatus) "completada" else "pendiente"}: ${task.notes}", Toast.LENGTH_SHORT).show()
                    } else {
                        val errorMsg = try {
                            JSONObject(responseBody).getString("message")
                        } catch (e: Exception) {
                            "Error al actualizar tarea"
                        }
                        Toast.makeText(this@TaskListActivity, "Error: $errorMsg (${response.code})", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    private fun deleteTaskFromApi(task: Task) {
        val request = Request.Builder()
            .url("$API_BASE_URL/api/tasks/${task.id}")
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
                        taskAdapter.removeTask(task)
                        if (taskAdapter.itemCount == 0) {
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

    private fun logoutUser() {
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            remove("logged_in_user_id")
            apply()
        }
        Toast.makeText(this, "Sesión cerrada.", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}