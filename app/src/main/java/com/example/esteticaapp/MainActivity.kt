package com.example.esteticaapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.esteticaapp.ui.theme.EsteticaAppTheme
import com.example.esteticaapp.ui.screens.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

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
                val scope = rememberCoroutineScope()
                
                // Estado para pasar el email a la pantalla de ForgotPassword y Login
                var initialEmailState by remember { mutableStateOf("") }

                // Verificación de sesión y rol al inicio
                LaunchedEffect(Unit) {
                    val user = auth.currentUser
                    if (user != null) {
                        if (user.isEmailVerified) {
                            // Verificamos rol de admin de forma asíncrona
                            if (AdminConfig.isAdmin(user.email)) {
                                currentScreen = "admin_welcome"
                            } else {
                                currentScreen = "main"
                            }
                        } else {
                            // Usuario existe pero no ha verificado email
                            if (AdminConfig.isAdmin(user.email)) {
                                currentScreen = "admin_welcome"
                            } else {
                                currentScreen = "email_verification"
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
                        initialEmail = initialEmailState,
                        onLoginSuccess = { email -> 
                            scope.launch {
                                if (AdminConfig.isAdmin(email)) {
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
                                // Redirigir a welcome para admins si intentan ir directo al dashboard
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
                        onBackClick = { currentScreen = "login" }
                    )
                    "email_verification" -> EmailVerificationScreen(
                        onBackToLogin = { 
                            auth.signOut()
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
                            currentScreen = "login"
                        }
                    )
                    "admin_dashboard" -> AdminDashboardScreen(
                        onLogout = { 
                            auth.signOut()
                            currentScreen = "login" 
                        },
                        onBackToWelcome = { currentScreen = "admin_welcome" }
                    )
                    "admin_gallery" -> {
                        AdminGalleryScreen(
                            onBack = { currentScreen = "admin_welcome" },
                            onLogout = {
                                auth.signOut()
                                currentScreen = "login"
                            }
                        )
                    }
                    "main" -> MainScreen(
                        onLogout = { 
                            auth.signOut()
                            currentScreen = "login" 
                        }
                    )
                }
            }
        }
    }
}
