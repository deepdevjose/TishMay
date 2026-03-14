package com.example.esteticaapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryPink,
    onPrimary = Color.White,
    secondary = GoldAccent,
    background = Color(0xFF1C1B1F),
    surface = Color(0xFF1C1B1F),
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryPink,
    onPrimary = Color.White,
    secondary = GoldAccent,
    background = BackgroundPink,
    surface = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    primaryContainer = BackgroundPink,
    onPrimaryContainer = PrimaryDarkPink
)

@Composable
fun EsteticaAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disabled for brand consistency
    content: @Composable () -> Unit
) {
    // Forzamos el uso de LightColorScheme para mantener la consistencia de marca
    // y evitar problemas de visibilidad en dispositivos con modo oscuro forzado.
    val colorScheme = LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
