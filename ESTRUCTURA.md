# AdBrand.AI: estructura del proyecto

Sigue la arquitectura del curso (4to ciclo): dos lados que comparten **una sola base de datos PostgreSQL**.

```
ADMINISTRACIÓN   Front End Web (React)  ──►  Back End (Django)     ─┐
                                                                    ├──►  BD (PostgreSQL)
USUARIO          Front End Web (React)  ─┐                          │
                 Front End Móvil (Kotlin)┴►  Back End (Spring Boot) ┘
```

Un solo repositorio con seis carpetas. Cada carpeta vacía tiene un archivo `.gitkeep` para que Git la suba; se puede borrar cuando la carpeta tenga archivos.

```
adbrand/
├── adbrand-docs/        Documentación: arquitectura, contratos de API, modelo de datos, sprints
├── adbrand-admin-web/   ADMINISTRACIÓN. React (Vite): panel del superadmin y moderadores
├── adbrand-admin-api/   ADMINISTRACIÓN. Django + Django REST Framework
├── adbrand-web/         USUARIO. React (Vite): web del dueño de la PYME
├── adbrand-mobile/      USUARIO. Kotlin + Jetpack Compose (más adelante)
└── adbrand-core-api/    USUARIO. Spring Boot (Java)
```

## Qué historia va en qué lado

| Lado | Historias |
|---|---|
| Administración (React + Django) | HU 1 a 4 administradores y auditoría, HU 5 a 8 suscripciones e ingresos, HU 14 a 16 moderación, HU 17 a 20 tokens y servidor |
| Usuario (React + Kotlin + Spring Boot) | HU 9 a 12 negocio y generación con IA, HU 13 filtro automático, HU 21 a 24 cuenta, HU 25 a 28 pagos, HU 29 a 32 kit de marca, HU 33 a 36 calendario, HU 37 a 40 reportes, HU 41 a 60 app móvil |

## adbrand-admin-api (Django) — ADMINISTRACIÓN

```
config/          Proyecto Django: settings, urls, conexión a la BD compartida
core/            Inicio de sesión de administradores, permisos por rol, formato de error {codigo, mensaje}
usuarios/        HU 1, 2, 3: cuentas de administrador, roles y permisos
auditoria/       HU 4: registro de acciones de administradores
suscripciones/   HU 5, 6, 7, 8: suscripciones activas, ingresos, filtros, morosos
moderacion/      HU 14 a 16: bandeja, aprobar o rechazar, historial (el filtro de la HU 13 está en Spring Boot)
tokens/          HU 17, 19, 20: consumo de tokens y alertas
servidor/        HU 18: estado del servidor
```

Cada app de Django se crea con `python manage.py startapp <nombre>` y usa siempre los mismos archivos:
`models.py` (tablas) → `serializers.py` (lo que entra y sale) → `services.py` (reglas) → `views.py` (endpoints) → `urls.py`, y `tests.py`.

## adbrand-admin-web (React) — ADMINISTRACIÓN, MVVM

```
src/
├── app/
│   ├── routes/        Rutas y protección por rol (superadmin, moderador)
│   └── context/       Sesión del administrador
├── shared/
│   ├── components/    Botones, tablas, menú lateral y estructura de página
│   ├── services/      Cliente HTTP hacia Django (uno solo)
│   └── hooks/
├── assets/
└── features/          Cada módulo con views/, viewmodels/ y models/
    ├── auth/            Inicio de sesión del administrador
    ├── admin/           HU 1, 2, 3, 4
    ├── suscripciones/   HU 5, 6, 7, 8
    ├── moderacion/      HU 14 a 16
    ├── tokens/          HU 17, 19, 20
    └── servidor/        HU 18
```

## adbrand-core-api (Spring Boot) — USUARIO

```
src/main/java/com/adbrand/core/
├── config/          CORS, seguridad con JWT, variables del servidor de IA
├── shared/
│   ├── error/       Formato de error {codigo, mensaje, campos}
│   └── util/
├── ia/              Cliente hacia el servidor de IA del instituto (client/, service/, dto/) y modo simulado
├── auth/            HU 21, 22, 24: registro, inicio de sesión, recuperar contraseña
├── negocio/         HU 9, 23: perfil del negocio y datos de la empresa
├── contenido/       HU 10, 11, 12: anuncio, kit de marca, regenerar
├── revision/        HU 13: filtro automático del contenido generado (reglas en la tabla regla_revision)
├── pago/            HU 25 a 28: planes, tarjeta y Yape, confirmación
├── brandkit/        HU 29 a 32: logo, colores e identidad visual
├── calendario/      HU 33 a 36: calendario de publicaciones
├── reporte/         HU 37 a 40: informes en PDF y Word
└── notificacion/    HU 49 a 52: alertas push
src/main/resources/db/migration/   Migraciones Flyway de TODA la base de datos
src/test/java/com/adbrand/core/    Pruebas, una carpeta por módulo
```

