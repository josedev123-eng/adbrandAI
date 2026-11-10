# adbrand-admin-api

Backend de administración de AdBrand.AI, hecho con Django 5.2 y Django REST Framework. Lo usa solo la web de administración (`adbrand-admin-web`).

## Estructura

```
config/          Proyecto Django: settings, urls y conexión a la base compartida
core/            Inicio de sesión de administradores, permisos por rol y utilidades de prueba
usuarios/        HU 1, 2 y 3: administradores, roles y desactivación
auditoria/       HU 4: registro de acciones críticas
suscripciones/   HU 5 a 8: suscripciones activas, ingresos por mes y deudas
moderacion/      HU 14 a 16: bandeja de dudosos, aprobar o rechazar, historial
tokens/          HU 17, 19 y 20: consumo de IA, alertas e historial
servidor/        HU 18: estado del backend, la base de datos y la IA
```

Cada app usa `models.py` → `serializers.py` → `services.py` (reglas) → `views.py` → `urls.py`, más `tests.py`.

Las tablas las crea Spring Boot con Flyway: los modelos de Django llevan `managed = False` y no se usa `makemigrations` para ellas.

## Configuración

Copiar `.env.example` a `.env` y completar los valores. El `.env` no se sube al repositorio.

## Comandos

```bash
python -m venv venv
venv\Scripts\pip install -r requirements.txt
venv\Scripts\python manage.py runserver 8000     # http://localhost:8000/api
venv\Scripts\python manage.py test               # pruebas
venv\Scripts\python manage.py crear_superadmin   # primer administrador
```

Los endpoints están en [`adbrand-docs/api/django.md`](../adbrand-docs/api/django.md).
