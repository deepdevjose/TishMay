# Feature: Appointments

## Que hace

Gestiona el ciclo de vida completo de una cita entre cliente y admin.

## Flujos principales

1. Cliente crea cita en agenda.
2. Admin recibe alerta y revisa en dashboard.
3. Admin actualiza estado de la cita.
4. Cliente visualiza estado actualizado y puede cancelar o reseñar.

## Pantallas involucradas

- `AgendaScreen`
- `AdminDashboardScreen`

## Reglas de negocio

- Limite por cliente: maximo 2 citas por dia y 5 por mes.
- No se permiten horarios ya pasados ni domingo.
- Maximo 3 cancelaciones por cliente.
- Reseña habilitada solo en estado `Completada`.

## Estados de cita

- `Pendiente`
- `Confirmada`
- `Rechazada`
- `Cancelada`
- `No Asistio`
- `Completada`

