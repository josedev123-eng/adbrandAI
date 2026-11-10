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
| 16 | Historial de contenidos aprobados y rechazados | Django `moderacion` (vista `vista_historial_contenido`, V34) + admin-web `features/moderacion` (entrada en el menú `Historial de contenidos`; filtros Estado, Desde y Hasta; clic en la fila para ver el texto) |
| 17 | Consumo de tokens de IA por empresa en el mes actual | Spring `ia` (guarda cada consumo) + Django `tokens` + admin-web `features/tokens` (entrada en el menú `Consumo de IA`; se actualiza sola cada 30 segundos) |
| 18 | Estado del servidor (backend, base de datos e IA) | Spring `estado` (`GET /api/estado`) + Django `servidor` (verifica y guarda en `verificacion_servidor`, V33) + admin-web `features/servidor` (entrada en el menú `Estado del servidor`; se verifica sola cada 15 segundos) |
| 19 | Alerta cuando una empresa llega al porcentaje de aviso de su límite mensual de tokens | Django `tokens` (límite en `limite_tokens`, V31) + admin-web `features/tokens` (aviso arriba de la tabla de `Consumo de IA` y columna % del límite) |
| 20 | Historial de consumo de tokens por periodo | Django `tokens` (vista `vista_consumo_token_mensual`, V32) + admin-web `features/tokens` (entrada en el menú `Historial de IA`; campos Desde y Hasta, por defecto los últimos 6 meses) |
| 21 | Registro de la cuenta de la PYME | Spring `auth` + web `features/auth` (pantalla `Crear una cuenta`) |
| 22 | Inicio de sesión del usuario PYME con JWT | Spring `auth` (`FiltroJwt`) + web `features/auth` y `app/context` (`SesionProvider`, `useSesion`); botón Cerrar sesión en el menú |
| 23 | Datos de la empresa (RUC, razón social, teléfono, dirección) | Spring `cuenta` + web `features/cuenta` (entrada en el menú `Mi cuenta`) |
| 24 | Recuperar la contraseña con un código de 6 dígitos por correo | Spring `auth` y `notificacion` + web `features/auth` (enlace ¿Olvidaste tu contraseña? en el inicio de sesión) |
| 25 | Ver los planes de suscripción | Spring `pago` (`GET /api/planes`, ruta pública) + web `features/pagos` (entrada en el menú `Planes`) |
| 26 | Pagar la suscripción con tarjeta (pasarela simulada) | Spring `pago` (`POST /api/pagos/tarjeta`) + web `features/pagos` (`PagoTarjetaView`) |
| 27 | Pagar la suscripción con Yape (QR y confirmación) | Spring `pago` (`/api/pagos/yape`) + web `features/pagos` (`PagoYapeView`) |
| 28 | Comprobante del pago por correo | Spring `pago` y `notificacion` (`GET /api/pagos/{id}/comprobante`) + web `features/pagos` (`ConfirmacionPagoView`) |
| 29 | Diseñar el logotipo del kit con vista previa | Spring `brandkit` (`PUT /api/kit-marca/{id}/logo`, columna `logo_diseno`, V36) + web `features/brandkit` (botón Diseñar logo en el kit; `EditarLogoView` y `LogoMarca`) |
| 30 | Elegir los colores de la marca con selector y verlos en un anuncio | Spring `brandkit` (`PUT /api/kit-marca/{id}/colores`) + web `features/brandkit` (botón Colores en el kit; `EditarColoresView` y `AnuncioVistaPrevia`) |
| 31 | Personalizar la identidad visual (tipografías y voz de marca); los anuncios nuevos usan esa voz | Spring `brandkit` (`PUT /api/kit-marca/{id}/identidad`, V37) y `contenido` (`PlantillaPromptAnuncio`) + web `features/brandkit` (botón Identidad en el kit; `EditarIdentidadView`) |
| 32 | Editar a mano colores, tipografías y logotipo del kit de marca | Spring `brandkit` (`EdicionKitMarcaService`) + web `features/brandkit` (lista Tus kits guardados y botón Editar en el kit) |
| 33 | Calendario mensual de publicaciones | Spring `calendario` (`GET /api/calendario?mes=AAAA-MM` y `POST /api/calendario`) + web `features/calendario` (entrada en el menú `Calendario`) |
| 34 | Programar un anuncio aprobado con fecha, hora y red social; no se programa en el pasado ni un anuncio rechazado | Spring `calendario` (`contenido_id`, V19) + web `features/calendario` (lista Anuncio aprobado en Programar una publicación) |
| 35 | Campañas y eventos especiales con color; filtro del calendario por campaña | Spring `calendario` (`/api/campanas`, tabla `campana`, V18) + web `features/calendario` (formulario Nueva campaña o evento, leyenda y selector Ver campaña) |
| 36 | Reprogramar una publicación arrastrándola a otro día; a un día pasado vuelve a su lugar con un aviso | Spring `calendario` (`PATCH /api/calendario/{id}/fecha`) + web `features/calendario` (arrastrar y soltar en la grilla) |
| 37 | Informe consolidado de las campañas de la empresa | Spring `reporte` (`GET /api/reportes/campanas`, índice V35) + web `features/reportes` (entrada en el menú `Informes`) |
| 38 | Descargar el informe de campañas en PDF | Spring `reporte` (`GET /api/reportes/campanas/pdf`, OpenPDF) + web `features/reportes` (botón Descargar PDF en `Informes`) |
| 39 | Descargar el informe de campañas en Word con formato de reporte | Spring `reporte` (`GET /api/reportes/campanas/docx`, Apache POI) + web `features/reportes` (botón Descargar Word en `Informes`) |
| 40 | Filtrar el informe y sus descargas por periodo y por campaña | Spring `reporte` (`desde`, `hasta` y `campanaId` en los 3 endpoints) + web `features/reportes` (filtros Desde, Hasta y Campaña en `Informes`) |

