/*
 * GaleriaItem representa un elemento visual de la galeria de la app.
 * Contiene identificador, título, URL de imagen, categoria y descripcion
 * para mostrar y organizar contenido, además de la fecha de última actualizacion.
 */

package com.example.esteticaapp.core.model

data class GaleriaItem(
    val id: String = "",
    val title: String = "",
    val imageUrl: String = "",
    val category: String = "",
    val description: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
