# AdBrand.AI

Plataforma que ayuda a las PYMEs a crear su identidad de marca y su contenido publicitario con inteligencia artificial: kit de marca (logo, colores, tipografías y voz), anuncios para Instagram y Facebook, calendario de publicaciones e informes. Un equipo de administración modera el contenido generado y gestiona suscripciones, consumo de IA y el estado del servidor.

Proyecto Integrador de Tecsup, 4.° ciclo. Grupo 9: Jose Rojas, Karim Sovero y Edu Galindo.

## Arquitectura

```
ADMINISTRACIÓN   adbrand-admin-web (React, :5174) ──► adbrand-admin-api (Django, :8000) ──┐
                                                                                           ├──► PostgreSQL ("adbrand")
USUARIO          adbrand-web (React, :5173)  ──┐                                           │
                 adbrand-mobile (Kotlin)     ──┴──► adbrand-core-api (Spring Boot, :8080) ─┘
                                                        └──► Servidor de IA de Tecsup
```

- La web de administración solo habla con Django; la web del usuario y la app móvil, solo con Spring Boot.
- Los dos backends comparten la base de datos y se comunican a través de sus tablas.
- Todas las tablas se crean con migraciones Flyway en Spring Boot.

Más detalle en [`adbrand-docs/arquitectura/arquitectura.md`](adbrand-docs/arquitectura/arquitectura.md).

## Tecnologías

| Parte | Tecnología |
|---|---|
| Backend del usuario | Java 17, Spring Boot 4.1.1, Spring Data JPA, Flyway, JWT, OpenPDF, Apache POI |
| Backend de administración | Python, Django 5.2, Django REST Framework |
| Webs (usuario y administración) | React 19, Vite, Vitest, ESLint |
| App móvil | Kotlin, Jetpack Compose, Retrofit, OkHttp |
| Base de datos | PostgreSQL 17 |

## Estructura

```
adbrandAI/
├── adbrand-core-api/    Spring Boot: backend del usuario PYME
├── adbrand-admin-api/   Django: backend de administración
├── adbrand-web/         React: web del usuario PYME
├── adbrand-admin-web/   React: web de administración
├── adbrand-mobile/      Kotlin: app móvil del usuario PYME
├── adbrand-docs/        Documentación (arquitectura, API, modelo de datos, sprints, guía de desarrollo)
└── scripts/             Scripts que se ejecutan a mano (datos de prueba)
```

Cada aplicación tiene su propio README con el detalle de sus carpetas.

## Requisitos

| Herramienta | Versión |
|---|---|
| JDK | 17 o superior |
| Python | 3.12 o superior |
| Node.js | 20 o superior |
| PostgreSQL | 17, con una base vacía llamada `adbrand` |
| Android Studio | Para la app móvil (emulador Android 8.0 o superior) |

## Cómo arrancar

1. Crear la base `adbrand` en PostgreSQL.
2. Copiar `.env.example` a `.env` en `adbrand-core-api` y en `adbrand-admin-api`, y completar los valores.
3. Arrancar Spring Boot primero, porque sus migraciones crean las tablas:
   ```bash
   cd adbrand-core-api
   .\mvnw spring-boot:run
   ```
4. Arrancar Django:
   ```bash
   cd adbrand-admin-api
   python -m venv venv
   venv\Scripts\pip install -r requirements.txt
   venv\Scripts\python manage.py runserver 8000
   ```
5. Arrancar las webs, cada una en su terminal:
   ```bash
   cd adbrand-web
   npm install
   npm run dev
   ```
   ```bash
   cd adbrand-admin-web
   npm install
   npm run dev -- --port 5174
   ```
6. App móvil: abrir `adbrand-mobile` en Android Studio y ejecutarla en el emulador. Llama a Spring Boot en `http://10.0.2.2:8080`.

Para crear el primer administrador: `venv\Scripts\python manage.py crear_superadmin` dentro de `adbrand-admin-api`.

## Pruebas

| Parte | Comando |
|---|---|
| Spring Boot | `.\mvnw test` en `adbrand-core-api` |
| Django | `venv\Scripts\python manage.py test` en `adbrand-admin-api` |
| Webs | `npm test` y `npm run lint` en `adbrand-web` y en `adbrand-admin-web` |
| App móvil | `.\gradlew.bat :app:testDebugUnitTest` en `adbrand-mobile` |

Las pruebas de Spring que pasan por el inicio de sesión usan la base local; cada prueba deshace sus cambios al terminar.

## Estado

| Sprint | Historias |
|---|---|
| Sprint 1 | HU 1 a 15: administradores, auditoría, panel financiero, perfil del negocio, anuncios con IA, kit de marca, filtro y moderación |
| Sprint 2 | HU 16 a 28, 32 y 33: historial de moderación, tokens de IA, estado del servidor, cuenta del usuario PYME, pagos, edición del kit y calendario |
| Sprint 3 | HU 29 a 31 y 34 a 45: personalización del kit, campañas, programación, informes en PDF y Word, y app móvil |

El detalle de cada historia, los endpoints y las migraciones está en la [guía de desarrollo](adbrand-docs/guia-desarrollo.md).

## Forma de trabajo

- Cada integrante trabaja en su rama (`rama-jose`, `rama-karim`, `rama-edu`) y la une a `main` con un pull request.
- Cada commit lleva el formato `HU-<número>: descripción`.
- Las reglas completas están en [`adbrand-docs/guia-desarrollo.md`](adbrand-docs/guia-desarrollo.md).