Inicio de sesión del usuario PYME (HU 21, 22 y 24):
- La web guarda el token en `localStorage` con la clave `adbrand-token` y lo manda en cada petición como `Authorization: Bearer <token>` (`apiClient.js`). Si Spring responde 401, la web borra el token y vuelve al inicio de sesión.
- `FiltroJwt` (`auth/config`) revisa el token y deja el id del usuario en el atributo `usuarioId`. Los controllers lo reciben con `@RequestAttribute("usuarioId") Long usuarioId`; ya no existe el usuario fijo `USUARIO_DE_PRUEBA`.
- Rutas públicas (sin token): `/api/auth/**` menos `GET /api/auth/sesion`, `GET /api/planes`, `GET /api/estado` (lo usa Django para la HU 18) y las peticiones `OPTIONS`. Las demás responden 401 `{"codigo":"NO_AUTENTICADO","mensaje":"Inicia sesión para continuar."}`.
- Las contraseñas se guardan solo con BCrypt (`spring-security-crypto`): mínimo 8 caracteres, una mayúscula, una minúscula y un número, sin espacios. El token se firma con `jwt.secreto`, que sale de `JWT_SECRET` en el `.env`.
- `ServicioCorreo` (`notificacion/service`) envía los correos. Con `MAIL_MOCK=true` no envía nada y escribe el correo en la consola de Spring como `[CORREO SIMULADO]`, así se puede leer el código de recuperación sin servidor de correo.

Pendiente conocido:
- Con `AI_MOCK=true` generar un kit (HU 11) falla, porque el modo simulado devuelve un anuncio y no el JSON del kit. Para probar la HU 32 se carga `adbrand-docs/datos-prueba/kit-marca.sql`.

### Endpoints que ya existen

