# Feature: Gallery

## Que hace

Muestra portafolio de servicios al cliente y habilita gestion para admin.

## Flujos principales

1. Lectura de items desde `gallery_services`.
2. Filtro por categoria.
3. Visualizacion fullscreen de imagenes.
4. CTA para ir a reserva desde item de galeria.

## Pantallas involucradas

- `GaleriaScreen` (cliente)
- `AdminGalleryScreen` (admin)

## Reglas de negocio

- El item se considera configurado si tiene `imageUrl` valido.
- Actualizaciones quedan con `updatedAt` para trazabilidad.

