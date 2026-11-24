# Sistema de diseño de AdBrand.AI

Guía para que todas las pantallas (web del usuario, panel del administrador y app móvil) se vean como **una sola marca**.
Si haces una pantalla nueva, usa lo que está aquí antes de inventar estilos propios.

## 1. Dirección de arte

- **Personalidad:** cercana, clara y confiable. AdBrand.AI le habla a dueños de PYMES, no a diseñadores:
  textos cortos, en español sencillo y de tú ("Guarda tus datos", no "Proceda a guardar").
- **Marca:** cuadrado **índigo plano** (`#4F46E5`, el mismo color de los botones) con **"AB" en blanco** y un
  **punto coral** (el ".AI"). Logo y botones del mismo color: el usuario reconoce la marca sin pensar.
  Es el mismo en el favicon, el menú lateral de las dos webs (`MarcaAdbrand`) y la app móvil (`LogoApp`).
  El panel del administrador usa el mismo logo con un borde coral para distinguir su pestaña.
- **Sin degradados en la marca:** los degradados índigo-violeta son el estilo más repetido en apps de IA y no
  distinguen a nadie. La marca usa colores **planos**: un color principal (índigo, en el logo y los botones) y **un solo
  acento: coral** (`--acento`). El coral se usa poco y con intención: el punto del logo, el ".AI" y detalles.
- **La IA tiene su propio color:** violeta/magenta (`--ia`). Todo lo que hace la IA (botón "Generar",
  etiqueta "Generado con IA") va en ese color, para que el usuario sepa qué viene de la IA.
- **Una sola acción principal por pantalla** (botón índigo). Lo demás va como botón secundario.

## 2. Tokens (en `src/index.css`, `:root`)

| Token | Valor | Uso |
|---|---|---|
| `--primary` | `#4f46e5` índigo | Acción principal, enlaces, foco |
| `--primary-soft` | `#ecebfd` | Fondos suaves de lo seleccionado |
| `--acento` / `--acento-texto` | `#f97316` / `#c2410c` | Acento de marca (coral). El claro para formas; el oscuro para texto (contraste AA) |
| `--ia` / `--ia-soft` | `#b5179e` / `#fbe8f8` | Todo lo que hace la IA |
| `--ok` / `--ok-soft` | verde | Éxito ("Datos guardados") |
| `--warn` / `--warn-soft` | ámbar | Advertencias, "De prueba" |
| `--bad` / `--bad-soft` | rojo | Errores y acciones peligrosas |
| `--fg` / `--muted` | `#1b1d2e` / `#5d6178` | Texto principal / secundario |
| `--bg` / `--surface` / `--surface-2` | grises claros / blanco | Fondo de la app / tarjetas / zonas suaves |
| `--line` | `#dfe1ec` | Bordes |
| `--shadow` | sombra suave | Tarjetas |

**Tipografías:** `--f-display` (Bricolage Grotesque) para títulos, `--f-body` (Plus Jakarta Sans) para texto,
`--f-mono` (JetBrains Mono) para códigos (comprobantes, referencias).

**Espacios:** múltiplos de 4 px (4, 8, 12, 16, 20, 24). **Bordes redondeados:** 10 px en campos y botones,
14 px en tarjetas, 999 px en etiquetas.

## 3. Componentes base (en `src/styles/sistema-diseno.css`)

| Clase | Para qué |
|---|---|
| `.pagina` + `.pagina-cabecera` | Contenedor de cada pantalla con su título y una frase de qué hace. Para cambiar el ancho, agrega tu clase: `.pagina.pagina-informe { max-width: 1200px }` |
| `.tarjeta` | Bloque blanco con borde y sombra |
| `.formulario`, `.fila`, `.campo`, `.campo-error`, `.campo-ayuda`, `.campo-con-error` | Formularios |
| `.boton-primario` | La acción principal (una por pantalla) |
| `.boton-secundario` | Cancelar, Volver, Editar |
| `.boton-ia` | Acciones que usan IA (solo web del usuario) |
| `.boton-peligro` | Desactivar, rechazar (solo admin) |
| `.etiqueta-ia`, `.etiqueta-simulado` | Etiquetas pequeñas |
| `.solo-lector` | Texto que solo leen los lectores de pantalla |