Spring Boot (`http://localhost:8080/api`):
- `GET /negocio/perfil` y `PUT /negocio/perfil`: perfil del negocio (HU 9).
- `POST /contenido/anuncios`: genera el anuncio, lo pasa por el filtro y lo guarda (HU 10 y 13). Responde `{id, texto, redSocial, tono, simulado, estado, motivoRevision}`.
- `POST /contenido/anuncios/{id}/regenerar`: regenera contenido con los mismos parámetros (HU 12). Responde igual que generar.
- `POST /kit-marca/generar`: genera Kit de Marca (logo, tipografías, paleta, voz) (HU 11).
- `GET /kit-marca/{id}` y `GET /kit-marca`: consulta kits (HU 11). El kit de otra empresa responde 404 `KIT_MARCA_NO_ENCONTRADO`.
- `PUT /kit-marca/{id}` con `{colores: [{hex, nombre}], tipografiaPrimaria, tipografiaSecundaria, logoConcepto}`: guarda los cambios en el kit y una versión en el historial. Colores en formato `#RRGGBB` (HU 32).
- `GET /kit-marca/{id}/versiones`: historial de ediciones del kit, la más reciente primero (HU 32). Cada versión trae también `logoDiseno` y `vozMarca`.
- `PUT /kit-marca/{id}/logo` con `{texto, forma, colorFondo, colorTexto, tipografia}`: guarda el logo diseñado y una versión nueva. `texto` hasta 20 caracteres, `forma` CIRCULO, CUADRADO o REDONDEADO, colores `#RRGGBB` (HU 29). El kit lo devuelve como `logoDiseno` (null si no tiene).
- `PUT /kit-marca/{id}/colores` con `{colores: [{hex, nombre}]}`: cambia solo la paleta (1 a 8 colores `#RRGGBB`) y guarda una versión nueva; 400 `COLOR_REPETIDO` si un color se repite (HU 30).
- `PUT /kit-marca/{id}/identidad` con `{tipografiaPrimaria, tipografiaSecundaria, vozMarca}`: cambia tipografías y voz (la principal y la voz son obligatorias; la secundaria es opcional) y guarda una versión nueva (HU 31).
- `GET /reportes/campanas`: informe de la empresa del token, sin id en la URL. Responde `{empresa, total, aprobados, dudosos, rechazados, campanas: [{id, fecha, redSocial, oferta, estado}]}`, del más reciente al más antiguo (HU 37).
- `GET /reportes/campanas/pdf`: el mismo informe en PDF (`application/pdf`), que se descarga como `informe-campanas-AAAA-MM-DD.pdf` (HU 38).
- `GET /reportes/campanas/docx`: el mismo informe en Word (`.docx`), que se descarga como `informe-campanas-AAAA-MM-DD.docx` (HU 39).
- Los 3 endpoints del informe aceptan `?desde=AAAA-MM-DD`, `?hasta=AAAA-MM-DD` y `?campanaId=` (todos opcionales; sin ellos sale el informe completo y con el mismo formato). El periodo filtra por `contenido.fecha_creacion` con los dos días incluidos; la campaña deja solo los anuncios programados en ella (`publicacion_programada.contenido_id`). 400 `FECHAS_INVALIDAS` si el inicio es posterior al fin, 400 `FECHA_INVALIDA` si una fecha no es AAAA-MM-DD y 404 `CAMPANA_NO_ENCONTRADA` si la campaña no es de la empresa (HU 40).
- `GET /campanas` y `POST /campanas` con `{nombre, color, tipo, fechaInicio, fechaFin}`: campañas y eventos de la empresa del token, de la que empieza antes a la que empieza después. `color` es `#RRGGBB` (se guarda en mayúsculas), `tipo` es CAMPANA o EVENTO; 400 `FECHAS_INVALIDAS` si el fin es anterior al inicio (HU 35).
- `GET /calendario?mes=AAAA-MM&campanaId=` (el filtro es opcional): cada publicación trae además `campanaId`, `campanaNombre`, `campanaColor`, `contenidoId` y `contenidoOferta` (en null si no tiene) (HU 35 y 34).
- `POST /calendario` acepta además `campanaId` y `contenidoId` (opcionales). 400 `FECHA_PASADA` si la fecha y hora ya pasaron; 404 `CONTENIDO_NO_ENCONTRADO` si el anuncio no es de la empresa; 400 `CONTENIDO_RECHAZADO` o `CONTENIDO_NO_APROBADO` si no está aprobado; 404 `CAMPANA_NO_ENCONTRADA` (HU 34 y 35).
- `PATCH /calendario/{id}/fecha` con `{fecha}`: mueve la publicación a otro día y mantiene la hora. 404 `PUBLICACION_NO_ENCONTRADA`, 400 `PUBLICACION_NO_MOVIBLE` si ya está PUBLICADA o CANCELADA y 400 `FECHA_PASADA` (HU 36).
- `POST /auth/registro`: crea la cuenta con `{nombre, nombreEmpresa, correo, contrasena}`; 201, o 409 `CORREO_REPETIDO` (HU 21).
- `POST /auth/login` con `{correo, contrasena}`: responde `{token, usuario}`; 401 `CREDENCIALES_INVALIDAS` o 403 `CUENTA_INACTIVA` (HU 22).
- `GET /auth/sesion`: el usuario del token, para recuperar la sesión al recargar la página (HU 22).
- `POST /auth/recuperar/solicitar` con `{correo}`: envía un código de 6 dígitos que vence en 15 minutos; responde 200 con el mismo mensaje exista o no el correo (HU 24).
- `POST /auth/recuperar/cambiar` con `{correo, codigo, contrasenaNueva}`: cambia la contraseña; 400 `CODIGO_INVALIDO` si el código está mal, venció o ya se usó (HU 24).
- `GET /cuenta/empresa` y `PUT /cuenta/empresa` con `{ruc, razonSocial, telefono, direccion}`: datos de la empresa del token, sin id en la URL; 409 `RUC_REPETIDO` (HU 23).
- `GET /estado`: responde `{"backend": "ACTIVO", "ia": {"estado": "ACTIVO" o "CAIDO", "tiempo_ms", "detalle"}}` (HU 18). La IA se revisa con una petición liviana a `AI_BASE_URL` (no llama al chat ni manda la clave; 5 s de espera); con `AI_MOCK=true` sale ACTIVO con "Modo simulado". La usa Django y queda como ruta pública para que `/api/estado` no pida token.

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
- `GET /moderacion/historial/` con `?estado=APROBADO|RECHAZADO`, `?desde=AAAA-MM-DD`, `?hasta=AAAA-MM-DD` y `?pagina=n` (todos opcionales): responde `{"total", "pagina", "paginas", "resultados": [{id, empresa, tipo, red_social, oferta, texto, estado, motivo, moderador, fecha_decision}]}`, del más reciente al más antiguo, 20 por página. `moderador` es el nombre del administrador o "Filtro automático" (HU 13). Las fechas filtran por `fecha_decision` con los dos días incluidos. Responde 400 si el estado no es válido, la fecha no es AAAA-MM-DD o el inicio es posterior al fin. Solo SUPERADMIN y MODERADOR (HU 16).
- `GET /auditoria/` con `?admin_id=`, `?desde=AAAA-MM-DD` y `?hasta=AAAA-MM-DD` (todos opcionales): historial de acciones, la más reciente primero, con el nombre y correo del administrador. Responde 400 si una fecha es inválida o `desde` es mayor que `hasta` (HU 4).
- `GET /tokens/consumo-actual/`: responde `{"mes": "2026-10", "total": n, "empresas": [{usuario_id, empresa, tokens, generaciones, ultima_generacion}]}` con los tokens de IA del mes actual (hora de Lima), de la empresa que más gasta a la que menos. La empresa es el `usuario_id`; el nombre sale de `perfil_negocio` o es "Empresa #id". Solo SUPERADMIN y TECNICO (HU 17).
  - HU 19: cada empresa trae además `limite`, `porcentaje_aviso`, `porcentaje` (1 decimal) y `alerta`, y la raíz trae `alertas: [{usuario_id, empresa, tokens, limite, porcentaje}]` con solo las empresas en alerta, de mayor a menor porcentaje. El límite es la fila propia de la empresa en `limite_tokens` o, si no tiene, la fila por defecto (`usuario_id` NULL). Hay alerta cuando `tokens * 100 >= limite * porcentaje_aviso` (en enteros: el 80 % exacto avisa). Sin ningún límite: `limite` y `porcentaje` en null y nunca hay alerta.
