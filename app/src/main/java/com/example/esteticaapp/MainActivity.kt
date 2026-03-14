package com.example.esteticaapp

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
import com.example.esteticaapp.ui.screens.*
import com.example.esteticaapp.ui.theme.EsteticaAppTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        NotificationHelper.createNotificationChannel(this)
        
        val auth = FirebaseAuth.getInstance()

        enableEdgeToEdge()
        setContent {
            EsteticaAppTheme {
                // Estado inicial de carga ("splash") mientras verificamos el rol del usuario
                var currentScreen by remember { mutableStateOf("splash") }
                
                // Estado para pasar el email a la pantalla de ForgotPassword
                var forgotPasswordInitialEmail by remember { mutableStateOf("") }

                // Verificación de sesión y rol al inicio
                LaunchedEffect(Unit) {
                    val user = auth.currentUser
                    if (user != null) {
                        if (user.isEmailVerified) {
                            // Verificamos rol de admin de forma asíncrona
                            if (AdminConfig.isAdmin(user.email)) {
                                currentScreen = "admin_dashboard"
                            } else {
                                currentScreen = "main"
                            }
                        } else {
                            // Usuario existe pero no ha verificado email
                            // (Opcional: Verificar si es admin igual, por si acaso es un admin sin verificar, 
                            // aunque por seguridad mejor requerir verificación)
                            if (AdminConfig.isAdmin(user.email)) {
                                currentScreen = "admin_dashboard"
                            } else {
                                currentScreen = "email_verification" // O mandarlo a login/verificación
                            }
                        }
                    } else {
                        currentScreen = "login"
                    }
                }

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
                        onLoginSuccess = { _ -> 
                            // Navegación para login manual (Email/Password)
                            currentScreen = "main" 
                        },
                        onRegisterClick = { currentScreen = "register" },
                        onForgotPasswordClick = { email -> 
                            forgotPasswordInitialEmail = email
                            currentScreen = "forgot_password" 
                        },
                        onNavigateTo = { screen ->
                            // CORRECCIÓN: Si el login devuelve "agenda", redirigimos a "main" 
                            // porque "agenda" no existe en este when (está dentro de MainScreen)
                            if (screen == "agenda") {
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
                        initialEmail = forgotPasswordInitialEmail,
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
