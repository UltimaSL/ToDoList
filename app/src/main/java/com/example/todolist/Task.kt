package com.example.todolist

import org.json.JSONObject

data class Task(
    val id: String,
    val userId: String,
    val notes: String,
    val tag: String,
    var isDone: Boolean
) {
    companion object {
        fun fromJson(json: JSONObject): Task {
            return Task(
                id = json.getString("_id"),
                userId = json.getString("id_usuario"),
                notes = json.getString("notas_usuario"),
                tag = json.optString("etiqueta", ""),
                isDone = json.optBoolean("is_done", false)
            )
        }
    }
}