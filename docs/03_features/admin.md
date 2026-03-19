# Feature: Admin

## Que hace

Permite operar el negocio: gestionar citas, capacidad y galeria de servicios.

## Flujos principales

1. Pantalla de bienvenida admin con accesos rapidos.
2. Dashboard de citas con filtros por estado.
3. Acciones sobre citas: confirmar, rechazar, no asistio, completar.
4. Configuracion de capacidad (`config/appointments`).
5. Gestion de galeria (alta/edicion de servicios).

## Pantallas involucradas

- `AdminWelcomeScreen`
- `AdminDashboardScreen`
- `AdminGalleryScreen`

## Reglas de negocio

- Solo cuentas admin pueden ejecutar acciones de gestion global.
- Cambios de estado notifican al cliente via Realtime Database.
- Capacidad influye en disponibilidad de horarios para nuevas citas.

