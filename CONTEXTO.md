# CONTEXTO PARA LA IA — Proyecto AdBrand.AI

Copia este archivo completo al inicio de cada conversación con la IA (ChatGPT, Claude, Gemini, Copilot, Antigravity, etc.) antes de pedirle código.

## Qué es el proyecto

AdBrand.AI es un sistema que utiliza IA para ayudar a las PYMEs a crear su identidad de marca y generar contenido publicitario, como logos, colores, publicaciones, guiones para reels y calendarios de contenido, de forma rápida y automatizada. Un equipo de administración modera ese contenido y gestiona suscripciones, tokens y el servidor.

Proyecto Integrador, curso Construcción y Pruebas de Software, Tecsup. Grupo 9: Jose Rojas, Karim Sovero, Edu Galindo.

## Arquitectura obligatoria (definida por el curso, NO cambiarla)

Dos lados que comparten **una sola base de datos PostgreSQL**:

```
ADMINISTRACIÓN   adbrand-admin-web (React + Vite, puerto 5174) ──► adbrand-admin-api (Django 5.2 + DRF, puerto 8000) ─┐
                                                                                                                       ├──► PostgreSQL 17 (base "adbrand")
USUARIO          adbrand-web (React + Vite, puerto 5173)       ─┐                                                      │
                 adbrand-mobile (Kotlin + Compose)              ┴► adbrand-core-api (Spring Boot 4.1.1, Java 17, puerto 8080) ┘
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
| HU 13 | Usuario (se ejecuta al generar) | Spring Boot | `revision` (filtro automático) |
| HU 14, 15, 16 | Administración | Django | `moderacion` |
| HU 17, 19, 20 | Administración | Django | `tokens` |
| HU 18 | Administración | Django | `servidor` |
| HU 9, 23 | Usuario | Spring Boot | `negocio` |
| HU 10, 12 | Usuario | Spring Boot | `contenido` (usa `ia` y `revision`) |
| HU 11 | Usuario | Spring Boot | `brandkit` (usa `ia`) |
| HU 21, 22, 24 | Usuario | Spring Boot | `auth` |
| HU 25 a 28 | Usuario | Spring Boot | `pago` |
| HU 29 a 32 | Usuario | Spring Boot | `brandkit` |
| HU 33 a 36 | Usuario | Spring Boot | `calendario` |
| HU 37 a 40 | Usuario | Spring Boot | `reporte` |
| HU 41 a 60 | Usuario (móvil) | Spring Boot | `adbrand-mobile/ui/...` y `notificacion` |

El filtro de la HU 13 está en Spring Boot porque el anuncio se revisa en el momento en que la IA lo genera, antes de devolverlo al usuario.

## Estado actual (lo que ya existe en `main`)

| HU | Qué hace | Dónde está |
|---|---|---|
| 1 | Crear administradores con rol | Django `usuarios` + admin-web `features/admin` |
| 2 | Modificar los permisos (el rol) de un administrador | Django `usuarios` + admin-web `features/admin` (entrada en el menú `Lista de administradores`; botón Editar permisos en cada fila) |
| 3 | Desactivar cuenta de administrador | Django `usuarios` + admin-web `features/admin` |
| 4 | Registro de auditoría de las acciones de cada administrador | Django `auditoria` + admin-web `features/auditoria` (entrada en el menú `Auditoría`; filtros por administrador y fechas) |
| 5 | Número de suscripciones activas | Django `suscripciones` + admin-web `features/suscripciones` (entrada en el menú `Panel financiero`; tarjeta Suscripciones activas) |
| 6 | Ingresos generados por mes | Django `suscripciones` (modelo `Pago`) + admin-web `features/suscripciones` (en el `Panel financiero`: tarjeta Ingresos del periodo y barras por mes) |
| 7 | Filtrar el panel financiero por rango de meses | Django `suscripciones` + admin-web `features/suscripciones` (en el `Panel financiero`: campos Desde y Hasta, botones Aplicar rango y Últimos 6 meses) |
| 8 | Suscripciones vencidas y pendientes de pago | Django `suscripciones` + admin-web `features/suscripciones` |
| 9 | Perfil del negocio | Spring `negocio` + web `features/negocio` |
| 10 | Generar anuncio con IA | Spring `contenido` e `ia` + web `features/contenido` |
| 11 | Solicitar Kit de Marca básico | Spring `brandkit` + web `features/brandkit` (entrada en el menú `Kit de Marca`; pantalla con Aceptar kit y Pedir otra versión) |
| 12 | Regenerar contenido que no convenció | Spring `contenido` + web `features/contenido` |
| 13 | Filtro automático del contenido | Spring `revision` + estado en `contenido` |
| 14 | Bandeja de revisión manual | Django `moderacion` + admin-web `features/moderacion` |
| 15 | Aprobar o rechazar contenido | Django `moderacion` + admin-web `features/moderacion` |
| 17 | Consumo de tokens de IA por empresa en el mes actual | Spring `ia` (guarda cada consumo) + Django `tokens` + admin-web `features/tokens` (entrada en el menú `Consumo de IA`; se actualiza sola cada 30 segundos) |
| 19 | Alerta cuando una empresa llega al porcentaje de aviso de su límite mensual de tokens | Django `tokens` (límite en `limite_tokens`, V31) + admin-web `features/tokens` (aviso arriba de la tabla de `Consumo de IA` y columna % del límite) |

Pendiente conocido:
- Spring todavía no tiene inicio de sesión: usa el usuario fijo `USUARIO_DE_PRUEBA = 1L` en los controllers hasta la HU 21. La web de administración ya pide login (ver "Inicio de sesión de administradores").

### Endpoints que ya existen

Spring Boot (`http://localhost:8080/api`):
- `GET /negocio/perfil` y `PUT /negocio/perfil`: perfil del negocio (HU 9).
- `POST /contenido/anuncios`: genera el anuncio, lo pasa por el filtro y lo guarda (HU 10 y 13). Responde `{id, texto, redSocial, tono, simulado, estado, motivoRevision}`.
- `POST /contenido/anuncios/{id}/regenerar`: regenera contenido con los mismos parámetros (HU 12). Responde igual que generar.
- `POST /kit-marca/generar`: genera Kit de Marca (logo, tipografías, paleta, voz) (HU 11).
- `GET /kit-marca/{id}` y `GET /kit-marca`: consulta kits (HU 11).

