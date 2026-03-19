# Testing Strategy

## Objetivo

Garantizar estabilidad funcional, reglas de negocio y experiencia de usuario en flujos criticos.

## Tipos de pruebas

### Unitarias

- Validacion de reglas de agenda (limites, horarios, estados).
- Validacion de parseo/modelos.
- Validacion de utilidades (`NetworkUtils`, mapeos de estado).

### Integracion

- Auth + Firestore en entorno de pruebas.
- Flujo de creacion/actualizacion de citas.
- Emision de notificaciones via RTDB.

### UI/Instrumentadas

- Login y navegacion inicial.
- Reserva/cancelacion de citas.
- Flujo admin (confirmar/rechazar/completar).

## Como correr tests

```powershell
.\gradlew.bat testDebugUnitTest --console=plain
.\gradlew.bat connectedDebugAndroidTest --console=plain
```

## Cobertura recomendada (objetivo)

- Reglas de negocio core: >80%
- Flujos de autenticacion: >70%
- Flujos admin/agenda: >70%

## Casos de regresion obligatorios

1. Login email y Google.
2. Usuario admin redirige a flujo admin.
3. Cliente puede reservar y ver cita en agenda.
4. Admin cambia estado y cliente recibe actualizacion.
5. Overlay de conectividad no muestra falso offline tras desbloqueo.

