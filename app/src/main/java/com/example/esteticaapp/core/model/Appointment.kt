/*
 * Appointment representa una cita agendada en la aplicación.
 * Guarda identificadores del usuario y de la cita, datos del cliente,
 * servicio, fecha/hora, estado y metadatos de creación.
 * También permite datos opcionales como reseña y código de invitación.
 */

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
    val review: Map<String, Any>? = null,
    val invitationCode: String? = null
)
