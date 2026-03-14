package com.example.esteticaapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.example.esteticaapp.ui.screens.*
import com.example.esteticaapp.ui.theme.EsteticaAppTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        NotificationHelper.createNotificationChannel(this)
        
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        
        // Determinar pantalla inicial basada en si el usuario ya está logueado y verificado
        val initialScreen = if (currentUser != null && (currentUser.isEmailVerified || AdminConfig.isAdmin(currentUser.email))) {
            if (AdminConfig.isAdmin(currentUser.email)) "admin_dashboard" else "main"
        } else {
            "login"
        }

        enableEdgeToEdge()
        setContent {
            EsteticaAppTheme {
                var currentScreen by remember { mutableStateOf(initialScreen) }

                when (currentScreen) {
                    "login" -> LoginScreen(
                        onLoginSuccess = { email ->
                            val user = auth.currentUser
                            if (AdminConfig.isAdmin(email)) {
                                currentScreen = "admin_dashboard"
                            } else if (user != null && user.isEmailVerified) {
                                currentScreen = "main"
                            } else {
                                // Si no está verificado, lo mandamos a la pantalla de verificación
                                currentScreen = "email_verification"
                            }
                        },
                        onRegisterClick = { currentScreen = "register" },
                        onForgotPasswordClick = { currentScreen = "forgot_password" },
                        onNavigateTo = { screen ->
                            // CORRECCIÓN: Si el login (ej. Google) pide ir a "agenda", 
                            // mandamos a "main" para que se cargue el shell con la BottomBar.
                            if (screen == "agenda" || screen == "main") {
                                currentScreen = "main"
                            } else {
                                currentScreen = screen
                            }
                        }
                    )
                    "register" -> ProfileRegistrationScreen(
                        onSaveSuccess = { currentScreen = "email_verification" },
                        onBackClick = { currentScreen = "login" }
                    )
                    "email_verification" -> EmailVerificationScreen(
                        onBackToLogin = { 
                            auth.signOut() // Cerramos sesión para que al volver a entrar se valide de nuevo
                            currentScreen = "login" 
                        }
                    )
                    "forgot_password" -> ForgotPasswordScreen(
                        onBackToLogin = { currentScreen = "login" }
                    )
                    "admin_dashboard" -> AdminDashboardScreen(
                        onLogout = { 
                            auth.signOut()
                            currentScreen = "login" 
                        }
                    )
                    "main" -> MainScreen(
                        onLogout = { 
                            auth.signOut()
                            currentScreen = "login" 
                        }
                    )
                    // Eliminamos el caso "agenda" directo para forzar el uso de MainScreen
                }
            }
        }
    }
}
