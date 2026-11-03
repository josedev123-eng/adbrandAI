# Sprint 3 – Cronograma de commits (martes 3 al lunes 9 de noviembre de 2026)

Plan día por día de los commits del Sprint 3 (Entrega 3 de 4). Sigue el reparto A/B/C, donde la persona C hace solo la app móvil.

> **Por confirmar:** el reparto supone **A = Karim**, **B = Edu** y **C = Jose**, por continuidad: Karim hizo el kit (HU 32), Edu el calendario (HU 33) y Jose queda con el móvil. Si cambia, solo hay que cambiar los nombres.

## Reparto

| Persona | Historias | Horas |
|---|---|---|
| **A – Karim** | HU 37, 38 (informe consolidado y PDF) + HU 29, 30, 31 (editor del kit de marca) | 53 h |
| **B – Edu** | HU 35, 34, 36 (campañas y calendario) + HU 39, 40 (informe en Word y filtro por periodo o campaña) | 52 h |
| **C – Jose** | HU 42, 43, 41, 44, 45 (app móvil: login, sesión, perfiles y previsualización) | 53 h |

Cada historia tiene 4 tareas, y **cada tarea es un commit**: 20 commits por persona y 60 en total.

## Reglas para todos

1. **Cada mañana**, antes de empezar: `git pull origin main` en tu rama.
2. **Un commit por tarea**, con el formato `HU-XX: descripción`, con tildes.
3. **Antes de cada commit**, corre las pruebas de la parte que tocaste (`mvnw test`, `manage.py test` o `npm test -- --no-file-parallelism`).
4. **Al terminar el día**, sube tus commits a tu rama: `git push origin rama-<tu-nombre>`.
5. **Hora de Windows en automático.** Las fechas de los commits salen de la hora real de la PC: no se cambian ni se ponen a mano.
6. **Nunca** `git push --force`, ni `rebase` sobre lo que ya está subido, ni editar el código de otro sin avisar.
7. **Migraciones**, cada uno en su rango: Karim **V18-V19**, Edu **V25-V29**, Jose **V35-V39**.

## Orden por dependencias

- **Martes:** B crea primero la **tabla de campañas** (HU 35), porque el filtro por campaña de los informes (HU 40) la necesita.
- **Martes y miércoles:** A hace primero el **endpoint del informe** (HU 37), porque B lo usa para el Word (HU 39) y el filtro (HU 40).
- **Martes:** C crea primero los **dos Singleton** del móvil (`ClienteApi` y `GestorSesion`), porque todas las pantallas los usan.
- **Sábado:** A termina el **kit de marca** (HU 29-31) y le pasa a C el formato del logo, los colores y las tipografías, para la previsualización (HU 45).

---

## Martes 3

| Persona | Commits |
|---|---|
| A – Karim | `HU-37: consulta consolidada de campañas por empresa` (V19) · `HU-37: endpoint del informe consolidado en Spring` |
| B – Edu | `HU-35: tabla de campañas y eventos especiales en Flyway` (V25) · `HU-35: asignar publicaciones a una campaña en Spring` |
| C – Jose | `HU-42: cliente HTTP único (Singleton) que conecta con Spring Boot` · `HU-42: gestor de sesión (Singleton) que guarda el token` |

## Miércoles 4

| Persona | Commits |
|---|---|
| A – Karim | `HU-37: pantalla de informes` · `HU-37: pruebas del informe con y sin campañas` · `HU-29: guardar el logo en la versión del kit` (V18) |
| B – Edu | `HU-35: campañas con colores y filtro en el calendario` · `HU-35: pruebas de agrupar y filtrar por campaña` · `HU-34: hora y estado de la publicación programada en Flyway` (V26) |
| C – Jose | `HU-42: pantalla de inicio de sesión en la app` · `HU-42: pruebas del login móvil` · `HU-43: tabla de tokens de renovación en Flyway` (V35) |

## Jueves 5

