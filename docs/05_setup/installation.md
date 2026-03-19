# Installation

## Requisitos

- Android Studio (version reciente estable)
- JDK 11
- Android SDK 24+
- Dispositivo o emulador con Google Play Services

## Clonar proyecto

```powershell
git clone <repo-url>
cd esteticaApp
```

## Configuracion Firebase

1. Crear proyecto en Firebase Console.
2. Habilitar Auth, Firestore, Realtime Database y Analytics.
3. Descargar `google-services.json`.
4. Colocar archivo en:
   - `google-services.json` (raiz, opcional de respaldo)
   - `app/google-services.json` (obligatorio para build)

## Build debug

```powershell
.\gradlew.bat :app:assembleDebug --console=plain
```

## Ejecutar en Android Studio

1. Abrir proyecto.
2. Sincronizar Gradle.
3. Seleccionar dispositivo.
4. Run app.

