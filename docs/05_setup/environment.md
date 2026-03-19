# Environment

## Variables y configuraciones

- `local.properties`: rutas locales del SDK.
- `gradle.properties`: flags de Gradle/JVM.
- `app/build.gradle.kts`: SDKs, buildTypes y dependencias.

## Integraciones externas

### Firebase

- Configuracion via `google-services.json`.
- Datos principales en Firestore y RTDB.

### Cloudinary

- Inicializacion en `app/EsteticaApp.kt`.
- Recomendado para produccion: mover credenciales a configuracion segura (BuildConfig + secretos fuera de repo).

## Permisos Android

- `CAMERA`
- `INTERNET`
- `ACCESS_NETWORK_STATE`
- `POST_NOTIFICATIONS`
- `RECEIVE_BOOT_COMPLETED`
- `WAKE_LOCK`

## Recomendaciones de entorno

- Usar cuenta Firebase dedicada por ambiente (dev/prod).
- Mantener `google-services.json` de produccion fuera de ramas publicas.

