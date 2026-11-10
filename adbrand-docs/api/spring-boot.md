# API de Spring Boot (adbrand-core-api)

Base: `http://localhost:8080/api`. La usan la web del usuario (`adbrand-web`) y la app móvil (`adbrand-mobile`).

- Todas las rutas piden `Authorization: Bearer <token>`, menos `/api/auth/**` (salvo `GET /api/auth/sesion`), `GET /api/planes` y `GET /api/estado`.
- La empresa siempre sale del token: ninguna ruta lleva el id de la empresa en la URL.
- Los errores responden `{"codigo": "...", "mensaje": "...", "campos": {"campo": "mensaje"}}`.

## Endpoints

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
- `POST /auth/login` con `{correo, contrasena}`: responde `{token, tokenRenovacion, usuario}`; 401 `CREDENCIALES_INVALIDAS` o 403 `CUENTA_INACTIVA` (HU 22). El `tokenRenovacion` lo usa la app móvil (HU 43); la web puede ignorarlo.
- `POST /auth/renovar` con `{tokenRenovacion}`: ruta pública. Si el token existe, no está revocado y no venció, devuelve `{token, tokenRenovacion}` nuevo y revoca el anterior (rotación). Si no sirve, 401 `TOKEN_RENOVACION_INVALIDO` (HU 43).
- `POST /auth/cerrar-sesion` con `{tokenRenovacion}`: ruta pública e idempotente; revoca el token y responde 204 (HU 43).
- `GET /previsualizacion`: kit de marca más reciente de la empresa del token (o `null`) y hasta 20 anuncios, del más reciente al más antiguo (HU 45). Responde `{kit, anuncios}`.
- `GET /auth/sesion`: el usuario del token, para recuperar la sesión al recargar la página (HU 22).
- `POST /auth/recuperar/solicitar` con `{correo}`: envía un código de 6 dígitos que vence en 15 minutos; responde 200 con el mismo mensaje exista o no el correo (HU 24).
- `POST /auth/recuperar/cambiar` con `{correo, codigo, contrasenaNueva}`: cambia la contraseña; 400 `CODIGO_INVALIDO` si el código está mal, venció o ya se usó (HU 24).
- `GET /cuenta/empresa` y `PUT /cuenta/empresa` con `{ruc, razonSocial, telefono, direccion}`: datos de la empresa del token, sin id en la URL; 409 `RUC_REPETIDO` (HU 23).
- `GET /estado`: responde `{"backend": "ACTIVO", "ia": {"estado": "ACTIVO" o "CAIDO", "tiempo_ms", "detalle"}}` (HU 18). La IA se revisa con una petición liviana a `AI_BASE_URL` (no llama al chat ni manda la clave; 5 s de espera); con `AI_MOCK=true` sale ACTIVO con "Modo simulado". La usa Django y queda como ruta pública para que `/api/estado` no pida token.