## 4. Componentes de React (en `src/shared/components`)

| Componente | Uso |
|---|---|
| `<Aviso mensaje={{ tipo, texto }} />` | Recuadro de aviso. Los de **éxito se desvanecen solos** a los 4 s; los de **error se quedan**. |
| `<Cargando texto="Cargando…" filas={3} />` | Esqueleto animado mientras llegan los datos. **No uses texto suelto** ("Cargando...") |
| `<SinResultados>` | Lista vacía, con ícono y mensaje |
| `<PantallaArranque />` | Pantalla con el logo mientras se revisa la sesión |
| `<LimiteDeErrores>` | Si una pantalla se rompe, muestra "Algo salió mal" en vez de quedar en blanco |
| `<AvisoConexion />` | Franja "Sin conexión a internet" |
| `<MarcaAdbrand />` | Logo + nombre |

## 5. Patrones de UX

- **Ver primero, editar después:** los datos guardados se muestran solo para leer, con un botón **"✎ Editar"**.
  El formulario tiene **Cancelar** y **Guardar cambios**; al guardar vuelve a la vista con el aviso de éxito.
  Ejemplos: Mi cuenta y Perfil del negocio.
- **Estados de cada pantalla:** cargando (esqueleto), vacía (`SinResultados`), error (aviso rojo con qué hacer)
  y con datos. Ninguna pantalla debe quedar en blanco.
- **Errores en lenguaje simple:** el cliente HTTP (`shared/services/apiClient.js`) ya traduce cada falla:
  servidor caído, sin internet, tiempo agotado, 403, 404, 429, 500… Muestra `error.message`.
- **Validar antes de enviar:** los errores de cada campo salen debajo del campo, en rojo.
- **Botones ocupados:** mientras se guarda, el botón cambia su texto ("Guardando...") y se desactiva.

## 6. Accesibilidad

- Foco visible (anillo índigo) para quien navega con teclado: `:focus-visible`.
- Si el sistema pide "reducir movimiento", se apagan las animaciones.
- Cada imagen con `alt` que diga qué muestra (o `alt=""` si es decorativa).
- Los avisos usan `role="status"` (éxito) o `role="alert"` (error) para lectores de pantalla.
- Contraste: texto `--fg` sobre blanco y `--muted` sobre blanco cumplen WCAG AA.

## 7. Rendimiento

- Cada pantalla se descarga cuando el usuario la abre (`React.lazy` en `App.jsx`). La carga inicial de la web
  del usuario bajó de 368 KB a 255 KB.
- Las imágenes generadas usan `loading="lazy"` y `decoding="async"`.
- `index.html` abre antes la conexión con Google Fonts (`preconnect`).

## 8. SEO y metadatos

- Web del usuario: `lang="es-PE"`, título y descripción, Open Graph (vista previa al compartir en WhatsApp o
  Facebook), `robots.txt` que permite indexar y un título por pantalla ("Mi cuenta · AdBrand.AI").
- Panel del administrador: **`noindex, nofollow`** y `robots.txt` que bloquea todo: un panel interno nunca
  debe aparecer en Google.

## 9. Seguridad en el frontend

- Nunca poner claves secretas en el código del navegador: todo lo que empieza con `VITE_` es **público**.
  Las llaves secretas (IA, Mercado Pago) van solo en el `.env` del backend.
- Los datos de tarjeta y el código de Yape los recibe el SDK de Mercado Pago: nunca pasan por nuestro código.
- No se usa `dangerouslySetInnerHTML`: React escapa todo el texto y evita inyección de HTML (XSS).
- `referrer` estricto: al salir a otro sitio solo se comparte el dominio.
- El token de sesión se guarda en `localStorage`. **Al publicar en internet** conviene agregar en el servidor:
  - HTTPS obligatorio.
  - Cabecera `Content-Security-Policy` que solo permita scripts propios, `sdk.mercadopago.com` y Google Fonts.
  - `X-Frame-Options: DENY` (que nadie meta la web dentro de otra) y `X-Content-Type-Options: nosniff`.
  - Idealmente, mover el token a una cookie `HttpOnly` + `Secure` + `SameSite`.
