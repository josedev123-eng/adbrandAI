# CONTEXTO PARA LA IA — Proyecto AdBrand.AI

Copia este archivo completo al inicio de cada conversación con la IA (ChatGPT, Claude, Gemini, Copilot, Antigravity, etc.) antes de pedirle código.

## Qué es el proyecto

AdBrand.AI es una plataforma SaaS con IA para PYMEs. El dueño de la PYME registra su negocio y la IA le genera anuncios publicitarios y un kit de marca. Un equipo de administración modera ese contenido y gestiona suscripciones, tokens y el servidor.
Proyecto Integrador, curso Construcción y Pruebas de Software, Tecsup. Grupo 9: Jose Rojas, Karim Sovero, Edu Galindo.

## Arquitectura obligatoria (definida por el curso, NO cambiarla)

Dos lados que comparten **una sola base de datos PostgreSQL**:

```
ADMINISTRACIÓN   adbrand-admin-web (React + Vite)  ──►  adbrand-admin-api (Django + DRF)  ─┐
                                                                                            ├──► PostgreSQL
USUARIO          adbrand-web (React + Vite)       ─┐                                        │
                 adbrand-mobile (Kotlin + Compose) ┴►  adbrand-core-api (Spring Boot 3, Java 17) ┘
```

- La web de administración solo habla con Django.
- La web del usuario y la app móvil solo hablan con Spring Boot.
- Django y Spring Boot NO se llaman entre sí: se comunican a través de las tablas de la base de datos.
- Solo Spring Boot llama al servidor de IA del instituto.

## Dónde va cada historia de usuario

| Historias | Lado | Backend | Módulo |
|---|---|---|---|
| HU 1, 2, 3 | Administración | Django | `usuarios` |
| HU 4 | Administración | Django | `auditoria` |
| HU 5 a 8 | Administración | Django | `suscripciones` |
| HU 13 a 16 | Administración | Django | `moderacion` |
| HU 17, 19, 20 | Administración | Django | `tokens` |
| HU 18 | Administración | Django | `servidor` |
| HU 9, 23 | Usuario | Spring Boot | `negocio` |
| HU 10, 11, 12 | Usuario | Spring Boot | `contenido` (usa `ia`) |
| HU 21, 22, 24 | Usuario | Spring Boot | `auth` |
| HU 25 a 28 | Usuario | Spring Boot | `pago` |
| HU 29 a 32 | Usuario | Spring Boot | `brandkit` |
| HU 33 a 36 | Usuario | Spring Boot | `calendario` |
| HU 37 a 40 | Usuario | Spring Boot | `reporte` |
| HU 41 a 60 | Usuario (móvil) | Spring Boot | `adbrand-mobile/ui/...` y `notificacion` |

## Patrón de desarrollo: MVVM (obligatorio)

**Frontends React** (`adbrand-web` y `adbrand-admin-web`): `src/features/<modulo>/`
- `views/`: componentes que solo dibujan. Prohibido llamar a la API o poner reglas aquí.
- `viewmodels/`: hooks (ej. `usePerfilNegocio`) con estado, validaciones y acciones.
- `models/`: llamadas a la API del módulo usando el cliente único de `src/shared/services/`.

**Móvil Kotlin** (`adbrand-mobile`): `ui/<modulo>/screens/` (composables) y `ui/<modulo>/viewmodel/` (ViewModel); datos en `data/remote`, `data/repository` y `domain/model`.

**Spring Boot**: cada módulo en `com.adbrand.core.<modulo>` con `controller/` → `service/` → `repository/`, más `entity/` y `dto/`. El controller no tiene lógica; la lógica va en el service.

**Django**: cada app con `models.py`, `serializers.py`, `services.py` (lógica), `views.py`, `urls.py` y `tests/`.

Regla de oro: **una vista nunca llama a la API directamente**; siempre pasa por su ViewModel.

## Base de datos

- Una sola PostgreSQL para todo.
- Todas las tablas (también las de administración) se crean SOLO con migraciones Flyway en `adbrand-core-api/src/main/resources/db/migration/` con nombres `V<n>__descripcion.sql`.
- Nunca se edita una migración que ya se subió; se crea una nueva.
- En Django los modelos llevan `class Meta: managed = False` y `db_table = '<tabla>'`. No usar `makemigrations` para estas tablas.
- Nombres de tablas y columnas en español, minúsculas y con guion bajo (ej. `perfil_negocio`, `fecha_creacion`).
- Ejemplo de comunicación entre lados: Spring Boot guarda el anuncio en `contenido` con estado `PENDIENTE`; Django lo muestra en la bandeja del moderador y cambia el estado a `APROBADO` o `RECHAZADO`.

## Servidor de IA

- Servidor del instituto: `http://192.168.17.11:3000` (solo funciona dentro de la red de Tecsup).
- Nunca escribir la URL ni la clave en el código. Usar variables de entorno: `AI_BASE_URL`, `AI_API_KEY`, `AI_MODEL`, `AI_TIMEOUT_SECONDS`, `AI_CHAT_PATH`, `AI_MOCK`.
- Con `AI_MOCK=true` se devuelve una respuesta de prueba para poder trabajar fuera del instituto.
- Todo el código de IA vive en `adbrand-core-api/.../core/ia/`. Ningún otro módulo llama al servidor de IA directamente.

## Convenciones

- Errores de los dos backends con el mismo formato JSON: `{"codigo": "...", "mensaje": "..."}`.
- Inicio de sesión con JWT en los dos backends.
- Código (clases, métodos, variables) en inglés o español, pero siempre igual dentro del mismo módulo; textos de pantalla en español.
- Una rama por historia: `hu-<numero>-<descripcion>` (ej. `hu-15-aprobar-rechazar`). Nadie sube directo a `main`.

## Lo que la IA NO debe hacer

- No crear carpetas nuevas en la raíz ni cambiar la estructura existente (ver `ESTRUCTURA.md`).
- No cambiar el stack (nada de Node/Express, Firebase, MongoDB, Next.js, etc.).
- No hacer que Django llame a Spring Boot ni al revés.
- No crear tablas fuera de Flyway.
- No poner llamadas a la API dentro de `views/` o `screens/`.
- No tocar módulos de otra historia que no sea la pedida.

## Cómo pedirle código a la IA

1. Pega este archivo.
2. Di la historia exacta, por ejemplo: "Implementa la HU 14 (bandeja de revisión manual) en `adbrand-admin-api/moderacion` y `adbrand-admin-web/src/features/moderacion`".
3. Pide un solo backend o un solo frontend por conversación.
4. Revisa que los archivos que te dé caigan en las carpetas de la tabla de arriba.
