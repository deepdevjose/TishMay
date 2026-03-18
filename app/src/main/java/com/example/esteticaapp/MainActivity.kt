package com.example.esteticaapp

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.esteticaapp.ui.theme.EsteticaAppTheme
import com.example.esteticaapp.ui.screens.*
import com.example.esteticaapp.ui.components.NoConnectionOverlay
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
                var currentScreen by remember { mutableStateOf("splash") }
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
                                if (navigateToExtra == "admin_dashboard") {
                                    currentScreen = "admin_dashboard"
                                } else {
                                    currentScreen = "admin_welcome"
                                }
                            } else {
                                manageAdminService(false)
                                currentScreen = "main"
                            }
                        } else {
                            if (AdminConfig.isAdmin(user.email)) {
                                manageAdminService(true)
                                currentScreen = "admin_welcome"
                            } else {
                                manageAdminService(false)
                                currentScreen = "email_verification"
                            }
                        }
                    } else {
                        manageAdminService(false)
                        currentScreen = "login"
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when (currentScreen) {
                        "splash" -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = com.example.esteticaapp.ui.theme.VibrantPink)
                            }
                        }
                        "login" -> LoginScreen(
                            initialEmail = initialEmailState,
                            onLoginSuccess = { email -> 
                                scope.launch {
                                    if (AdminConfig.isAdmin(email)) {
                                        manageAdminService(true)
                                        currentScreen = "admin_welcome"
                                    } else {
                                        currentScreen = "main"
                                    }
                                }
                            },
                            onRegisterClick = { currentScreen = "register" },
                            onForgotPasswordClick = { email -> 
                                initialEmailState = email
                                currentScreen = "forgot_password" 
                            },
                            onNavigateTo = { screen ->
                                if (screen == "agenda") {
                                    currentScreen = "main"
                                } else if (screen == "admin_dashboard") {
                                    currentScreen = "admin_welcome"
                                } else {
                                    currentScreen = screen
                                }
                            }
                        )
                        "register" -> ProfileRegistrationScreen(
                            onSaveSuccess = { email -> 
                                initialEmailState = email
                                currentScreen = "email_verification" 
                            },
                            onBackClick = { 
                                auth.signOut()
                                manageAdminService(false)
                                currentScreen = "login" 
                            }
                        )
                        "email_verification" -> EmailVerificationScreen(
                            onBackToLogin = { 
                                auth.signOut()
                                manageAdminService(false)
                                currentScreen = "login" 
                            }
                        )
                        "forgot_password" -> ForgotPasswordScreen(
                            initialEmail = initialEmailState,
                            onBackToLogin = { currentScreen = "login" }
                        )
                        "admin_welcome" -> AdminWelcomeScreen(
                            onNavigateToDashboard = { currentScreen = "admin_dashboard" },
                            onNavigateToGallery = { currentScreen = "admin_gallery" },
                            onLogout = {
                                auth.signOut()
                                manageAdminService(false)
                                currentScreen = "login"
                            }
                        )
                        "admin_dashboard" -> AdminDashboardScreen(
                            onLogout = { 
                                auth.signOut()
                                manageAdminService(false)
                                currentScreen = "login" 
                            },
                            onBackToWelcome = { currentScreen = "admin_welcome" }
                        )
                        "admin_gallery" -> {
                            AdminGalleryScreen(
                                onBack = { currentScreen = "admin_welcome" },
                                onLogout = {
                                    auth.signOut()
                                    manageAdminService(false)
                                    currentScreen = "login"
                                }
                            )
                        }
                        "main" -> MainScreen(
                            onLogout = { 
                                auth.signOut()
                                manageAdminService(false)
                                currentScreen = "login" 
                            }
                        )
                    }

                    // Overlay global de desconexión
                    NoConnectionOverlay(isVisible = !isOnline)
                }
            }
        }
    }
}
