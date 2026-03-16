# Reglas de Negocio - EstéticaApp

Este documento define la lógica operativa, restricciones y flujos de decisión que rigen el comportamiento de **EstéticaApp**.

## 1. Gestión de Citas y Agenda (Core)

### Restricciones de Reserva
- **Frecuencia por Usuario:**
    - Máximo **2 citas por día** por clienta.
    - Máximo **5 citas por mes** por clienta.
    - *Excepción:* Para exceder estos límites, la clienta debe contactar directamente vía WhatsApp (botón integrado).
- **Bloqueos de Horario:**
    - No se pueden reservar horarios que ya han pasado en el día actual.
    - Los domingos no son laborables y están bloqueados en el calendario.
- **Unicidad:** Una clienta no puede tener dos citas en el mismo bloque horario.

### Capacidad del Local
- **Capacidad Dinámica:** El administrador define cuántas **aplicadoras** hay disponibles por turno (2 horas).
- **Límite de Concurrencia:** Si hay `N` aplicadoras, el sistema solo permite `N` citas simultáneas en un mismo bloque horario. Una vez alcanzado, el horario se marca como "Lleno".

### Gestión de Estados
1.  **Pendiente:** Estado inicial. La cita aparece en naranja en el dashboard.
2.  **Confirmada:** El administrador acepta la solicitud. La clienta recibe confirmación visual en su agenda.
3.  **Cancelada:** 
    - La clienta puede cancelar desde su app.
    - Cada cancelación incrementa el `cancellationCount` del perfil. 
    - Límite de **3 cancelaciones por mes**.
4.  **Rechazada:** El administrador declina la cita (ej. falta de insumos o error de agenda).
5.  **No Asistió:** El administrador marca este estado si la clienta no llega tras 15 min de gracia.
6.  **Completada:** Marcado manualmente por el administrador. Esto habilita la opción de reseña para la clienta e incrementa su contador de servicios totales.

## 2. Análisis de Mirada (IA Diagnosis)

- **Validación Biométrica:** El escaneo requiere la detección de un rostro humano con ambos ojos visibles mediante ML Kit. Sin esta validación, no se envía el frame a Gemini.
- **Motor de Inferencia:** Se utiliza **Gemini 2.0 Flash** con un prompt de experto en visagismo.
- **Privacidad:** Las imágenes son efímeras (procesadas en memoria). Solo se persiste el texto del diagnóstico y la recomendación de estilo en el historial (`analysis_history`) del usuario.

## 3. Resumen Inteligente para Administradores

- **Criterio de Análisis:** Solo considera citas cuya fecha coincida con el día actual (normalizando formatos de fecha para evitar errores por idioma del dispositivo).
- **Cálculo de Productividad:** 
    - Compara citas confirmadas vs capacidad máxima diaria (`maxCapacityPerHour * 4 turnos`).
- **Priorización:** El asistente resalta primero las citas **Pendientes** para forzar la toma de decisiones administrativa.

## 4. Roles y Seguridad (Firebase Rules)

- **Administrador:**
    - Identificado por su correo en la colección `/administradores/`.
    - Permiso de escritura en `/config/`, `/gallery_services/` y actualización de estado en cualquier documento de `/citas/`.
- **Cliente:**
    - Solo puede leer/escribir en su propio documento en `/clientes/{userId}/`.
    - Solo puede crear citas donde `userId` coincida con su UID autenticado.

## 5. Feedback y Reseñas

- **Disponibilidad:** El formulario de reseña solo es visible para citas con estado `Completada` que no tengan una reseña previa (`review == null`).
- **Sistema de Calificación:** 1 a 5 estrellas + comentario opcional. Los datos se guardan directamente en el documento de la cita para mantener la integridad histórica.
