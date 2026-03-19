# EstéticaApp - Diagnóstico de Mirada con Inteligencia Artificial

EstéticaApp es una plataforma integral desarrollada para centros de estética especializados en diseño de mirada y extensiones de pestañas. La aplicación utiliza tecnologías avanzadas de Inteligencia Artificial Generativa y Visión por Computadora para personalizar la experiencia del cliente y optimizar la gestión operativa del negocio.

## Características Principales

### Experiencia del Cliente
*   **Diagnóstico Facial con IA**: Implementación de algoritmos de detección facial que analizan la morfología del rostro y la forma de los ojos para recomendar estilos personalizados de extensiones (ej. Cat Eye, Dolly, Natural).
*   **Agenda Inteligente**: Sistema de reservas automatizado que valida la disponibilidad en tiempo real, gestiona límites de citas y permite la cancelación o reprogramación eficiente.
*   **Historial de Servicios**: Registro detallado de tratamientos previos y diagnósticos visuales accesibles desde el perfil del usuario.
*   **Feedback y Valoraciones**: Módulo para recopilar la satisfacción del cliente mediante calificaciones y comentarios post-servicio.

### Gestión Administrativa
*   **Panel de Control (Dashboard)**: Interfaz centralizada para la administración de citas, permitiendo aceptar, rechazar o marcar servicios como completados.
*   **Asistente de Negocio con IA**: Integración de modelos generativos para analizar el rendimiento diario, calcular la ocupación y generar reportes ejecutivos.
*   **Gestión de Galería**: Herramientas para subir y organizar el portafolio de trabajos realizados, con almacenamiento optimizado en la nube.
*   **Control de Capacidad**: Configuración dinámica del personal disponible para evitar la sobreventa de espacios.
*   **Notificaciones en Tiempo Real**: Sistema de alertas instantáneas para informar al personal sobre nuevas reservas o cambios en la agenda.

## Arquitectura y Tecnologías

El proyecto sigue una arquitectura **MVVM (Model-View-ViewModel)** moderna, garantizando la separación de responsabilidades y la escalabilidad del código.

### Stack Tecnológico
*   **Lenguaje**: Kotlin
*   **Interfaz de Usuario**: Jetpack Compose (Material Design 3)
*   **Inyección de Dependencias**: Hilt (o enfoque manual según implementación)
*   **Carga de Imágenes**: Coil

### Servicios en la Nube (Backend as a Service)
*   **Firebase Authentication**: Gestión segura de identidades y sesiones de usuario.
*   **Cloud Firestore**: Base de datos NoSQL para el almacenamiento persistente de usuarios, citas y configuraciones.
*   **Firebase Realtime Database**: Sincronización de eventos críticos y notificaciones para el administrador.
*   **Firebase Vertex AI**: Implementación del modelo **Gemini 2.0 Flash** para inferencia y generación de contenido.

### Inteligencia Artificial y Machine Learning
*   **Google ML Kit**: Detección de rostros y análisis de puntos de referencia faciales en el dispositivo.
*   **CameraX**: Captura y procesamiento de imágenes optimizado para el diagnóstico visual.

### Integraciones Externas
*   **Cloudinary**: Servicio para la optimización, transformación y almacenamiento seguro de recursos multimedia.

## Estructura del Proyecto

El código fuente se organiza en los siguientes paquetes principales:

*   **ui**: Contiene todos los componentes de la interfaz de usuario, pantallas y temas.
    *   `screens`: Pantallas individuales (Login, Home, Agenda, AdminDashboard, CameraIA, etc.).
    *   `components`: Elementos reutilizables de UI.
    *   `theme`: Definiciones de diseño (colores, tipografía).
*   **viewmodel**: Clases encargadas de la gestión del estado de la UI y la lógica de negocio.
*   **data**: Capa de acceso a datos, modelos y repositorios.
*   **utils**: Clases de utilidad, helpers de red y formateadores.

## Requisitos de Instalación

Para ejecutar el proyecto en un entorno local, asegúrese de cumplir con los siguientes requisitos:

1.  **Entorno de Desarrollo**: Android Studio Ladybug o superior.
2.  **SDK de Android**: API Level 24 (Min) a 34 (Target).
3.  **Configuración de Firebase**:
    *   Crear un proyecto en la consola de Firebase.
    *   Habilitar Authentication, Firestore, Realtime Database y Vertex AI.
    *   Descargar el archivo `google-services.json` y colocarlo en el directorio `app/`.
4.  **Configuración de Cloudinary**:
    *   Obtener las credenciales de API desde el panel de Cloudinary.
    *   Configurar las claves en el archivo de propiedades correspondiente o variables de entorno.

## Permisos Requeridos

La aplicación solicita los siguientes permisos para su correcto funcionamiento:

*   `CAMERA`: Necesario para el diagnóstico facial y captura de evidencia.
*   `INTERNET`: Requerido para la comunicación con servicios en la nube.
*   `POST_NOTIFICATIONS`: Para recibir alertas de estado de citas.

---
Este proyecto demuestra la integración efectiva de soluciones nativas de Android con servicios en la nube e Inteligencia Artificial para resolver problemas de negocio reales.
