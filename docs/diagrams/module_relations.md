# Module Relations

```mermaid
graph TD
    APP[app/*\nMainActivity + EsteticaApp] --> NAV[navigation/*]

    NAV --> AUTH[feature/auth]
    NAV --> HOME[feature/home]
    NAV --> ADMIN[feature/admin]
    NAV --> GALLERY[feature/gallery]

    AUTH --> CORECFG[core/config]
    HOME --> COREMODEL[core/model]
    HOME --> CORENET[core/network]
    HOME --> CORENOTIF[core/notifications]
    ADMIN --> COREMODEL
    ADMIN --> CORENOTIF
    GALLERY --> COREMODEL

    UI[ui/components + ui/theme] --> AUTH
    UI --> HOME
    UI --> ADMIN
    UI --> GALLERY
```

## Notas

- `navigation` centraliza rutas y flujo entre features.
- `core` expone contratos compartidos; evita acoplar features entre si.
- `ui` contiene componentes transversales y tema visual.

