# Data Models

## `Appointment`

Fuente: `core/model/Appointment.kt`

```kotlin
data class Appointment(
    val id: String = "",
    val userId: String = "",
    val clientName: String = "",
    val service: String = "",
    val date: String = "",
    val time: String = "",
    val status: String = "Pendiente",
    val timestamp: Long = System.currentTimeMillis(),
    val review: Map<String, Any>? = null
)
```

## `GaleriaItem`

Fuente: `core/model/GaleriaItem.kt`

```kotlin
data class GaleriaItem(
    val id: String = "",
    val title: String = "",
    val imageUrl: String = "",
    val category: String = "",
    val description: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
```

## UiState por feature

- `feature/auth/presentation/LoginUiState.kt`
- `feature/home/presentation/HomeUiState.kt`
- `feature/admin/presentation/AdminUiState.kt`
- `feature/gallery/presentation/GalleryUiState.kt`