Django (`http://localhost:8000/api`):
- `POST /usuarios/login/` con `{"correo", "contrasena"}`: responde `{token, admin}`; 401 si los datos están mal y 403 si la cuenta está desactivada (login de administradores).
- `GET /usuarios/sesion/`: devuelve el administrador dueño del token; la web lo usa al recargar la página.
- `GET /usuarios/roles/` y `POST /usuarios/administradores/` (HU 1). Crear una cuenta queda en la auditoría como `CREAR_ADMIN`.
- `GET /usuarios/administradores/`: lista los administradores con su estado (HU 3).
- `PATCH /usuarios/administradores/<id>/desactivar/`: desactiva cuenta; solo el superadmin y nunca a sí mismo. Quién lo hace sale del token de sesión (HU 3).
- `PATCH /usuarios/administradores/<id>/rol/` con `{"rol_id": n}`: cambia el rol de un administrador; solo el superadmin y nunca a sí mismo; quién lo hace sale del token. Responde 403 si no es superadmin o es su propia cuenta, 400 si el rol no existe y 404 si el administrador no existe (HU 2).
- `GET /suscripciones/con-deuda/` con `?estado=VENCIDA` o `?estado=PENDIENTE_PAGO`: suscripciones vencidas y pendientes de pago, la más antigua primero (HU 8).
- `GET /suscripciones/activas/`: responde `{"total": n}` con las suscripciones en estado `ACTIVA` cuya `fecha_vencimiento` es hoy o posterior; una ACTIVA con la fecha pasada no cuenta, igual que en la HU 8 (HU 5).
- `GET /suscripciones/ingresos-mensuales/`: responde `[{"mes": "2026-09", "total": "150.00"}, ...]` con lo cobrado en los últimos 6 meses contando el actual, del más antiguo al más reciente. Suma `pago.monto` por mes de `fecha_pago`; un mes sin pagos sale con `"0.00"`. El ingreso es el pago recibido, no el monto de la suscripción (HU 6).
- `GET /suscripciones/ingresos-mensuales/?desde=AAAA-MM&hasta=AAAA-MM`: el mismo listado para ese rango de meses, los dos incluidos. Responde 400 si el formato es inválido, si falta uno de los dos, si `desde` es mayor que `hasta` o si el rango pasa de 24 meses (HU 7).
- `GET /moderacion/contenidos/dudosos/`: bandeja, solo estado `DUDOSO`, el más antiguo primero (HU 14).
- `GET /moderacion/contenidos/<id>/`: detalle completo con motivo y negocio (HU 14).
- `POST /moderacion/contenidos/<id>/moderar/`: aprueba o rechaza con `accion`; el moderador sale del token de sesión. Acepta `motivo_rechazo` opcional y lo guarda en `motivo_revision` (HU 15).
- `GET /auditoria/` con `?admin_id=`, `?desde=AAAA-MM-DD` y `?hasta=AAAA-MM-DD` (todos opcionales): historial de acciones, la más reciente primero, con el nombre y correo del administrador. Responde 400 si una fecha es inválida o `desde` es mayor que `hasta` (HU 4).
- `GET /tokens/consumo-actual/`: responde `{"mes": "2026-10", "total": n, "empresas": [{usuario_id, empresa, tokens, generaciones, ultima_generacion}]}` con los tokens de IA del mes actual (hora de Lima), de la empresa que más gasta a la que menos. La empresa es el `usuario_id`; el nombre sale de `perfil_negocio` o es "Empresa #id". Solo SUPERADMIN y TECNICO (HU 17).
  - HU 19: cada empresa trae además `limite`, `porcentaje_aviso`, `porcentaje` (1 decimal) y `alerta`, y la raíz trae `alertas: [{usuario_id, empresa, tokens, limite, porcentaje}]` con solo las empresas en alerta, de mayor a menor porcentaje. El límite es la fila propia de la empresa en `limite_tokens` o, si no tiene, la fila por defecto (`usuario_id` NULL). Hay alerta cuando `tokens * 100 >= limite * porcentaje_aviso` (en enteros: el 80 % exacto avisa). Sin ningún límite: `limite` y `porcentaje` en null y nunca hay alerta.