- `GET /tokens/historial/?desde=AAAA-MM&hasta=AAAA-MM` (los dos obligatorios; `&usuario_id=` opcional): responde `{"desde", "hasta", "total", "meses": [{mes, tokens, generaciones}]}` con todos los meses del periodo, también los que están en 0, del más antiguo al más reciente. Responde 400 si falta una fecha, si el formato no es AAAA-MM, si el inicio es posterior al fin ("La fecha de inicio no puede ser posterior a la fecha de fin") o si pasa de 24 meses. Solo SUPERADMIN y TECNICO (HU 20). La web hace la misma revisión del periodo antes de llamar.
- `GET /servidor/estado/`: verifica en ese momento la base de datos (`SELECT 1`), el backend (llama a `CORE_API_URL/api/estado`, 3 s de espera) y la IA (la informa Spring). Si Spring no responde, backend e IA quedan CAIDO. Guarda las 3 verificaciones en `verificacion_servidor` y, si la base falla, igual responde. Responde `{"verificado_en": "hora de Lima", "servicios": [{servicio, nombre, estado, tiempo_ms, detalle}]}`. Solo SUPERADMIN y TECNICO (HU 18).
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

Cada integrante usa su rango de versiones: Karim V13 a V19, Edu V20 a V29, Jose V30 a V39. Por eso cada uno pone `spring.flyway.out-of-order=true` en su `adbrand-core-api/.env`, así Flyway aplica una versión menor que llega después de una mayor. V12 es del login de administradores.

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
| V18 | `campana` (nombre, color `#RRGGBB`, tipo CAMPANA o EVENTO, fecha de inicio y de fin) y `publicacion_programada.campana_id` | 35 |
| V19 | `publicacion_programada.contenido_id` (el anuncio que se va a publicar) | 34 |
| V20 | `plan` (planes de suscripción Básico, Pro y Premium) | 25 |
| V21 | `pago`: datos de la operación y del plan pagado | 26 |
| V22 | `operacion_yape` (pago con Yape en dos pasos) | 27 |
| V23 | `pago.numero_comprobante` | 28 |
| V24 | `publicacion_programada` (publicaciones del calendario) | 33 |
| V30 | `consumo_token` (tokens de entrada, salida y total de cada generación con IA, por `usuario_id` y `origen`; `simulado` si se estimaron) | 17 |
| V31 | `limite_tokens` (límite mensual y porcentaje de aviso; `usuario_id` NULL es el de por defecto: 100 000 tokens y aviso al 80 %; una fila por empresa) | 19 |
| V32 | vista `vista_consumo_token_mensual` (tokens y generaciones por mes, con la hora de Lima, y por `usuario_id`; solo lectura) | 20 |
| V33 | `verificacion_servidor` (servicio BACKEND, BASE_DATOS o IA; estado ACTIVO o CAIDO; tiempo_ms, detalle y fecha de cada verificación) | 18 |
| V34 | vista `vista_historial_contenido` (contenidos APROBADO o RECHAZADO con el nombre del moderador o "Filtro automático", el motivo y la fecha de la decisión; solo lectura) | 16 |
| V35 | índice de `contenido` por `usuario_id` y `fecha_creacion` (para el informe de campañas) | 37 |
| V36 | `logo_diseno` (TEXT con el JSON del logo) en `kit_marca` y `kit_marca_version` | 29 |
| V37 | `kit_marca_version.voz_marca` | 31 |

