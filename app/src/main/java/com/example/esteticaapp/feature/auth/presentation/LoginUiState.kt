package com.example.esteticaapp.feature.auth.presentation

sealed class LoginUiState {
    data object Idle : LoginUiState()
    data object Loading : LoginUiState()
    data class Success(val navigateTo: String) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