- Qué se registra en la auditoría: crear una cuenta (`CREAR_ADMIN`), desactivar una cuenta (`DESACTIVAR_ADMIN`), cambiar un rol (`CAMBIAR_ROL`) y moderar un contenido (`APROBAR_CONTENIDO`, `RECHAZAR_CONTENIDO`). Para registrar una acción nueva se llama a `auditoria.services.registrar_accion(admin_id, accion, entidad, entidad_id, detalle)`; nunca lanza errores, así que no rompe la acción original.

### Inicio de sesión de administradores

- La web de administración pide correo y contraseña (`features/auth`). `POST /api/usuarios/login/` devuelve un token firmado con `django.core.signing` que dura 8 horas; la web lo guarda (`shared/services/sesionToken.js`) y `apiClient.js` lo manda en cada pedido como `Authorization: Bearer <token>`. Si Django responde 401, la web vuelve al login con un aviso.
- Al recargar la página la web llama `GET /api/usuarios/sesion/` para saber quién está conectado y su rol (`app/context/SesionProvider.jsx`, `useSesion()`).
- Toda vista de Django hereda de `usuarios.vistas.VistaAdmin`: exige el token, que la cuenta esté ACTIVA y uno de los `roles_permitidos` de la vista. Quién hace la acción se toma de `request.user`; nunca se manda en el cuerpo.
- El menú de la web muestra solo las pantallas del rol (`app/routes/pantallasPorRol.js`), pero quien decide es Django.

