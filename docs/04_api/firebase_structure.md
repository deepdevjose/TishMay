# Firebase Structure

## Firestore

### `administradores/{email}`

- Documento por correo en minusculas.
- Se usa para validar rol admin.

### `clientes/{uid}`

Campos principales observados en codigo:

- `firstName`: string
- `lastName`: string
- `email`: string
- `avatar`: string
- `cancellationCount`: number
- `completedAppointmentsCount`: number
- `referralsCount`: number
- `punctualityStreak`: number
- `hasExploredGallery`: boolean
- `last_analysis`: map

Subcoleccion:

- `analysis_history/{id}`: historial de analisis IA.

### `citas/{id}`

- `userId`: string
- `clientName`: string
- `service`: string
- `date`: string
- `time`: string
- `status`: string
- `timestamp`: number
- `review`: map|null

### `gallery_services/{id}`

- `title`: string
- `imageUrl`: string
- `category`: string
- `description`: string
- `updatedAt`: number

### `config/appointments`

- `maxCapacityPerHour`: number

## Realtime Database

### `/admin_notifications/{id}`

- `title`: string
- `message`: string
- `timestamp`: number
- `read`: boolean

### `/client_notifications/{userId}/{id}`

- `title`: string
- `message`: string
- `timestamp`: number
- `read`: boolean

