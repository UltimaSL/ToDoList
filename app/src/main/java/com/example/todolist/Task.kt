package com.example.todolist

import org.json.JSONObject

// Clase de datos para representar una tarea
// Los nombres de las propiedades coinciden con las claves JSON de tu API
data class Task(
    val id: String,        // Mapea a '_id' de MongoDB
    val userId: String,    // Mapea a 'id_usuario' de MongoDB
    val notes: String,     // Mapea a 'notas_usuario'
    val tag: String        // Mapea a 'etiqueta'
) {
    // Companion object para crear un objeto Task desde un JSONObject de la API
    companion object {
        fun fromJson(json: JSONObject): Task {
            return Task(
                id = json.getString("_id"),
                userId = json.getString("id_usuario"),
                notes = json.getString("notas_usuario"),
                tag = json.optString("etiqueta", "") // Usar optString para campos opcionales
            )
        }
    }
}