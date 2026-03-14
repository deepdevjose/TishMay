package com.example.esteticaapp

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object AdminConfig {
    /**
     * Verifica si un usuario es administrador consultando la colección 'administradores' en Firestore.
     * La colección debe contener documentos donde el ID sea el correo electrónico del administrador.
     */
    suspend fun isAdmin(email: String?): Boolean {
        if (email.isNullOrBlank()) return false
        
        val emailKey = email.lowercase().trim()
        Log.d("AdminConfig", "Verificando si es admin: $emailKey")

        return try {
            val db = FirebaseFirestore.getInstance()
            val doc = db.collection("administradores")
                .document(emailKey)
                .get()
                .await()
            
            val exists = doc.exists()
            Log.d("AdminConfig", "Resultado para $emailKey: Existe=$exists")
            exists
        } catch (e: Exception) {
            Log.e("AdminConfig", "Error al verificar admin para $emailKey", e)
            e.printStackTrace()
            false
        }
    }
}