| Rol | Puede usar |
|---|---|
| SUPERADMIN | Todo |
| MODERADOR | Moderación (bandeja, detalle, aprobar y rechazar) |
| FINANZAS | Suscripciones (deudas, activas e ingresos) |
| TECNICO | Consumo de IA (`/api/tokens/`, HU 17) y su sesión |

Respuestas: sin token, 403 "Inicia sesión para continuar."; rol que no corresponde, 403 "Tu rol no tiene acceso a esta sección."; token falso o vencido, "Tu sesión venció o no es válida. Vuelve a iniciar sesión.".

Primer superadministrador: en `adbrand-admin-api` correr `python manage.py crear_superadmin` (pide nombres, apellidos, correo y contraseña fuerte).

### Migraciones Flyway aplicadas

Cada integrante usa su rango de versiones: Karim V13 a V19, Edu V20 a V29, Jose V30 a V39. Por eso cada uno pone `spring.flyway.out-of-order=true` en su `adbrand-core-api/.env`, así Flyway aplica una versión menor que llega después de una mayor.

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
| V30 | `consumo_token` (tokens de entrada, salida y total de cada generación con IA, por `usuario_id` y `origen`; `simulado` si se estimaron) | 17 |
| V31 | `limite_tokens` (límite mensual y porcentaje de aviso; `usuario_id` NULL es el de por defecto: 100 000 tokens y aviso al 80 %; una fila por empresa) | 19 |

Los datos de prueba NO van en Flyway: están en `adbrand-docs/datos-prueba/` y se cargan a mano sobre la base local, una sola vez. `contenido-dudoso.sql` llena la bandeja de revisión y `suscripciones-y-pagos.sql` llena el panel financiero y la lista de deudas (8 suscripciones y sus pagos de los últimos 6 meses).

### Notas para las siguientes historias

- Autenticación de administradores: ver la sección "Inicio de sesión de administradores". Toda vista nueva de la web de administración hereda de `VistaAdmin` y declara sus `roles_permitidos`.
- El `Panel financiero` (`PanelFinancieroView` y `usePanelFinanciero` en `features/suscripciones`) reúne las HU 5, 6 y 7. Un indicador nuevo del dueño de la aplicación va ahí, con su endpoint en la app Django `suscripciones`.
- Las HU 25 a 28 (pagos del usuario, en Spring) deben escribir en la tabla `pago` (V10), que es de donde el panel saca los ingresos. Si necesitan más columnas, se agregan con una migración nueva; no se edita V10.

## Patrón de desarrollo: MVVM (obligatorio)

**Frontends React** (`adbrand-web` y `adbrand-admin-web`): `src/features/<modulo>/`
- `views/`: componentes que solo dibujan. Prohibido llamar a la API o poner reglas aquí.
- `viewmodels/`: hooks (ej. `usePerfilNegocio`) con estado, validaciones y acciones.
- `models/`: llamadas a la API del módulo usando el cliente único `src/shared/services/apiClient.js`.
- El menú de cada web está en `src/shared/components/` (`MenuUsuario`, `MenuAdmin`) y `App.jsx` decide qué pantalla mostrar.

**Móvil Kotlin** (`adbrand-mobile`): `ui/<modulo>/screens/` (composables) y `ui/<modulo>/viewmodel/` (ViewModel); datos en `data/remote`, `data/repository` y `domain/model`.

**Spring Boot**: cada módulo en `com.adbrand.core.<modulo>` con `controller/` → `service/` → `repository/`, más `entity/` y `dto/`. El controller no tiene lógica; la lógica va en el service.

**Django**: cada app con `models.py`, `serializers.py`, `services.py` (lógica), `views.py`, `urls.py` y `tests.py`. Las rutas de cada app se registran en `config/urls.py`.

Regla de oro: **una vista nunca llama a la API directamente**; siempre pasa por su ViewModel.

## Base de datos

