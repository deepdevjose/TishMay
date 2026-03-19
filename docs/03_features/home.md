# Feature: Home

## Que hace

Entrega la experiencia principal del cliente: agenda, perfil, galeria y escaneo IA.

## Flujos principales

1. Navegacion inferior entre Agenda, Galeria, IA y Perfil.
2. Consulta y refresco de citas del usuario.
3. Visualizacion de progreso/logros y datos de perfil.
4. Analisis de mirada por camara y guardado de resultado.

## Pantallas involucradas

- `MainScreen`
- `AgendaScreen`
- `PerfilScreen`
- `CameraIAScreen`
- `HomeScreen` (pantalla de inicio complementaria)

## Reglas de negocio

- Acciones de escritura bloqueadas sin conectividad.
- Cancelaciones incrementan `cancellationCount`.
- Reseña solo para citas completadas sin reseña previa.

