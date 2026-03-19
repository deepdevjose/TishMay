package com.example.esteticaapp.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    data object Agenda : BottomNavItem("agenda", "Agenda", Icons.Default.DateRange)
    data object Galeria : BottomNavItem("galeria", "Galeria", Icons.Default.PhotoLibrary)
    data object CameraIA : BottomNavItem("camera_ia", "IA Camera", Icons.Default.Face)
    data object Perfil : BottomNavItem("perfil", "Perfil", Icons.Default.Person)
}

