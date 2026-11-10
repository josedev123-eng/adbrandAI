# API de Django (adbrand-admin-api)

Base: `http://localhost:8000/api`. La usa solo la web de administración (`adbrand-admin-web`).

- Todas las rutas, menos el inicio de sesión, piden `Authorization: Bearer <token>` del administrador, que la cuenta esté ACTIVA y un rol permitido por la vista.
- Quién hace cada acción sale del token; nunca se manda en el cuerpo.

## Endpoints

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
- `GET /moderacion/historial/` con `?estado=APROBADO|RECHAZADO`, `?desde=AAAA-MM-DD`, `?hasta=AAAA-MM-DD` y `?pagina=n` (todos opcionales): responde `{"total", "pagina", "paginas", "resultados": [{id, empresa, tipo, red_social, oferta, texto, estado, motivo, moderador, fecha_decision}]}`, del más reciente al más antiguo, 20 por página. `moderador` es el nombre del administrador o "Filtro automático" (HU 13). Las fechas filtran por `fecha_decision` con los dos días incluidos. Responde 400 si el estado no es válido, la fecha no es AAAA-MM-DD o el inicio es posterior al fin. Solo SUPERADMIN y MODERADOR (HU 16).
- `GET /auditoria/` con `?admin_id=`, `?desde=AAAA-MM-DD` y `?hasta=AAAA-MM-DD` (todos opcionales): historial de acciones, la más reciente primero, con el nombre y correo del administrador. Responde 400 si una fecha es inválida o `desde` es mayor que `hasta` (HU 4).
- `GET /tokens/consumo-actual/`: responde `{"mes": "2026-10", "total": n, "empresas": [{usuario_id, empresa, tokens, generaciones, ultima_generacion}]}` con los tokens de IA del mes actual (hora de Lima), de la empresa que más gasta a la que menos. La empresa es el `usuario_id`; el nombre sale de `perfil_negocio` o es "Empresa #id". Solo SUPERADMIN y TECNICO (HU 17).
  - HU 19: cada empresa trae además `limite`, `porcentaje_aviso`, `porcentaje` (1 decimal) y `alerta`, y la raíz trae `alertas: [{usuario_id, empresa, tokens, limite, porcentaje}]` con solo las empresas en alerta, de mayor a menor porcentaje. El límite es la fila propia de la empresa en `limite_tokens` o, si no tiene, la fila por defecto (`usuario_id` NULL). Hay alerta cuando `tokens * 100 >= limite * porcentaje_aviso` (en enteros: el 80 % exacto avisa). Sin ningún límite: `limite` y `porcentaje` en null y nunca hay alerta.
- `GET /tokens/historial/?desde=AAAA-MM&hasta=AAAA-MM` (los dos obligatorios; `&usuario_id=` opcional): responde `{"desde", "hasta", "total", "meses": [{mes, tokens, generaciones}]}` con todos los meses del periodo, también los que están en 0, del más antiguo al más reciente. Responde 400 si falta una fecha, si el formato no es AAAA-MM, si el inicio es posterior al fin ("La fecha de inicio no puede ser posterior a la fecha de fin") o si pasa de 24 meses. Solo SUPERADMIN y TECNICO (HU 20). La web hace la misma revisión del periodo antes de llamar.
- `GET /servidor/estado/`: verifica en ese momento la base de datos (`SELECT 1`), el backend (llama a `CORE_API_URL/api/estado`, 3 s de espera) y la IA (la informa Spring). Si Spring no responde, backend e IA quedan CAIDO. Guarda las 3 verificaciones en `verificacion_servidor` y, si la base falla, igual responde. Responde `{"verificado_en": "hora de Lima", "servicios": [{servicio, nombre, estado, tiempo_ms, detalle}]}`. Solo SUPERADMIN y TECNICO (HU 18).
- Qué se registra en la auditoría: crear una cuenta (`CREAR_ADMIN`), desactivar una cuenta (`DESACTIVAR_ADMIN`), cambiar un rol (`CAMBIAR_ROL`) y moderar un contenido (`APROBAR_CONTENIDO`, `RECHAZAR_CONTENIDO`). Para registrar una acción nueva se llama a `auditoria.services.registrar_accion(admin_id, accion, entidad, entidad_id, detalle)`; nunca lanza errores, así que no rompe la acción original.


## Inicio de sesión y roles

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

