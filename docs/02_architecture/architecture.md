# Architecture

## Estilo arquitectonico

El proyecto sigue un enfoque **MVVM + feature-based packaging**.

- `feature/*/ui`: pantallas Compose.
- `feature/*/presentation`: estado y ViewModels.
- `core/*`: modelos compartidos, red, config y notificaciones.
- `navigation/*`: rutas y grafo de navegacion.
- `app/*`: entrypoints (`MainActivity`, `EsteticaApp`).

## Capas

### App Layer

- Inicializa tema, sesion y observacion de conectividad.
- Orquesta flujo inicial (`splash`, auth, admin, main).

### Presentation Layer

- Composables + UiState por feature.
- Manejo reactivo con `StateFlow` y `collectAsState()`.

### Domain/Rules (impl in features + core)

- Reglas de negocio para agenda, capacidades y estados.
- Validaciones de acceso por rol admin/cliente.

### Data Layer (Firebase BaaS)

- Firestore para entidades de negocio.
- Realtime Database para notificaciones near real-time.
- Firebase Auth para identidad.

## Flujo de datos

1. UI dispara evento de usuario.
2. ViewModel/feature ejecuta logica y consulta Firebase.
3. Resultado actualiza UiState.
4. Compose recompone.
5. Cambios en Firestore/RTDB se reflejan por listeners.

## Decisiones tecnicas

### Por que Firebase

- Reduce tiempo de salida al mercado.
- SDK Android estable para auth + datos + listeners.
- Escala para MVP y fases de crecimiento.

### Por que Jetpack Compose

- UI declarativa y mantenible.
- Mejor productividad en iteraciones de producto.
- Integracion natural con `StateFlow`.

### Por que package-by-feature

- Reduce acoplamiento entre modulos.
- Facilita ownership por modulo.
- Escala mejor para nuevas features.

## Consideraciones no funcionales

- Observacion de conectividad con `NetworkUtils`.
- Servicios foreground para notificaciones criticas.
- Soporte offline parcial con bloqueo de acciones que requieren internet.