Cada módulo tiene `controller/` → `service/` → `repository/`, más `entity/` (tablas) y `dto/`.

## adbrand-web (React) — USUARIO, MVVM

```
src/
├── app/{routes, context}
├── shared/{components, services, hooks}   services/ = cliente HTTP hacia Spring Boot
├── assets/
└── features/
    ├── auth/         HU 21, 22, 24
    ├── negocio/      HU 9, 23
    ├── contenido/    HU 10, 11, 12
    ├── pagos/        HU 25 a 28
    ├── brandkit/     HU 29 a 32
    ├── calendario/   HU 33 a 36
    └── reportes/     HU 37 a 40
```

## adbrand-mobile (Kotlin + Jetpack Compose) — USUARIO, MVVM

```
app/src/main/java/com/adbrand/
├── core/{network, theme}        network/ = cliente HTTP hacia Spring Boot
├── data/{remote, repository}
├── domain/model/
├── notifications/               HU 49 a 52
└── ui/                          Cada módulo con screens/ y viewmodel/
    ├── auth/              HU 41 a 43
    ├── perfil/            HU 44
    ├── previsualizacion/  HU 45 a 48
    ├── aprobacion/        HU 53 a 56
    ├── metricas/          HU 57 a 60
    └── calendario/
```

La app móvil usa los mismos endpoints de `adbrand-core-api`.

## adbrand-docs

```
arquitectura/    Diagrama del curso y decisiones
api/             Contratos de Django y de Spring Boot
modelo-datos/    Tablas y relaciones de la BD compartida
sprints/         Backlog, Sprint Backlog y laboratorios
```

## Patrón de desarrollo: MVVM

| Capa | Qué hace | Web (React, las dos) | Móvil (Kotlin) | Backend |
|---|---|---|---|---|
| View | Dibuja la pantalla y recibe acciones. Sin lógica. | `features/*/views/` | `ui/*/screens/` | — |
| ViewModel | Estado, validaciones y acciones. No dibuja. | `features/*/viewmodels/` | `ui/*/viewmodel/` | — |
| Model | Datos y reglas del negocio. | `features/*/models/` → API | `data/` y `domain/` → API | Django y Spring Boot |

Regla de oro: **una View nunca llama a la API directamente**; siempre pasa por su ViewModel.

## Una sola base de datos, un solo dueño de las tablas

Los dos backends usan la misma BD. Para que no se pisen:

- Todas las tablas se crean con migraciones Flyway en `adbrand-core-api/src/main/resources/db/migration` (`V1__...sql`, `V2__...sql`), también las del lado de administración.
- En Django los modelos llevan `managed = False` y no se usa `makemigrations` para esas tablas.
- Cómo se comunican los dos lados: por las tablas. Ejemplo: Spring Boot genera el anuncio, lo revisa con las reglas de `regla_revision` (HU 13) y lo guarda en `contenido` como `APROBADO` o `DUDOSO`; el moderador ve los dudosos en la bandeja de Django (HU 14) y los aprueba o rechaza (HU 15).

## Reglas para trabajar en equipo

1. Cada historia va en su lado y en su módulo (ver la tabla de arriba). No se crean carpetas nuevas en la raíz.
   Si aparece una historia que no encaja, el grupo decide el módulo nuevo y lo agrega aquí antes de programar.
2. Cada integrante trabaja en su rama (`rama-jose`, `rama-karim`, `rama-edu`) y la une a `main` con un PR. Nadie sube directo a `main`.
   Antes de crear una migración nueva, `git pull origin main` y avisar al grupo qué número se toma.
3. Las tablas solo se crean con migraciones nuevas. No se edita una migración que ya se subió.
4. Respeta MVVM: la pantalla en views/ o screens/, la lógica en viewmodels/ o viewmodel/.
5. Antes de usar la IA para programar, pásale el archivo `CONTEXTO.md` de la raíz para que siga estas mismas reglas.