Si la base local ya tenía una versión mayor cuando llegaron V13 a V19 o V30 a V37, Spring necesita `spring.flyway.out-of-order=true` en el `.env` para aplicarlas.

Los datos de prueba NO van en Flyway: están en `adbrand-docs/datos-prueba/` y se cargan a mano sobre la base local, una sola vez. `contenido-dudoso.sql` llena la bandeja de revisión, `suscripciones-y-pagos.sql` llena el panel financiero y la lista de deudas (8 suscripciones y sus pagos de los últimos 6 meses) y `kit-marca.sql` deja un kit listo para editar (HU 32; cambiar el correo por el de tu cuenta).

### Resumen del Sprint 2 de Jose (`rama-jose`)

| HU | Qué quedó | Migración |
|---|---|---|
| 17 (tarea técnica) | Login de administradores con token, menú según el rol y Django revisando el rol en cada endpoint | V12 (rol TECNICO) |
| 17 | Consumo de tokens de IA por empresa en el mes actual (Spring guarda cada consumo; panel `Consumo de IA`) | V30 |
| 19 | Alerta cuando una empresa llega al porcentaje de aviso de su límite mensual | V31 |
| 20 | Historial de consumo de tokens por periodo (`Historial de IA`) | V32 |
| 18 | Estado del backend, la base de datos y la IA (`Estado del servidor`) | V33 |
| 16 | Historial de contenidos aprobados y rechazados (`Historial de contenidos`) | V34 |

