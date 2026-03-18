package com.example.esteticaapp

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

/**
 * Servicio en primer plano para clientas.
 * Escucha confirmaciones de citas y actualizaciones de estado personalizadas.
 */
class ClientNotificationService : Service() {

    private val TAG = "ClientNotifService"
    private val auth = FirebaseAuth.getInstance()
    private val rtdb = FirebaseDatabase.getInstance("https://estetica-e0333-default-rtdb.firebaseio.com")
    private var notifRef: DatabaseReference? = null

    private val childListener = object : ChildEventListener {
        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            val read = snapshot.child("read").getValue(Boolean::class.java) ?: false
            val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L
            val title = snapshot.child("title").getValue(String::class.java) ?: "Actualización de Cita"
            
            Log.d(TAG, "🔔 Notificación para cliente: ${snapshot.key} | Título: $title")

            // Margen de 1 hora
            val isRecent = (System.currentTimeMillis() - timestamp) < 3600000 

            if (!read && isRecent) {
                val message = snapshot.child("message").getValue(String::class.java) ?: ""
                
                NotificationHelper.showNotification(applicationContext, title, message)
                
                // Marcar como leído
                snapshot.ref.child("read").setValue(true)
            }
        }

        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
        override fun onChildRemoved(snapshot: DataSnapshot) {}
        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
        override fun onCancelled(error: DatabaseError) {
            Log.e(TAG, "❌ Error en Firebase Client: ${error.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val userId = auth.currentUser?.uid
        
        if (userId == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        Log.d(TAG, "✅ Servicio de cliente iniciado para UID: $userId")
        
        // Usamos el mismo NotificationHelper para el canal y la notificación de persistencia
        NotificationHelper.createNotificationChannel(this)
        val notification = NotificationHelper.createForegroundNotification(this)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(2002, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(2002, notification)
        }

        notifRef = rtdb.getReference("client_notifications").child(userId)
        notifRef?.limitToLast(5)?.addChildEventListener(childListener)

        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "🛑 Servicio de cliente destruido")
        notifRef?.removeEventListener(childListener)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
