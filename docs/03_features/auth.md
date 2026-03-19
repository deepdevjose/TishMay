# Feature: Auth

## Que hace

Gestiona autenticacion, registro de perfil y recuperacion de cuenta.

## Flujos principales

1. Login con email/password.
2. Login con Google (`CredentialManager`).
3. Registro de perfil inicial.
4. Verificacion de correo.
5. Recuperacion de contrasena.

## Pantallas involucradas

- `LoginScreen`
- `ProfileRegistrationScreen`
- `EmailVerificationScreen`
- `ForgotPasswordScreen`

## Reglas de negocio

- Si usuario no existe en `clientes`, va a registro.
- Si correo pertenece a admin (`administradores/{email}`), redirige a flujo admin.
- Verificacion de correo condiciona acceso completo en usuario cliente.

