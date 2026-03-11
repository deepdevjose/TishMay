package com.example.esteticaapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.example.esteticaapp.ui.screens.LoginScreen
import com.example.esteticaapp.ui.screens.MainScreen
import com.example.esteticaapp.ui.screens.ProfileRegistrationScreen
import com.example.esteticaapp.ui.theme.EsteticaAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EsteticaAppTheme {
                var currentScreen by remember { mutableStateOf("login") }

                when (currentScreen) {
                    "login" -> LoginScreen(
                        onLoginSuccess = { currentScreen = "main" },
                        onRegisterClick = { currentScreen = "register" }
                    )
                    "register" -> ProfileRegistrationScreen(
                        onSaveClick = { currentScreen = "login" },
                        onBackClick = { currentScreen = "login" }
                    )
                    "main" -> MainScreen(
                        onLogout = { currentScreen = "login" }
                    )
                }
            }
        }
    }
}
