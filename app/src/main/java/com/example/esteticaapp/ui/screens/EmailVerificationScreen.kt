package com.example.esteticaapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.esteticaapp.ui.theme.BackgroundPink
import com.example.esteticaapp.ui.theme.PrimaryPink
import com.example.esteticaapp.ui.theme.TextSecondary
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

@Composable
fun EmailVerificationScreen(
    onBackToLogin: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    var isResending by remember { mutableStateOf(false) }
    var resendMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPink)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.MarkEmailRead,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = PrimaryPink
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "¡Verifica tu correo!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Hemos enviado un enlace de confirmación a tu correo electrónico. Por favor, revisa tu bandeja de entrada y la carpeta de spam.",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onBackToLogin,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink)
        ) {
            Text("Ir al Inicio de Sesión", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = {
                isResending = true
                auth.currentUser?.sendEmailVerification()
                    ?.addOnCompleteListener {
                        isResending = false
                        resendMessage = if (it.isSuccessful) "Correo reenviado con éxito" else "Error al reenviar"
                    }
            },
            enabled = !isResending
        ) {
            Text(
                text = if (isResending) "Reenviando..." else "¿No recibiste el correo? Reenviar",
                color = PrimaryPink
            )
        }

        if (resendMessage != null) {
            Text(
                text = resendMessage!!,
                style = MaterialTheme.typography.bodySmall,
                color = if (resendMessage!!.contains("éxito")) Color(0xFF4CAF50) else Color.Red
            )
        }
    }
}
