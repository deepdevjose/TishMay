package com.example.esteticaapp.ui.screens

data class GaleriaItem(
    val id: String = "",
    val title: String = "",
    val imageUrl: String = "",
    val category: String = "",
    val description: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
