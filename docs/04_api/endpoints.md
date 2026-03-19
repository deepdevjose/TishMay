# Endpoints and Data Contracts

El proyecto usa Firebase SDK y no expone endpoints REST propios.

## Operaciones clave

### Auth

- `FirebaseAuth.signInWithEmailAndPassword`
- `FirebaseAuth.createUserWithEmailAndPassword`
- `CredentialManager` + `GoogleAuthProvider`

### Firestore

- Lectura por listener: `addSnapshotListener`
- Lectura puntual: `get()`/`await()`
- Escritura: `set`, `update`, `add`
- Operaciones atomicas: `runTransaction`

### Realtime Database

- Escucha incremental: `addChildEventListener`
- Publicacion de notificaciones: `push().setValue(...)`

## Seguridad y acceso (resumen)

- Admin: permisos extendidos para gestion global.
- Cliente: acceso restringido a su propio UID.
- Reglas deben validar `request.auth != null` y ownership por documento.

> Nota: documentar reglas exactas en Firebase Console cuando se formalice release.

