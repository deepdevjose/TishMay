package com.example.esteticaapp.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.esteticaapp.feature.admin.ui.AdminDashboardScreen
import com.example.esteticaapp.feature.admin.ui.AdminGalleryScreen
import com.example.esteticaapp.feature.admin.ui.AdminWelcomeScreen
import com.example.esteticaapp.feature.auth.ui.EmailVerificationScreen
import com.example.esteticaapp.feature.auth.ui.ForgotPasswordScreen
import com.example.esteticaapp.feature.auth.ui.LoginScreen
import com.example.esteticaapp.feature.auth.ui.ProfileRegistrationScreen
import com.example.esteticaapp.feature.home.ui.MainScreen
import com.example.esteticaapp.ui.theme.VibrantPink

@Composable
fun AppNavGraph(
    currentScreen: String,
    initialEmail: String,
    onInitialEmailChange: (String) -> Unit,
    onScreenChange: (String) -> Unit,
    onLoginSuccess: (String) -> Unit,
    onLogout: () -> Unit
) {
    when (currentScreen) {
        Routes.Splash -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = VibrantPink)
            }
        }

        Routes.Login -> LoginScreen(
            initialEmail = initialEmail,
            onLoginSuccess = onLoginSuccess,
            onRegisterClick = { onScreenChange(Routes.Register) },
            onForgotPasswordClick = { email ->
                onInitialEmailChange(email)
                onScreenChange(Routes.ForgotPassword)
            },
            onNavigateTo = { screen ->
                when (screen) {
                    "agenda" -> onScreenChange(Routes.Main)
                    Routes.AdminDashboard -> onScreenChange(Routes.AdminWelcome)
                    else -> onScreenChange(screen)
                }
            }
        )

        Routes.Register -> ProfileRegistrationScreen(
            onSaveSuccess = { email ->
                onInitialEmailChange(email)
                onScreenChange(Routes.EmailVerification)
            },
            onBackClick = onLogout
        )

        Routes.EmailVerification -> EmailVerificationScreen(onBackToLogin = onLogout)

        Routes.ForgotPassword -> ForgotPasswordScreen(
            initialEmail = initialEmail,
            onBackToLogin = { onScreenChange(Routes.Login) }
        )

        Routes.AdminWelcome -> AdminWelcomeScreen(
            onNavigateToDashboard = { onScreenChange(Routes.AdminDashboard) },
            onNavigateToGallery = { onScreenChange(Routes.AdminGallery) },
            onLogout = onLogout
        )

        Routes.AdminDashboard -> AdminDashboardScreen(
            onLogout = onLogout,
            onBackToWelcome = { onScreenChange(Routes.AdminWelcome) }
        )

        Routes.AdminGallery -> AdminGalleryScreen(
            onBack = { onScreenChange(Routes.AdminWelcome) },
            onLogout = onLogout
        )

        Routes.Main -> MainScreen(onLogout = onLogout)
    }
}

