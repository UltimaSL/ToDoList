package com.example.todolist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class AddTaskBottomSheet : BottomSheetDialogFragment() {

    private lateinit var taskDescriptionInput: EditText
    private lateinit var tagAutoCompleteTextView: AutoCompleteTextView
    private lateinit var saveTaskButton: Button

    // Cliente OkHttp
    private val client = OkHttpClient()
    private val JSON = "application/json; charset=utf-8".toMediaType()

    // URL base de tu API de Flask (asegúrate de que sea la correcta, local o de Render)
    private val API_BASE_URL = "https://todo-list-api-oekd.onrender.com/" // Para emuladores de Android
    // Si usas tu celular real y está en la misma red Wi-Fi, usa la IP de tu computadora (ej. http://192.168.1.X:5000)

    // Clave para el argumento del ID de usuario
    companion object {
        const val ARG_USER_ID = "user_id"

        fun newInstance(userId: String): AddTaskBottomSheet {
            val fragment = AddTaskBottomSheet()
            val args = Bundle()
            args.putString(ARG_USER_ID, userId)
            fragment.arguments = args
            return fragment
        }
    }

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

        // No forzamos el estado para que se ajuste a wrap_content
        // val behavior = BottomSheetBehavior.from(view.parent as View)
        // behavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED

        taskDescriptionInput = view.findViewById(R.id.task_description_input)
        tagAutoCompleteTextView = view.findViewById(R.id.tag_autocomplete_text_view)
        saveTaskButton = view.findViewById(R.id.save_task_button)

        // Configurar el adaptador para el menú desplegable de etiquetas
        val tagOptions = resources.getStringArray(R.array.tag_options)
        val adapter = ArrayAdapter(requireContext(), R.layout.dropdown_menu_item, tagOptions)
        tagAutoCompleteTextView.setAdapter(adapter)

        // Obtener el ID de usuario de los argumentos
        val userId = arguments?.getString(ARG_USER_ID)

        saveTaskButton.setOnClickListener {
            val description = taskDescriptionInput.text.toString()
            val selectedTag = tagAutoCompleteTextView.text.toString()

            if (description.isEmpty()) {
                Toast.makeText(requireContext(), "Por favor, escribe la descripción de la tarea", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (userId == null) {
                Toast.makeText(requireContext(), "Error: ID de usuario no disponible", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Construir el JSON para enviar a la API
            val jsonObject = JSONObject().apply {
                put("id_usuario", userId) // Usar el ID de usuario obtenido
                put("notas_usuario", description)
                put("etiqueta", selectedTag)
            }
            val requestBody = jsonObject.toString().toRequestBody(JSON)

            val request = Request.Builder()
                .url("$API_BASE_URL/api/tasks") // Endpoint para guardar tareas
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(requireContext(), "Error de red al guardar tarea: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body?.string()
                    runOnUiThread {
                        if (response.isSuccessful) {
                            Toast.makeText(requireContext(), "Tarea guardada exitosamente", Toast.LENGTH_SHORT).show()
                            dismiss() // Cierra la ventana emergente al guardar
                        } else {
                            val errorMsg = try {
                                JSONObject(responseBody).getString("message")
                            } catch (e: Exception) {
                                "Error al guardar tarea"
                            }
                            Toast.makeText(requireContext(), "Error: $errorMsg (${response.code})", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            })
        }
    }

    // Helper para ejecutar en el hilo principal
    private fun runOnUiThread(action: () -> Unit) {
        if (activity != null) {
            activity?.runOnUiThread(action)
        }
    }
}