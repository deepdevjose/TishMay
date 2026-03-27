/**
 * MainScreen es la estructura principal de navegación de la aplicación para el cliente.
 * Actúa como el contenedor base que orquesta el flujo entre las diferentes secciones
 * mediante una barra de navegación inferior (Bottom Navigation).
 * 
 * Tecnologías utilizadas:
 * - Navigation Compose: Para gestionar el NavHost y el NavController.
 * - Jetpack Compose Scaffold: Proporciona la estructura estándar con barra de navegación.
 * - Material 3: Componentes de NavigationBar y NavigationBarItem para una UI moderna.
 * 
 * Funcionalidades clave:
 * - Navegación entre Agenda, Galería, Cámara IA y Perfil.
 * - Control de Bloqueo: Permite desactivar la navegación cuando se realizan procesos
 *   críticos (como el escaneo de IA) para evitar interrupciones.
 * - Gestión de BackStack: Asegura que el estado de las pantallas se preserve al navegar.
 */

package com.example.esteticaapp.feature.home.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.esteticaapp.feature.gallery.ui.GaleriaScreen
import com.example.esteticaapp.navigation.BottomNavItem
import com.example.esteticaapp.ui.theme.BackgroundPink
import com.example.esteticaapp.ui.theme.VibrantPink

@Composable
fun MainScreen(onLogout: () -> Unit) {
    val navController = rememberNavController()
    var isNavigationLocked by remember { mutableStateOf(false) }

    val items = listOf(
        BottomNavItem.Agenda,
        BottomNavItem.Galeria,
        BottomNavItem.CameraIA,
        BottomNavItem.Perfil
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = BackgroundPink,
                contentColor = VibrantPink
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        enabled = !isNavigationLocked,
                        onClick = {
                            if (!isNavigationLocked) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = VibrantPink,
                            selectedTextColor = VibrantPink,
                            indicatorColor = VibrantPink.copy(alpha = 0.1f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                            disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Agenda.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Agenda.route) { AgendaScreen() }
            composable(BottomNavItem.Galeria.route) {
                GaleriaScreen(
                    onNavigateToBooking = {
                        navController.navigate(BottomNavItem.Agenda.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                ) 
            }
            composable(BottomNavItem.CameraIA.route) {
                CameraIAScreen(onLockNavigation = { isNavigationLocked = it }) 
            }
            composable(BottomNavItem.Perfil.route) {
                PerfilScreen(
                    onLogout = onLogout,
                    onNavigateToAgenda = {
                        navController.navigate(BottomNavItem.Agenda.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                ) 
            }
        }
    }
}