| Persona | Commits |
|---|---|
| A – Karim | `HU-29: guardar y versionar el logo en Spring` · `HU-29: editor interactivo del logo con vista previa` · `HU-29: pruebas del logo y su historial` |
| B – Edu | `HU-34: programar publicaciones en Spring` · `HU-34: formulario para programar con fecha y hora` · `HU-34: pruebas de fechas válidas y pasadas` |
| C – Jose | `HU-43: endpoint para renovar la sesión en Spring Boot` · `HU-43: renovación automática de la sesión en la app` · `HU-43: pruebas de la renovación y de la cuenta desactivada` |

## Viernes 6

| Persona | Commits |
|---|---|
| A – Karim | `HU-30: selector de colores de la marca` · `HU-30: validar y guardar la paleta en Spring` · `HU-30: vista previa de los colores en un anuncio` · `HU-30: pruebas de colores válidos e inválidos` |
| B – Edu | `HU-36: arrastrar y soltar en el calendario` · `HU-36: actualizar la fecha reprogramada en Spring` · `HU-36: aviso y regreso si la fecha no es válida` · `HU-36: pruebas de reprogramar` |
| C – Jose | `HU-41: pantalla de datos de la empresa en la app` · `HU-41: conectar con los datos de la cuenta` · `HU-41: validar RUC, teléfono y campos obligatorios` · `HU-41: pruebas de editar la empresa desde el celular` |

## Sábado 7

| Persona | Commits |
|---|---|
| A – Karim | `HU-31: editor de tipografías y voz de marca` · `HU-31: guardar la identidad visual del kit` · `HU-31: usar el kit de marca en el prompt de la IA` · `HU-31: pruebas de anuncios con la identidad guardada` |
| B – Edu | `HU-39: plantilla del informe en Word` · `HU-39: generar el informe en .docx` |
| C – Jose | `HU-44: pantalla del perfil del negocio en la app` · `HU-44: conectar con el perfil del negocio` · `HU-44: validar los campos obligatorios del perfil` · `HU-44: pruebas de editar el perfil desde el celular` |

**Al final del sábado:** A le pasa a C el formato del kit de marca (logo, colores y tipografías).

## Domingo 8

| Persona | Commits |
|---|---|
| A – Karim | `HU-38: plantilla del informe en PDF` · `HU-38: generar el informe en PDF` |
| B – Edu | `HU-39: botón Descargar Word` · `HU-39: pruebas del documento generado` · `HU-40: selector de periodo y campaña` |
| C – Jose | `HU-45: endpoint del anuncio con su kit de marca en Spring Boot` · `HU-45: pantalla de previsualización en la app` |

## Lunes 9 – cierre del sprint

| Persona | Commits |
|---|---|
| A – Karim | `HU-38: botón Descargar PDF` · `HU-38: pruebas del contenido del PDF` |
| B – Edu | `HU-40: filtro por periodo y campaña en el informe` · `HU-40: aplicar el filtro a las descargas en PDF y Word` · `HU-40: pruebas de periodos válidos e inválidos` |
| C – Jose | `HU-45: aplicar colores, tipografía y logo del kit` · `HU-45: pruebas de la previsualización con y sin kit` |

**Cierre (lunes en la tarde):**
1. Cada uno corre todas sus pruebas y sube su rama.
2. Cada uno abre su **Pull Request a `main`** con "Create a merge commit".
3. Se unen en orden: **A → B → C**, porque B usa el informe de A y C usa el kit de A.
4. Se actualiza `CONTEXTO.md` con las HU del Sprint 3, las migraciones nuevas y la app móvil.

## Resumen de commits por día

| Día | A – Karim | B – Edu | C – Jose | Total |
|---|---|---|---|---|
| Martes 3 | 2 | 2 | 2 | 6 |
| Miércoles 4 | 3 | 3 | 3 | 9 |
| Jueves 5 | 3 | 3 | 3 | 9 |
| Viernes 6 | 4 | 4 | 4 | 12 |
| Sábado 7 | 4 | 2 | 4 | 10 |
| Domingo 8 | 2 | 3 | 2 | 7 |
| Lunes 9 | 2 | 3 | 2 | 7 |
| **Total** | **20** | **20** | **20** | **60** |