Pruebas al cerrar el sprint: Spring 59, Django 170 y admin-web 102, todas en verde.

### Resumen del Sprint 3 de Jose (`rama-jose`)

| HU | Qué quedó | Migración |
|---|---|---|
| 37 | Informe consolidado de campañas (`Informes`): empresa, totales por estado y una fila por anuncio | V35 |
| 29 | Editor del logo con vista previa; el logo se guarda como parámetros en JSON, no como imagen ni SVG | V36 |
| 30 | Editor de colores con selector y un anuncio de ejemplo pintado con la paleta | — |
| 31 | Editor de tipografías y voz de marca con vista previa; generar y regenerar anuncios usan la voz del kit editado más reciente | V37 |
| 38 | Descargar el informe en PDF con la empresa, los totales y la tabla de campañas | — |

Notas del sprint de Jose:

- Cada editor del kit (logo, colores, identidad) guarda el cambio como una versión nueva en `kit_marca_version` con `EdicionKitMarcaService.guardarComoVersion`. Un editor nuevo debe usar el mismo método.
- `logoDiseno` (HU 29) es `{texto, forma, colorFondo, colorTexto, tipografia}`; la web lo dibuja con `LogoMarca.jsx`. Lo puede usar la app móvil (HU 45) para mostrar el logo del kit.
- HU 31: `PlantillaPromptAnuncio.construir(perfil, oferta, red, identidad)` agrega "Voz de la marca" al prompt si el kit tiene voz. El kit que cuenta es el editado más reciente (`KitMarcaRepository.findFirstByUsuarioIdOrderByFechaActualizacionDescIdDesc`). Sin kit, el prompt queda igual que antes.
- HU 38: el PDF se arma con OpenPDF (`reporte/pdf/PlantillaInformePdf`) a partir de `InformeService.campanas`, el mismo que usa la pantalla, para que los datos coincidan. El Word (HU 39) y el filtro por periodo (HU 40) deben partir del mismo `InformeService`.
- En la web, `apiDescargar(ruta)` de `shared/services/apiClient.js` descarga archivos con el token (devuelve un Blob), y `guardarArchivo` y `nombreArchivoInforme` de `features/reportes/models/reportesApi.js` los guardan con su nombre.
- Pruebas al cerrar el sprint: Spring 219 y web del usuario 45, todas en verde.

### Resumen del Sprint 3 de Karim (`rama-karim`)

| HU | Qué quedó | Migración |
|---|---|---|
| 35 | Campañas y eventos con nombre, color y fechas; las publicaciones de una campaña se pintan con su color y el calendario se filtra por campaña | V18 |
| 34 | Programar eligiendo un anuncio aprobado; no se programa en una fecha u hora pasada ni un anuncio rechazado o en revisión | V19 |
| 36 | Arrastrar una publicación programada a otro día; a un día pasado no se mueve y sale un aviso, y si Spring rechaza el cambio vuelve a su día | — |
| 40 | Filtros Desde, Hasta y Campaña en `Informes`; el informe, el PDF y el Word salen con los mismos filtros | — |
| 39 | Botón Descargar Word en `Informes`; el `.docx` trae empresa, periodo, totales y la tabla de anuncios | — |

Notas del sprint de Karim:

