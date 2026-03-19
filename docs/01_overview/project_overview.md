# Project Overview

## Que es la app

EsteticaApp es una aplicacion Android para centros de estetica enfocada en gestion de citas, relacion con clientes y analisis de mirada con IA.

## Problema que resuelve

- Reduce friccion para reservar y gestionar citas.
- Evita sobrecupos con reglas de capacidad por horario.
- Mejora la toma de decisiones con panel administrativo y notificaciones en tiempo real.
- Aporta diferenciacion del servicio con recomendaciones asistidas por IA.

## Publico objetivo

- Clientes finales que desean reservar servicios de mirada.
- Administradores del negocio (dueno/a o recepcion) que gestionan agenda y galeria.

## Features principales

- Autenticacion con email/password y Google.
- Registro de perfil de cliente.
- Agenda de citas con cancelaciones, estado y reseñas.
- Panel admin para confirmar/rechazar/completar citas.
- Galeria de servicios para cliente y admin.
- Diagnostico de mirada con CameraX + ML Kit + Gemini.
- Notificaciones en segundo plano para admin y cliente.

## Stack tecnologico

- Kotlin + Jetpack Compose (Material 3)
- Arquitectura MVVM por feature
- Firebase Auth, Firestore y Realtime Database
- Firebase AI (Gemini), ML Kit Face Detection
- CameraX
- Coil y Cloudinary

## Estado actual

- `minSdk`: 24
- `targetSdk`: 36
- `compileSdk`: 36
- Build principal validado con `:app:assembleDebug`

