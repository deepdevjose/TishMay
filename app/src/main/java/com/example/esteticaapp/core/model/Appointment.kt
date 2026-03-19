package com.example.esteticaapp.core.model

data class Appointment(
    val id: String = "",
    val userId: String = "",
    val clientName: String = "",
    val service: String = "",
    val date: String = "",
    val time: String = "",
    val status: String = "Pendiente",
    val timestamp: Long = System.currentTimeMillis(),
    val review: Map<String, Any>? = null
)
