package com.example.esteticaapp

object AdminConfig {
    val adminEmails = setOf(
        "admin@tishmay.com",
        "deepdevjose@itsoeh.edu.mx"
    )

    fun isAdmin(email: String?): Boolean {
        return email != null && adminEmails.contains(email.lowercase())
    }
}
