/*
 * AdminUiState representa el estado de la UI del panel de administracion.
 * Controla si la pantalla esta en carga y que filtro de citas esta activo.
 * El filtro inicia en "Todas" para mostrar la vista general por defecto.
 */

package com.example.esteticaapp.feature.admin.presentation

data class AdminUiState(
    val isLoading: Boolean = false,
    val selectedFilter: String = "Todas"
)
