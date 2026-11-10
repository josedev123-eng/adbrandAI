# Modelo de datos


- Una sola PostgreSQL (`adbrand`). Cada integrante tiene la suya en su PC; lo que se comparte son las migraciones en GitHub, no los datos.
- Todas las tablas (también las de administración) se crean SOLO con migraciones Flyway en `adbrand-core-api/src/main/resources/db/migration/` con nombres `V<n>__descripcion.sql`. Se aplican solas al arrancar Spring Boot.
- Nunca se edita una migración que ya se subió; se crea una nueva (por ejemplo con `ALTER TABLE`).
- Dos personas no pueden usar el mismo número de versión. Antes de crear una migración: `git pull` de `main` y avisar al grupo qué número se toma.
- En Django los modelos llevan `class Meta: managed = False` y `db_table = '<tabla>'`. No usar `makemigrations` ni `migrate` para estas tablas.
- Nombres de tablas y columnas en español, minúsculas y con guion bajo (ej. `perfil_negocio`, `fecha_creacion`).

## Cómo se comunican los dos lados

Spring Boot genera el anuncio, lo revisa con las reglas de `regla_revision` y lo guarda en `contenido`:
- `APROBADO`: pasó el filtro y el usuario lo puede copiar y publicar.
- `DUDOSO`: incumple alguna regla; no se publica y aparece en la bandeja de Django (HU 14).
- `RECHAZADO`: lo decide el moderador desde Django (HU 15).


## Migraciones Flyway

Cada integrante usa su rango de versiones: Karim V13 a V19, Edu V20 a V29, Jose V30 a V39. Por eso cada uno pone `spring.flyway.out-of-order=true` en su `adbrand-core-api/.env`, así Flyway aplica una versión menor que llega después de una mayor. V12 es del login de administradores. En el Sprint 3, V18 quedó reservada para `token_renovacion` (HU 43).

| Versión | Tablas | HU |
|---|---|---|
| V1 | `rol` (SUPERADMIN, MODERADOR, FINANZAS) y `usuario_admin` | 1 |
| V2 | `perfil_negocio` (un perfil por `usuario_id`; tono CERCANO, PROFESIONAL, DIVERTIDO o ELEGANTE) | 9 |
| V3 | `regla_revision` (término, categoría, motivo, activa; 10 reglas iniciales) y `contenido` (cada anuncio generado con `estado` y `motivo_revision`) | 13 |
| V4 | `contenido.moderador_id` (quién aprobó/rechazó) | 15 |
| V5 | `contenido.prompt_original` (para regenerar con HU 12) | 12 |
| V6 | `usuario_admin.estado` (ACTIVO/INACTIVO) | 3 |
| V7 | `suscripcion` (estado, fechas, monto y plan de cada cliente) | 8 |
| V8 | `kit_marca` (logo, tipografías, paleta, voz) | 11 |
| V9 | `auditoria_admin` (qué administrador hizo qué acción, sobre qué y cuándo) | 4 |
| V10 | `pago` (monto, fecha y método TARJETA o YAPE de cada pago de una suscripción) | 6 |
| V11 | `auditoria_admin.fecha` con zona horaria (TIMESTAMPTZ) | 4 |
| V12 | rol `TECNICO` (consumo de tokens de IA y estado del servidor) | 17 |
| V13 | `usuario` (cuenta del usuario PYME: nombre, empresa, correo único, hash BCrypt, estado ACTIVO/INACTIVO) | 21 |
| V14 | `usuario.ultimo_acceso` | 22 |
| V15 | `codigo_recuperacion` (hash del código, vencimiento y si ya se usó) | 24 |
| V16 | `usuario.ruc` (único), `razon_social`, `telefono` y `direccion` | 23 |
| V17 | `kit_marca_version` (cada edición del kit de marca) | 32 |
| V18 | `token_renovacion` (`usuario_id`, `token_hash`, `fecha_vencimiento`, `revocado`, `fecha_creacion`; solo se guarda el hash SHA-256) | 43 |
| V20 | `plan` (planes de suscripción Básico, Pro y Premium) | 25 |
| V21 | `pago`: datos de la operación y del plan pagado | 26 |
| V22 | `operacion_yape` (pago con Yape en dos pasos) | 27 |
| V23 | `pago.numero_comprobante` | 28 |
| V24 | `publicacion_programada` (publicaciones del calendario) | 33 |
| V25 | `campana` (nombre, color `#RRGGBB`, tipo CAMPANA o EVENTO, fecha de inicio y de fin) y `publicacion_programada.campana_id` | 35 |
| V26 | `publicacion_programada.contenido_id` (el anuncio que se va a publicar) | 34 |
| V30 | `consumo_token` (tokens de entrada, salida y total de cada generación con IA, por `usuario_id` y `origen`; `simulado` si se estimaron) | 17 |
| V31 | `limite_tokens` (límite mensual y porcentaje de aviso; `usuario_id` NULL es el de por defecto: 100 000 tokens y aviso al 80 %; una fila por empresa) | 19 |
| V32 | vista `vista_consumo_token_mensual` (tokens y generaciones por mes, con la hora de Lima, y por `usuario_id`; solo lectura) | 20 |
| V33 | `verificacion_servidor` (servicio BACKEND, BASE_DATOS o IA; estado ACTIVO o CAIDO; tiempo_ms, detalle y fecha de cada verificación) | 18 |
| V34 | vista `vista_historial_contenido` (contenidos APROBADO o RECHAZADO con el nombre del moderador o "Filtro automático", el motivo y la fecha de la decisión; solo lectura) | 16 |
| V35 | índice de `contenido` por `usuario_id` y `fecha_creacion` (para el informe de campañas) | 37 |
| V36 | `logo_diseno` (TEXT con el JSON del logo) en `kit_marca` y `kit_marca_version` | 29 |
| V37 | `kit_marca_version.voz_marca` | 31 |

Si la base local ya tenía una versión mayor cuando llegaron V13 a V19 o V30 a V37, Spring necesita `spring.flyway.out-of-order=true` en el `.env` para aplicarlas.

Los datos de prueba NO van en Flyway: están en `scripts/datos-prueba/` y se cargan a mano sobre la base local, una sola vez. `contenido-dudoso.sql` llena la bandeja de revisión, `suscripciones-y-pagos.sql` llena el panel financiero y la lista de deudas (8 suscripciones y sus pagos de los últimos 6 meses) y `kit-marca.sql` deja un kit listo para editar (HU 32; cambiar el correo por el de tu cuenta).

