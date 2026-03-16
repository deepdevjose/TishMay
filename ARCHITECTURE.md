# Arquitectura Técnica - EstéticaApp

Este documento proporciona una visión profunda de la estructura y decisiones técnicas de **EstéticaApp**, diseñada para ser escalable, reactiva y orientada al usuario.

## 🏗️ Patrón de Arquitectura: MVVM Progresivo

La aplicación implementa el patrón **Model-View-ViewModel**, aprovechando al máximo la reactividad de **Jetpack Compose** y los flujos de Firebase.

### 1. Capa de Interfaz (UI)
- **Declarativa:** Construida al 100% con Jetpack Compose.
- **Navegación:** Gestión centralizada mediante `NavHost`.
- **Tematización:** Sistema de diseño personalizado en `ui.theme` que define una paleta cromática sofisticada (PrimaryPink, SoftRose) y dimensiones consistentes.

### 2. Capa de Lógica (ViewModel)
- **Gestión de Estado:** Uso de `MutableStateFlow` y delegados `remember` en Compose para asegurar una UI fluida.
- **Reactividad Backend:** Los ViewModels (y las pantallas principales) utilizan `addSnapshotListener` de Firestore para reaccionar a cambios en la base de datos sin necesidad de recarga manual.

### 3. Capa de Datos e Infraestructura
- **Firebase Ecosystem:**
    - **Firestore:** Almacenamiento jerárquico (`clientes`, `citas`, `config`, `administradores`).
    - **Auth:** Proveedores de Email/Password y Google Sign-In.
    - **Realtime Database (RTDB):** Utilizado exclusivamente para **señalización de baja latencia**, específicamente para notificaciones instantáneas al administrador.
- **Vertex AI (Gemini 2.0 Flash):** Integración mediante el SDK de Firebase para tareas de visión y procesamiento de lenguaje natural (Resúmenes y Diagnósticos).
- **ML Kit:** Detección de puntos de referencia faciales en tiempo real para validación biométrica local.

## 🧠 Flujos de Inteligencia Artificial

### Diagnóstico de Mirada (Clienta)
1. **Captura:** `CameraX` obtiene frames de la cámara frontal.
2. **Pre-procesamiento:** ML Kit valida la presencia de un rostro y extrae la región de interés (ojos).
3. **Inferencia:** El frame se envía a Gemini 2.0 con un prompt especializado en visagismo.
4. **Persistencia:** El resultado JSON se guarda en la subcolección `analysis_history` de la clienta.

### Resumen Inteligente (Administrador)
1. **Agregación:** Se consultan todas las citas del día actual.
2. **Contextualización:** Se recupera la configuración de `maxCapacityPerHour`.
3. **Generación:** Gemini procesa las estadísticas (confirmadas, pendientes, capacidad) para generar un informe narrativo sobre el estado de la jornada.

## 📡 Comunicación Admin-Clienta (Real-time)
Cuando una clienta realiza una reserva exitosa:
1. Se crea el documento en Firestore (`/citas`).
2. Se realiza un `push` a RTDB (`/admin_notifications`).
3. El `AdminDashboardScreen` tiene un listener activo en ese nodo de RTDB y dispara una notificación local mediante `NotificationHelper`.

## 📁 Componentes de Infraestructura Clave
- `NotificationHelper.kt`: Encapsula la creación de canales y el envío de notificaciones de sistema.
- `NetworkUtils.kt`: Monitoriza la conectividad para prevenir errores en operaciones críticas de Firebase.
- `Firestore.rules`: Capa de seguridad que restringe el acceso basado en UID y roles de administrador.
