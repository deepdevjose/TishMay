# Navigation Flow

```mermaid
flowchart TD
    S[splash] --> L[login]
    L --> R[register]
    L --> F[forgot_password]
    L --> EV[email_verification]
    L --> M[main]
    L --> AW[admin_welcome]

    R --> EV
    EV --> L

    AW --> AD[admin_dashboard]
    AW --> AG[admin_gallery]
    AD --> AW
    AG --> AW

    M --> AGENDA[agenda]
    M --> GALERIA[galeria]
    M --> CAM[camera_ia]
    M --> PERFIL[perfil]
```

