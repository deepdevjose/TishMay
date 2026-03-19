# Data Flow

```mermaid
flowchart LR
    UI[Compose UI\nfeature/*/ui] --> EVT[Eventos de usuario]
    EVT --> VM[ViewModel / Presentation\nUiState + StateFlow]

    VM --> FS[(Firestore)]
    VM --> RT[(Realtime DB)]
    VM --> AUTH[(Firebase Auth)]
    VM --> IA[(Firebase AI / ML Kit)]

    FS --> VM
    RT --> VM
    AUTH --> VM
    IA --> VM

    VM --> STATE[UiState actualizado]
    STATE --> UI

    NET[NetworkUtils] --> UI
    UI --> OVERLAY[NoConnectionOverlay]
```

## Notas

- El flujo es reactivo: cambios de backend actualizan estado y Compose recompone.
- Firestore se usa para datos de negocio; RTDB para notificaciones en tiempo real.
- La conectividad se monitorea globalmente para evitar operaciones inconsistentes offline.

