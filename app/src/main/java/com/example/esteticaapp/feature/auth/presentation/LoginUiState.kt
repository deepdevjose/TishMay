package com.example.esteticaapp.feature.auth.presentation

/*
 * LoginUiState modela los estados de la UI durante el flujo de inicio de sesion.
 * Incluye estado inactivo, carga, exito con destino de navegacion
 * y error con mensaje para retroalimentacion al usuario.
 */

sealed class LoginUiState {
    data object Idle : LoginUiState()
    data object Loading : LoginUiState()
    data class Success(val navigateTo: String) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}
