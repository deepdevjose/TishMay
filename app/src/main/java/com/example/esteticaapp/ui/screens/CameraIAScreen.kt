package com.example.esteticaapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.esteticaapp.ui.theme.VibrantPink

@Composable
fun CameraIAScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Simulación de vista de cámara
    ) {
        // Overlay de información simple
        Text(
            text = "Análisis de Mirada IA",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp)
        )

        // Botón de captura circular
        Button(
            onClick = { /* Capturar */ },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .size(80.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = VibrantPink),
            contentPadding = PaddingValues(0.dp)
        ) {
            // Círculo interno para diseño de cámara
            Surface(
                modifier = Modifier.size(60.dp),
                shape = CircleShape,
                color = Color.Transparent,
                border = androidx.compose.foundation.BorderStroke(2.dp, Color.White)
            ) {}
        }
    }
}
