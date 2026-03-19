# Arquitectura Técnica Detallada - EstéticaApp

Este documento describe la infraestructura, patrones de diseño y flujos de datos de **EstéticaApp**, diseñada para ser una plataforma robusta, segura y altamente reactiva.

## 🏗️ Patrón de Arquitectura: MVVM Progresivo

La aplicación implementa el patrón **Model-View-ViewModel (MVVM)**, aprovechando la reactividad de **Jetpack Compose** y los flujos en tiempo real de Firebase.

### 1. Capa de Presentación (UI)
- **Compose-First:** Interfaz 100% declarativa. No se utilizan archivos XML para layouts.
- **Navegación Basada en Estado:** El flujo de pantallas en `MainActivity.kt` se determina dinámicamente según el estado de autenticación y el rol (Admin vs. Cliente).
- **Gestión de Retroceso (Back Handling):** Uso de `androidx.activity.compose.BackHandler` para interceptar gestos del sistema y evitar cierres accidentales en flujos críticos.

### 2. Capa de Lógica (ViewModel)
- **Gestión de Estado:** Uso de `StateFlow` para emitir estados inmutables.
- **Structured Concurrency:** Todas las operaciones asíncronas están ligadas al ciclo de vida del componente mediante `viewModelScope` o `LaunchedEffect`.

## 🔐 Sistema de Identidad y Acceso

### Estrategias de Autenticación
1.  **Google OAuth:** Implementado con `CredentialManager` para un flujo moderno y seguro.
2.  **Email y Contraseña:** Sistema tradicional con verificación de correo obligatoria.
3.  **Vinculación de Cuentas (Account Linking):** Los usuarios de Google pueden asignar una contraseña a su perfil mediante `linkWithCredential`, permitiendo el acceso híbrido bajo una identidad única.

## ☁️ Infraestructura de Notificaciones (WhatsApp Style)

Para garantizar que las notificaciones lleguen incluso si la app está cerrada, se implementó una arquitectura basada en **Servicios en Primer Plano (Foreground Services)**:

### 1. Canales de Escucha (RTDB)
- **AdminNotificationService:** Escucha el nodo `/admin_notifications` para nuevas citas.
- **ClientNotificationService:** Escucha el nodo `/client_notifications/{userId}` para confirmaciones.
- **Motor:** Se utiliza `addChildEventListener` con `limitToLast(10)` para maximizar la eficiencia y reducir el consumo de datos y batería.

### 2. Política de Transparencia de Android
Siguiendo las normativas de Android (API 31+), los servicios muestran una notificación persistente ("Servicio de Estética Activo"). Esto informa al usuario que la app está procesando datos en segundo plano para su beneficio, garantizando seguridad y confiabilidad.

## 🧠 Inteligencia Artificial (Vertex AI + ML Kit)

### Diagnóstico de Mirada
1.  **Visión Local:** ML Kit valida la presencia de rostro y ojos antes de la inferencia.
2.  **IA Generativa:** Gemini 2.5 Flash procesa el frame con un "System Prompt" especializado en visagismo.
3.  **Resumen Ejecutivo:** Generación de reportes narrativos para el administrador basados en métricas de agenda y capacidad.

## 🛡️ Resiliencia Offline
- **NetworkUtils:** Monitoreo reactivo de la red.
- **NoConnectionOverlay:** Bloqueo total de la UI mediante `pointerInput` y `zIndex` alto cuando se pierde la conexión, evitando estados inconsistentes en Firebase.
