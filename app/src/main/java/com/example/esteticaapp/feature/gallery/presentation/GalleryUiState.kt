package com.example.esteticaapp.feature.gallery.presentation

import com.example.esteticaapp.core.model.GaleriaItem

data class GalleryUiState(
    val isLoading: Boolean = true,
    val selectedCategory: String = "Todos",
    val items: List<GaleriaItem> = emptyList()
)

