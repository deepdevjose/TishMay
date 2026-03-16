# EstéticaApp - Diagnóstico de Mirada con IA 👁️✨

**EstéticaApp** es una solución integral para centros de estética especializados en extensiones de pestañas y diseño de mirada. Combina la potencia de la Inteligencia Artificial generativa (**Gemini 2.0 Flash**) y Visión por Computadora (**ML Kit**) para transformar la experiencia del cliente y optimizar la gestión administrativa.

## 🚀 Características Principales

### Para Clientas (Customer Experience)
- **Análisis de Mirada IA:** Escaneo biométrico que recomienda estilos (Cat Eye, Dolly, Natural, etc.) analizando la forma del ojo y rasgos faciales.
- **Agenda Inteligente:** Sistema de reservas con validación de disponibilidad en tiempo real, límites de citas diarias/mensuales y gestión de cancelaciones.
- **Historial de Belleza:** Acceso a diagnósticos previos y seguimiento de servicios realizados.
- **Feedback & Reseñas:** Posibilidad de calificar la experiencia con estrellas y comentarios tras finalizar el servicio.

### Para Administradores (Business Control)
- **Asistente de Resumen IA:** Un panel inteligente que analiza la agenda del día, calcula la ocupación y genera un reporte de voz/texto sobre el estado del negocio.
- **Dashboard de Gestión:** Control total sobre el flujo de citas (Aceptar, Rechazar, Completar, No Asistió).
- **Control de Capacidad:** Configuración dinámica del número de aplicadoras disponibles por turno para evitar sobrecupos.
- **Gestión de Galería:** Catálogo dinámico de trabajos realizados con integración directa a la nube.
- **Notificaciones Real-time:** Alertas instantáneas mediante Firebase Realtime DB cuando una clienta agenda un servicio.

## 🛠️ Stack Tecnológico

- **Lenguaje:** [Kotlin](https://kotlinlang.org/)
- **UI:** [Jetpack Compose](https://developer.android.com/jackpack/compose) con Material Design 3.
- **Backend:** 
    - **Firebase Auth:** Autenticación segura y gestión de perfiles.
    - **Firestore:** Base de datos NoSQL para citas, configuración y usuarios.
    - **Realtime Database:** Sincronización de notificaciones administrativas.
    - **Vertex AI (Gemini 2.0):** Motor de inferencia para diagnósticos y resúmenes.
- **IA/ML:** Google ML Kit Face Detection.
- **Imágenes:** Cloudinary (Storage & Optimization).
- **Efectos:** Konfetti para celebraciones de feedback.

## 📁 Estructura del Proyecto

```text
com.example.esteticaapp
├── ui
│   ├── screens       # Pantallas principales (Home, Agenda, AdminDashboard, etc.)
│   ├── components    # Componentes UI reutilizables (Chips, Cards, Dialogs)
│   └── theme         # Sistema de diseño (Color.kt, Type.kt, Dimensions.kt)
├── data              # (Refactorización) Modelos de datos y DTOs
├── viewmodel         # Lógica de estado y conexión con Firebase
├── utils             # Helpers (Network, Notifications, DateFormatters)
└── MainActivity.kt   # Navegación y punto de entrada
```

## 📋 Configuración Rápida

1. **Firebase:** Colocar `google-services.json` en `app/`. Habilitar Firestore, Auth (Google/Email), RTDB y Vertex AI.
2. **Cloudinary:** Configurar credenciales en la capa de datos/config para la subida de imágenes.
3. **Build:** Ejecutar `./gradlew installDebug`.

---
*EstéticaApp es una plataforma diseñada para escalar el negocio de la belleza mediante la digitalización inteligente.*
