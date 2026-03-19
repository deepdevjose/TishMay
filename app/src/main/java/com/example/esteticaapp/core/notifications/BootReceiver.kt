package com.example.esteticaapp.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device booted, attempting to restart services...")
            
            val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val isAdmin = prefs.getBoolean("is_admin", false)
            
            // Si estaba marcado como admin, reiniciamos el servicio de admin
            // Si NO, asumimos que es cliente (si hay sesión, pero aquí simplificamos)
            // Nota: Para clientes, deberíamos guardar un "is_client" también, pero
            // por ahora priorizamos el caso crítico que es el ADMIN recibiendo citas.
            
            if (isAdmin) {
                startServiceWrapper(context, AdminNotificationService::class.java)
            } else {
                // Opcional: Reiniciar servicio de cliente si es necesario
                // startServiceWrapper(context, ClientNotificationService::class.java)
            }
        }
    }

    private fun startServiceWrapper(context: Context, serviceClass: Class<*>) {
        val serviceIntent = Intent(context, serviceClass)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            Log.d("BootReceiver", "Started service: ${serviceClass.simpleName}")
        } catch (e: Exception) {
            Log.e("BootReceiver", "Failed to start service: ${e.message}")
        }
    }
}