- Una sola PostgreSQL (`adbrand`). Cada integrante tiene la suya en su PC; lo que se comparte son las migraciones en GitHub, no los datos.
- Todas las tablas (también las de administración) se crean SOLO con migraciones Flyway en `adbrand-core-api/src/main/resources/db/migration/` con nombres `V<n>__descripcion.sql`. Se aplican solas al arrancar Spring Boot.
- Nunca se edita una migración que ya se subió; se crea una nueva (por ejemplo con `ALTER TABLE`).
- Dos personas no pueden usar el mismo número de versión. Antes de crear una migración: `git pull` de `main` y avisar al grupo qué número se toma.
- En Django los modelos llevan `class Meta: managed = False` y `db_table = '<tabla>'`. No usar `makemigrations` ni `migrate` para estas tablas.
- Nombres de tablas y columnas en español, minúsculas y con guion bajo (ej. `perfil_negocio`, `fecha_creacion`).

### Cómo se comunican los dos lados

Spring Boot genera el anuncio, lo revisa con las reglas de `regla_revision` y lo guarda en `contenido`:
- `APROBADO`: pasó el filtro y el usuario lo puede copiar y publicar.
- `DUDOSO`: incumple alguna regla; no se publica y aparece en la bandeja de Django (HU 14).
- `RECHAZADO`: lo decide el moderador desde Django (HU 15).

## Servidor de IA

- Servidor del instituto: `http://192.168.17.11:3000` (solo funciona dentro de la red de Tecsup). Formato compatible con OpenAI, ruta `/api/chat/completions`.
- Nunca escribir la URL ni la clave en el código. Van en el archivo `.env` de `adbrand-core-api`: `AI_BASE_URL`, `AI_API_KEY`, `AI_MODEL`, `AI_TIMEOUT_SECONDS`, `AI_CHAT_PATH`, `AI_MOCK`.
- Modelo del instituto: `AI_MODEL=Qwen/Qwen3.6-35B-A3B-FP8` (nombre exacto, con `Qwen/`). Cada uno saca su propia clave y la pone solo en su `.env`.
- Con `AI_MOCK=true` se devuelve una respuesta de prueba para poder trabajar fuera del instituto (en casa). En Tecsup, `AI_MOCK=false`.
- Todo el código de IA vive en `adbrand-core-api/.../core/ia/`. Ningún otro módulo llama al servidor de IA directamente; usan `ServicioIa`.
- Qwen3 a veces escribe su razonamiento entre `<think>` y `</think>` antes de la respuesta. `ServicioIa` lo quita antes de devolver el texto, así que los módulos reciben solo la respuesta final.
- HU 17: toda generación que pide una empresa usa `ServicioIa.generarTexto(sistema, usuario, usuarioId, origen)`, que además guarda los tokens en `consumo_token` (V30). Un módulo nuevo que use la IA agrega su valor a `OrigenConsumo` y al CHECK de la tabla con una migración nueva. Con `AI_MOCK=true` o si el servidor no manda `usage`, los tokens se estiman (4 caracteres = 1 token) y se marcan `simulado`.

## Configuración local y cómo arrancar

Los archivos `.env` NO se suben a GitHub (están en `.gitignore`). Cada uno crea los suyos:
- `adbrand-core-api/.env`: `DB_PASSWORD` y las variables `AI_*`.
- `adbrand-admin-api/.env`: `DB_PASSWORD`.

| Parte | Carpeta | Comando |
|---|---|---|
| Spring Boot | `adbrand-core-api` | `.\mvnw spring-boot:run` |
| Django | `adbrand-admin-api` | `venv\Scripts\activate` y luego `python manage.py runserver 8000` |
| Web del usuario | `adbrand-web` | `npm install` (la primera vez) y `npm run dev` |
| Web de administración | `adbrand-admin-web` | `npm install` (la primera vez) y `npm run dev` |

Puertos: Spring 8080, Django 8000, Web usuario 5173, Admin web 5174 (o 5176 si ocupado).

## Pruebas

| Parte | Herramientas | Comando |
|---|---|---|
| Spring Boot | JUnit 6, AssertJ, Mockito, `@WebMvcTest` | `.\mvnw test` (o `.\mvnw test -Dtest="NombreDeLaPrueba"`) |
| Django | `APITestCase` de DRF | `python manage.py test <app>` |
| Web de administración | Vitest y Testing Library | `npm test` |
| Las dos webs | ESLint | `npm run lint` |

