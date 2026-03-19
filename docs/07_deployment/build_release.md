# Build and Release

## Build debug

```powershell
.\gradlew.bat :app:assembleDebug --console=plain
```

APK esperado:

- `app/build/outputs/apk/debug/app-debug.apk`

## Build release (AAB)

```powershell
.\gradlew.bat :app:bundleRelease --console=plain
```

AAB esperado:

- `app/build/outputs/bundle/release/app-release.aab`

## Firma

Estado actual observado en `app/build.gradle.kts`:

- `release` usa `signingConfigs.getByName("debug")`.

Recomendado para produccion:

1. Crear keystore dedicada.
2. Configurar `signingConfigs.release`.
3. Mover passwords a variables/keystore seguro.

## Checklist pre-release

- Build release sin errores.
- Probar login, agenda, admin y notificaciones.
- Validar reglas Firebase en entorno productivo.
- Verificar politicas de permisos y privacidad.

## Publicacion Play Store (si aplica)

1. Generar AAB firmado.
2. Subir a Play Console (internal test).
3. Ejecutar smoke test con cuentas admin/cliente.
4. Promover a produccion.