- El informe, el PDF y el Word salen de `InformeService.campanas(usuarioId, desde, hasta, campanaId)`, así los tres muestran los mismos datos. El formato del JSON de la HU 37 no cambió.
- El Word se arma con Apache POI (`org.apache.poi:poi-ooxml` en el `pom.xml`) en `reporte/word/PlantillaInformeWord`, con los colores de la marca.
- `CalendarioService`, `PublicacionResponse`, `CrearPublicacionRequest` e `InformeService` conservan sus constructores anteriores, así las pruebas de la HU 33 y de la HU 37 siguen igual.
- La fecha pasada se revisa en la web (solo el día, para avisar sin consultar) y en Spring (día y hora). Las pruebas nuevas usan fechas relativas a hoy (`LocalDate.now().plusDays(7)`), así no dejan de pasar con el tiempo.
- En la web, `features/calendario/models` tiene `campanasApi.js` (también lo usa `Informes` para el filtro) y `anunciosApi.js` (los anuncios para programar salen de `/reportes/campanas`).
- Pruebas al cerrar el sprint: Spring 251 y web del usuario 69, todas en verde.

### Resumen del Sprint 2 de Edu (`rama-edu`)

| HU | Qué quedó | Migración |
|---|---|---|
| 25 | Planes Básico, Pro y Premium en `GET /api/planes`; pantalla `Planes` (`features/pagos`) | V20 |
| 26 | Pago con tarjeta simulado (Luhn, vencimiento y CVV), suscripción de un mes y `POST /api/pagos/tarjeta`; pantalla `PagoTarjetaView` | V21 |
| 27 | Pago con Yape en dos pasos: `POST /api/pagos/yape`, `POST /api/pagos/yape/{id}/confirmar` y `GET /api/pagos/yape/pendientes`; pantalla `PagoYapeView` con QR (`qrcode`) | V22 |
| 28 | Comprobante `ADB-000001` con `GET /api/pagos/{id}/comprobante` y envío con `ServicioCorreo`; pantalla `ConfirmacionPagoView` | V23 |
| 33 | Calendario mensual con `GET /api/calendario?mes=AAAA-MM` y `POST /api/calendario`; pantalla `Calendario` (`features/calendario`) | V24 |

Notas del sprint de Edu:

- Cada pago aprobado (tarjeta o Yape) crea una suscripción `ACTIVA` de un mes en `suscripcion` (V7) y una fila en `pago` (V10); el panel financiero de Django los ve sin cambios.
- El número de comprobante se deriva del id del pago (`ADB-%06d`) y el correo sale solo la primera vez que se pide. Con `MAIL_MOCK=true` el comprobante se escribe en la consola.
- La pasarela es simulada: la tarjeta `4000 0000 0000 0002` siempre se rechaza. El pago con Yape queda `PENDIENTE` hasta que el usuario pulsa "Ya pagué, confirmar".
- Pruebas al cerrar el sprint: Spring 37 (pago y calendario) y React 13 (Vitest), todas en verde. La suite completa de Spring y Django necesita la base PostgreSQL local con sus credenciales.

### Notas para las siguientes historias

- Autenticación de administradores: ver la sección "Inicio de sesión de administradores". Toda vista nueva de la web de administración hereda de `VistaAdmin` y declara sus `roles_permitidos`.
- El `Panel financiero` (`PanelFinancieroView` y `usePanelFinanciero` en `features/suscripciones`) reúne las HU 5, 6 y 7. Un indicador nuevo del dueño de la aplicación va ahí, con su endpoint en la app Django `suscripciones`.
- Las HU 25 a 28 (pagos del usuario, en Spring) escriben en las tablas `pago` (V10) y `suscripcion` (V7), de donde el panel financiero saca los ingresos. Las columnas nuevas se agregaron con V21 y V23; no se edita V10.

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
- `adbrand-core-api/.env`: `DB_PASSWORD`, las variables `AI_*`, `spring.flyway.out-of-order=true`, `JWT_SECRET` (texto largo al azar, mínimo 32 caracteres) y `MAIL_MOCK=true`. Para enviar correos de verdad: `MAIL_MOCK=false` con `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` y `MAIL_FROM`.
- `adbrand-admin-api/.env`: `DB_PASSWORD`. Opcional: `CORE_API_URL` (dirección de Spring para el panel del estado del servidor, por defecto `http://localhost:8080`).

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
| Las dos webs | Vitest y Testing Library | `npm test` |
| Las dos webs | ESLint | `npm run lint` |

