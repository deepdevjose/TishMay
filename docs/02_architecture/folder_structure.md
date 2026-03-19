# Folder Structure

## Estructura principal de codigo

```text
com.example.esteticaapp
|- app
|  |- EsteticaApp.kt
|  |- MainActivity.kt
|- core
|  |- config
|  |- model
|  |- network
|  |- notifications
|- feature
|  |- admin
|  |  |- ui
|  |  |- presentation
|  |- auth
|  |  |- ui
|  |  |- presentation
|  |- home
|  |  |- ui
|  |  |- presentation
|  |- gallery
|  |  |- ui
|  |  |- presentation
|- navigation
|  |- AppNavGraph.kt
|  |- Routes.kt
|  |- BottomNavItem.kt
|- ui
   |- components
   |- theme
```

## Convenciones

- `ui`: solo composables y widgets.
- `presentation`: ViewModel/UiState.
- `core/model`: data classes compartidas.
- `core/network`: utilidades transversales de conectividad.
- `core/notifications`: receiver, services y helper de notificaciones.

## Principios de organizacion

- Evitar clases duplicadas entre raiz y features.
- Mantener modelos fuera de pantallas.
- Centralizar rutas en `navigation/Routes.kt`.
- Evitar referencias cruzadas entre features salvo contratos en `core` o `navigation`.

## Regla para nuevas features

Toda nueva feature debe incluir minimo:

1. `feature/<name>/ui`
2. `feature/<name>/presentation`
3. `UiState` dedicado
4. Entrada de navegacion en `navigation` si aplica

