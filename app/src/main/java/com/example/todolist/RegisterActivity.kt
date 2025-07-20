package com.example.todolist

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class RegisterActivity : AppCompatActivity() {

    private lateinit var fullNameInput: EditText
    private lateinit var registerEmailInput: EditText
    private lateinit var registerPasswordInput: EditText
    private lateinit var registerButton: Button

    // URL base de tu API de Flask (asegúrate de que sea la correcta, local o de Render)
    private val API_BASE_URL = "https://todo-list-api-oekd.onrender.com" // 10.0.2.2 para emuladores de Android
    // Si usas tu celular real y está en la misma red Wi-Fi, usa la IP de tu computadora (ej. http://192.168.1.X:5000)

    private val JSON = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register) // Infla tu layout de registro

        // Inicializa las vistas
        fullNameInput = findViewById(R.id.full_name_input)
        registerEmailInput = findViewById(R.id.register_email_input)
        registerPasswordInput = findViewById(R.id.register_password_input)
        registerButton = findViewById(R.id.register_button)

        registerButton.setOnClickListener {
            val fullName = fullNameInput.text.toString()
            val email = registerEmailInput.text.toString()
            val password = registerPasswordInput.text.toString()

            if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Construye el objeto JSON para la solicitud
            val jsonObject = JSONObject().apply {
                put("nombre_usuario", fullName)
                put("correo_usuario", email)
                put("contrasena_usuario", password)
            }
            val requestBody = jsonObject.toString().toRequestBody(JSON)

            val request = Request.Builder()
                .url("$API_BASE_URL/auth/register") // Usa la ruta completa del endpoint de registro
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(this@RegisterActivity, "Error de red: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body?.string()
                    runOnUiThread {
                        if (response.isSuccessful) {
                            Toast.makeText(this@RegisterActivity, "Registro exitoso: $responseBody", Toast.LENGTH_LONG).show()
                            // Opcional: Navegar a la pantalla de login o directamente a la lista de tareas
                            finish() // Cierra esta actividad para volver a la anterior (login)
                        } else {
                            val errorMsg = try {
                                JSONObject(responseBody).getString("message")
                            } catch (e: Exception) {
                                "Error en el registro"
                            }
                            Toast.makeText(this@RegisterActivity, "Error: $errorMsg (${response.code})", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            })
        }
    }
}