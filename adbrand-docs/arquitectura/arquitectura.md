# Arquitectura de AdBrand.AI

## Vista general

```
ADMINISTRACIÓN   adbrand-admin-web (React, :5174) ──► adbrand-admin-api (Django, :8000) ──┐
                                                                                           ├──► PostgreSQL ("adbrand")
USUARIO          adbrand-web (React, :5173)  ──┐                                           │
                 adbrand-mobile (Kotlin)     ──┴──► adbrand-core-api (Spring Boot, :8080) ─┘
                                                        └──► Servidor de IA de Tecsup
```

El sistema tiene dos lados, cada uno con su backend, y los dos comparten una sola base de datos PostgreSQL.

| Lado | Quién lo usa | Frontend | Backend |
|---|---|---|---|
| Usuario | El dueño de la PYME | `adbrand-web` (React) y `adbrand-mobile` (Kotlin) | `adbrand-core-api` (Spring Boot) |
| Administración | Superadmin, moderadores, finanzas y técnico | `adbrand-admin-web` (React) | `adbrand-admin-api` (Django) |

## Reglas de comunicación

- La web de administración solo habla con Django.
- La web del usuario y la app móvil solo hablan con Spring Boot.
- Django y Spring Boot no se llaman entre sí; se comunican a través de las tablas. La única excepción es la HU 18: Django consulta `GET /api/estado` de Spring Boot para mostrar el estado del servidor.
- Solo Spring Boot llama al servidor de IA del instituto, siempre a través de `ServicioIa`, que además registra los tokens consumidos.

## Por qué dos backends

La arquitectura la define el curso: un backend en Java (Spring Boot) para el usuario y otro en Python (Django) para la administración. Cada uno tiene su propio inicio de sesión:

- **Usuario PYME:** JWT firmado por Spring Boot, que dura 8 horas. La app móvil, además, usa un token de renovación (HU 43).
- **Administrador:** token firmado por Django, que dura 8 horas, con permisos por rol.

## Base de datos compartida

- Todas las tablas, también las de administración, las crea Spring Boot con migraciones Flyway (`adbrand-core-api/src/main/resources/db/migration`).
- En Django los modelos llevan `managed = False`: Django lee y escribe las tablas, pero no las crea.
- Ejemplo de comunicación por tablas: Spring Boot genera un anuncio, lo revisa con las reglas de `regla_revision` (HU 13) y lo guarda en `contenido` como `APROBADO` o `DUDOSO`. El moderador ve los dudosos en la bandeja de Django (HU 14) y los aprueba o rechaza (HU 15).

El detalle de las tablas está en [`../modelo-datos/tablas.md`](../modelo-datos/tablas.md).

## Patrón de cada parte

| Parte | Patrón | Capas |
|---|---|---|
| Spring Boot | Módulos por funcionalidad | `controller` → `service` → `repository`, más `entity` y `dto` |
| Django | Una app por tema | `models` → `serializers` → `services` → `views` → `urls` |
| Webs React | MVVM | `views` (dibuja) → `viewmodels` (estado y validaciones) → `models` (API) |
| App Kotlin | MVVM | `screens` (Compose) → `viewmodel` → `repository` → `remote` (API) |

En las webs y en el móvil, una pantalla nunca llama a la API directamente: siempre pasa por su ViewModel.

## Seguridad

- Las contraseñas se guardan solo con hash: BCrypt en Spring Boot y el hasher de Django en administración.
- La empresa y el administrador salen siempre del token, nunca de lo que manda el cliente.
- Las claves (base de datos, JWT, IA y correo) van en el `.env` de cada backend, que no se sube al repositorio. Ver los `.env.example`.

La estructura de carpetas completa está en [`estructura.md`](estructura.md).
