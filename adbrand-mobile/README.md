# adbrand-mobile

App móvil del usuario PYME de AdBrand.AI, hecha con Kotlin y Jetpack Compose. Usa los mismos endpoints de Spring Boot (`adbrand-core-api`) que la web, así la cuenta es la misma en los dos lados.

## Estructura (MVVM)

```
app/src/main/java/com/adbrand/
├── core/
│   ├── network/      ClienteApi (Singleton): un solo cliente HTTP; renueva la sesión ante un 401
│   ├── session/      GestorSesion (Singleton): guarda los tokens cifrados en el celular
│   └── theme/        Colores y tipografía de la app
├── data/
│   ├── remote/       Llamadas a la API de Spring Boot
│   └── repository/   Repositorios que usan los ViewModel
├── domain/model/     Modelos de la app (Usuario, Empresa, PerfilNegocio, Previsualizacion)
└── ui/               Cada módulo con screens/ (Compose) y viewmodel/
    ├── auth/              HU 42 y 43: inicio de sesión y renovación de la sesión
    ├── perfil/            HU 41 y 44: datos de la empresa y perfil del negocio
    └── previsualizacion/  HU 45: publicaciones con el kit de marca
```

Ninguna pantalla llama a la API directamente; siempre pasa por su ViewModel. La contraseña nunca se guarda en el celular.

## Cómo ejecutarla

1. Arrancar `adbrand-core-api`.
2. Abrir esta carpeta en Android Studio y esperar el Gradle Sync.
3. Ejecutarla en un emulador con Android 8.0 o superior. Desde el emulador, Spring Boot está en `http://10.0.2.2:8080`.

## Comandos

```bash
.\gradlew.bat :app:testDebugUnitTest   # pruebas
.\gradlew.bat :app:assembleDebug       # genera el APK
```
