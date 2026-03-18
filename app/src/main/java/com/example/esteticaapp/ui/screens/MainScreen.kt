package com.example.esteticaapp.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.esteticaapp.ui.theme.BackgroundPink
import com.example.esteticaapp.ui.theme.VibrantPink

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Agenda : Screen("agenda", "Agenda", Icons.Default.DateRange)
    object Galeria : Screen("galeria", "Galería", Icons.Default.PhotoLibrary)
    object CameraIA : Screen("camera_ia", "IA Camera", Icons.Default.Face)
    object Perfil : Screen("perfil", "Perfil", Icons.Default.Person)
}

@Composable
fun MainScreen(onLogout: () -> Unit) {
    val navController = rememberNavController()
    var isNavigationLocked by remember { mutableStateOf(false) }

    val items = listOf(
        Screen.Agenda,
        Screen.Galeria,
        Screen.CameraIA,
        Screen.Perfil
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
            startDestination = Screen.Agenda.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Agenda.route) { AgendaScreen() }
            composable(Screen.Galeria.route) { 
                GaleriaScreen(
                    onNavigateToBooking = {
                        navController.navigate(Screen.Agenda.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                ) 
            }
            composable(Screen.CameraIA.route) { 
                CameraIAScreen(onLockNavigation = { isNavigationLocked = it }) 
            }
            composable(Screen.Perfil.route) { 
                PerfilScreen(
                    onLogout = onLogout,
                    onNavigateToAgenda = {
                        navController.navigate(Screen.Agenda.route) {
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
