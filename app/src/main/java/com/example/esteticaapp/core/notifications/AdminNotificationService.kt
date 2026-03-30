/*
 * AdminNotificationService mantiene un listener en Realtime Database
 * para detectar nuevas entradas en 'admin_notifications' y mostrar
 * notificaciones locales al administrador cuando no han sido leidas.
 * Corre en primer plano, escucha los ultimos nodos al iniciar,
 * filtra notificaciones viejas, marca como leido y libera listener al destruirse.
 */

package com.example.esteticaapp.core.notifications

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.content.pm.ServiceInfo
import android.util.Log
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase

class AdminNotificationService : Service() {

    private val TAG = "AdminNotifService"
    private val rtdb = FirebaseDatabase.getInstance("https://estetica-e0333-default-rtdb.firebaseio.com")
    private val notifRef = rtdb.getReference("admin_notifications")

    private val childListener = object : ChildEventListener {
        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            val read = snapshot.child("read").getValue(Boolean::class.java) ?: false
            val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L
            val title = snapshot.child("title").getValue(String::class.java) ?: "Nueva Actividad"
            
            Log.d(TAG, "🔔 Entrada detectada: ${snapshot.key} | Título: $title | Leída: $read")

            // Margen de 1 hora para evitar que suenen notificaciones viejas al arrancar
            val isRecent = (System.currentTimeMillis() - timestamp) < 3600000 

            if (!read && isRecent) {
                val message = snapshot.child("message").getValue(String::class.java) ?: ""
                
                Log.d(TAG, "🚀 !!! DISPARANDO NOTIFICACIÓN VISUAL !!!")
                NotificationHelper.showNotification(applicationContext, title, message)
                
                // Marcar como leído para que no vuelva a sonar
                snapshot.ref.child("read").setValue(true)
            }
        }

        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
            Log.d(TAG, "📝 Cambio en nodo: ${snapshot.key}")
        }
        
        override fun onChildRemoved(snapshot: DataSnapshot) {}
        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
        override fun onCancelled(error: DatabaseError) {
            Log.e(TAG, "❌ Error en Firebase: ${error.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "✅ Servicio iniciado en primer plano")
        
        NotificationHelper.createNotificationChannel(this)
        
        val notification = NotificationHelper.createForegroundNotification(this)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1001, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(1001, notification)
        }

        // Escuchar solo los últimos 5 para no saturar al inicio
        notifRef.limitToLast(5).addChildEventListener(childListener)

        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "🛑 Servicio destruido, removiendo listener")
        notifRef.removeEventListener(childListener)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
