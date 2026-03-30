/*
 * AdminConfig centraliza la validacion de administradores.
 * Consulta Firestore (coleccion 'administradores') usando el email como ID de documento
 * para determinar si el usuario tiene privilegios de administrador.
 * Tambien registra logs de diagnostico y devuelve false si ocurre un error.
 */

package com.example.esteticaapp.core.config

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
        
        Log.d("AdminConfig", "Verificando si es admin en DB: $emailKey")

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