- En Spring, las pruebas de un módulo van en `src/test/java/com/adbrand/core/<modulo>/`. Usan Mockito para no depender de la base de datos ni de la IA.
- En Django, el runner `core/test_runner.py` crea las tablas de Flyway en la base temporal de pruebas, así que las pruebas no necesitan Spring.
- Como los endpoints de Django piden token, las pruebas usan `core/pruebas.py`: `crear_admin("ROL")` crea un administrador activo, `con_sesion(admin)` da el header para un pedido e `iniciar_sesion(self.client, admin)` lo deja puesto para todos los pedidos.
- En React, las pruebas reemplazan el `models/...Api.js` con datos de prueba (`vi.mock`), así no necesitan el backend encendido.

## Convenciones

- Errores de Spring Boot: `{"codigo": "...", "mensaje": "...", "campos": {"campo": "mensaje"}}` (ver `shared/error/ManejadorDeErrores`). Códigos usados: `DATOS_INVALIDOS` (400), `PERFIL_NO_ENCONTRADO` (404), `IA_NO_DISPONIBLE` (503).
- Errores de Django: por ahora el formato de DRF, `{"campo": ["mensaje"]}`, y 404 con `{"detail": "..."}`.
- Código (clases, métodos, variables) en español; textos de pantalla en español.
- Cada commit con el formato `HU-<número>: descripción corta`, por ejemplo `HU-14: bandeja de contenidos dudosos`.

### Ramas

- Cada integrante trabaja en su rama: `rama-jose`, `rama-karim`, `rama-edu`.
- Cuando termina sus historias, abre un PR de su rama a `main`. Nadie sube directo a `main`.
- Antes de empezar algo nuevo: `git pull origin main` en su rama, para trabajar sobre lo último.

## Estilo visual (las dos webs)

- Los colores y letras están como variables en `src/index.css` de cada web. Úsalas siempre (`var(--primary)`, etc.) en lugar de colores sueltos.
- Fondo claro `--bg` #f4f4f9 y tarjetas blancas `--surface`.
- `--primary` #4f46e5 (índigo) para botones y menú.
- `--ia` #b5179e (fucsia) solo para lo que hace la IA: botón "Generar con IA" y etiqueta "Generado con IA".
- Estados: `--ok` verde, `--warn` ámbar (dudoso, en revisión), `--bad` rojo (error, motivo de la observación).
- Letras: Bricolage Grotesque para títulos, Plus Jakarta Sans para el texto y JetBrains Mono para códigos.
- Clases comunes: `.pagina`, `.tarjeta`, `.campo`, `.boton-primario`, `.boton-secundario`, `.aviso`.

## Lo que la IA NO debe hacer

- No crear carpetas nuevas en la raíz ni cambiar la estructura existente (ver `ESTRUCTURA.md`).
- No cambiar el stack ni las versiones (nada de Node/Express, Firebase, MongoDB, Next.js, Spring Boot 3, etc.).
- No hacer que Django llame a Spring Boot ni al revés.
- No crear tablas fuera de Flyway ni editar las migraciones ya subidas (V1 a V12, V30 y V31). Cada uno usa su rango (Karim V13-V19, Edu V20-V29, Jose V30-V39).
- No borrar ni reemplazar pruebas de otros integrantes; solo agregar las nuevas.
- No poner llamadas a la API dentro de `views/` o `screens/`.
- No escribir contraseñas, la URL de la IA ni su clave en el código.
- No tocar módulos de otra historia que no sea la pedida.

## Cómo pedirle código a la IA

1. Pega este archivo.
2. Di la historia exacta, por ejemplo: "Implementa la HU 15 (aprobar o rechazar contenido) en `adbrand-admin-api/moderacion` y `adbrand-admin-web/src/features/moderacion`, sobre lo que ya hizo la HU 14".
3. Pide un solo backend o un solo frontend por conversación.
4. Revisa que los archivos que te dé caigan en las carpetas de la tabla de arriba y que incluya sus pruebas.