- En Spring, las pruebas de un módulo van en `src/test/java/com/adbrand/core/<modulo>/`. Usan Mockito para no depender de la base de datos ni de la IA. Las pruebas de `auth`, `cuenta`, `EditarKitMarcaTest`, las del calendario (`CampanaTest`, `ProgramarPublicacionTest`, `ReprogramarPublicacionTest`) y las del informe (`InformeFiltrosTest`, `InformeWordTest`) usan `@SpringBootTest` con `@Transactional` para pasar por `FiltroJwt` con tokens reales: usan la base local y todo se deshace al terminar cada prueba.
- En Django, el runner `core/test_runner.py` crea las tablas de Flyway en la base temporal de pruebas, así que las pruebas no necesitan Spring.
- La vista `vista_consumo_token_mensual` (V32) se lee con el modelo `ConsumoMensual` (clave compuesta `mes` + `usuario_id`). En la base de prueba Django la crea como una tabla común, así que las pruebas la llenan directo.
- La vista `vista_historial_contenido` (V34) se lee con el modelo `HistorialContenido`. Sus pruebas usan `usar_vista_de_flyway("V34__...sql")` de `core/pruebas.py`, que cambia la tabla de prueba por la vista real con el mismo SQL de la migración.
- Como los endpoints de Django piden token, las pruebas usan `core/pruebas.py`: `crear_admin("ROL")` crea un administrador activo, `con_sesion(admin)` da el header para un pedido e `iniciar_sesion(self.client, admin)` lo deja puesto para todos los pedidos.
- En React, las pruebas reemplazan el `models/...Api.js` con datos de prueba (`vi.mock`), así no necesitan el backend encendido.

## Convenciones

- Errores de Spring Boot: `{"codigo": "...", "mensaje": "...", "campos": {"campo": "mensaje"}}` (ver `shared/error/ManejadorDeErrores`). Códigos usados: `DATOS_INVALIDOS` (400), `CODIGO_INVALIDO` (400), `NO_AUTENTICADO` y `CREDENCIALES_INVALIDAS` (401), `CUENTA_INACTIVA` (403), `PERFIL_NO_ENCONTRADO` y `KIT_MARCA_NO_ENCONTRADO` (404), `CORREO_REPETIDO` y `RUC_REPETIDO` (409), `IA_NO_DISPONIBLE` (503). Calendario e informes (HU 34 a 36, 39 y 40): `FECHA_PASADA`, `FECHAS_INVALIDAS`, `FECHA_INVALIDA`, `CONTENIDO_RECHAZADO`, `CONTENIDO_NO_APROBADO` y `PUBLICACION_NO_MOVIBLE` (400), `CAMPANA_NO_ENCONTRADA`, `CONTENIDO_NO_ENCONTRADO` y `PUBLICACION_NO_ENCONTRADA` (404).
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
- No crear tablas fuera de Flyway ni editar las migraciones ya subidas (V1 a V24 y V30 a V37). Cada uno usa su rango (Karim V13-V19, Edu V20-V29, Jose V30-V39).
- No borrar ni reemplazar pruebas de otros integrantes; solo agregar las nuevas.
- No poner llamadas a la API dentro de `views/` o `screens/`.
- No escribir contraseñas, la URL de la IA ni su clave en el código.
- No tocar módulos de otra historia que no sea la pedida.

## Cómo pedirle código a la IA

1. Pega este archivo.
2. Di la historia exacta, por ejemplo: "Implementa la HU 15 (aprobar o rechazar contenido) en `adbrand-admin-api/moderacion` y `adbrand-admin-web/src/features/moderacion`, sobre lo que ya hizo la HU 14".
3. Pide un solo backend o un solo frontend por conversación.
4. Revisa que los archivos que te dé caigan en las carpetas de la tabla de arriba y que incluya sus pruebas.