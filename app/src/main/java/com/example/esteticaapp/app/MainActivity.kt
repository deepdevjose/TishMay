/*
 * MainActivity es el punto de entrada de la app.
 * Inicializa notificaciones, solicita permiso de notificaciones (Android 13+),
 * observa el estado de red y decide la pantalla inicial segun sesion/rol (admin o cliente).
 * También inicia o detiene el servicio de notificaciones para administradores
 * y renderiza la navegación principal con un overlay global cuando no hay conexion.
 */
package com.example.esteticaapp.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.esteticaapp.ui.theme.EsteticaAppTheme
import com.example.esteticaapp.ui.components.NoConnectionOverlay
import com.example.esteticaapp.core.notifications.NotificationHelper
import com.example.esteticaapp.core.notifications.AdminNotificationService
import com.example.esteticaapp.core.network.NetworkUtils
import com.example.esteticaapp.core.config.AdminConfig
import com.example.esteticaapp.navigation.AppNavGraph
import com.example.esteticaapp.navigation.Routes
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        NotificationHelper.createNotificationChannel(this)
        
        val auth = FirebaseAuth.getInstance()

        enableEdgeToEdge()
        setContent {
            EsteticaAppTheme {
                val context = LocalContext.current
                
                // Permiso de notificaciones para Android 13+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val permissionState = rememberPermissionState(
                        permission = android.Manifest.permission.POST_NOTIFICATIONS
                    )
                    LaunchedEffect(Unit) {
                        if (!permissionState.status.isGranted) {
                            permissionState.launchPermissionRequest()
                        }
                    }
                }

                var isOnline by remember { mutableStateOf(true) }
                
                // Observar conectividad globalmente
                LaunchedEffect(Unit) {
                    NetworkUtils.observeConnectivity(context).collect { online ->
                        isOnline = online
                    }
                }

                // Estado inicial de carga ("splash") mientras verificamos el rol del usuario
                var currentScreen by remember { mutableStateOf(Routes.Splash) }
                val scope = rememberCoroutineScope()
                
                // Estado para pasar el email a la pantalla de ForgotPassword y Login
                var initialEmailState by remember { mutableStateOf("") }

                // Helper para controlar el servicio de notificaciones
                val manageAdminService = { isAdmin: Boolean ->
                    val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                    prefs.edit().putBoolean("is_admin", isAdmin).apply()

                    val serviceIntent = Intent(context, AdminNotificationService::class.java)
                    if (isAdmin) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }
                    } else {
                        context.stopService(serviceIntent)
                    }
                }

                // Verificación de sesión y rol al inicio
                LaunchedEffect(Unit) {
                    val user = auth.currentUser
                    val navigateToExtra = intent.getStringExtra("navigate_to")
                    
                    if (user != null) {
                        if (user.isEmailVerified) {
                            if (AdminConfig.isAdmin(user.email)) {
                                manageAdminService(true)
                                // Si venimos de una notificación, vamos directo al dashboard
                                if (navigateToExtra == Routes.AdminDashboard) {
                                    currentScreen = Routes.AdminDashboard
                                } else {
                                    currentScreen = Routes.AdminWelcome
                                }
                            } else {
                                manageAdminService(false)
                                currentScreen = Routes.Main
                            }
                        } else {
                            if (AdminConfig.isAdmin(user.email)) {
                                manageAdminService(true)
                                currentScreen = Routes.AdminWelcome
                            } else {
                                manageAdminService(false)
                                currentScreen = Routes.EmailVerification
                            }
                        }
                    } else {
                        manageAdminService(false)
                        currentScreen = Routes.Login
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    AppNavGraph(
                        currentScreen = currentScreen,
                        initialEmail = initialEmailState,
                        onInitialEmailChange = { initialEmailState = it },
                        onScreenChange = { currentScreen = it },
                        onLoginSuccess = { email ->
                            scope.launch {
                                if (AdminConfig.isAdmin(email)) {
                                    manageAdminService(true)
                                    currentScreen = Routes.AdminWelcome
                                } else {
                                    currentScreen = Routes.Main
                                }
                            }
                        },
                        onLogout = {
                            auth.signOut()
                            manageAdminService(false)
                            currentScreen = Routes.Login
                        }
                    )

                    // Overlay global de desconexión
                    NoConnectionOverlay(isVisible = !isOnline)
                }
            }
        }
    }
}
