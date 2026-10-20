// View: editar el kit de marca a mano (HU-32). La lógica está en useEditarKitMarca.
import { MAXIMO_COLORES, useEditarKitMarca } from '../viewmodels/useEditarKitMarca'
import './EditarKitMarcaView.css'

const COLOR_VALIDO = /^#[0-9A-Fa-f]{6}$/

function Campo({ etiqueta, error, ayuda, children }) {
  return (
    <label className={error ? 'campo campo-con-error' : 'campo'}>
      <span>{etiqueta}</span>
      {children}
      {error ? <small className="campo-error">{error}</small> : ayuda && <small className="campo-ayuda">{ayuda}</small>}
    </label>
  )
}

function fechaCorta(fecha) {
  return new Date(fecha).toLocaleString('es-PE', { dateStyle: 'medium', timeStyle: 'short' })
}

export default function EditarKitMarcaView({ kit, alGuardar, alCancelar }) {
  const {
    formulario,
    errores,
    mensaje,
    guardando,
    versiones,
    cambiarCampo,
    cambiarColor,
    agregarColor,
    quitarColor,
    guardar,
  } = useEditarKitMarca(kit, alGuardar)

  function alEnviar(evento) {
    evento.preventDefault()
    guardar()
  }

  return (
    <main className="pagina pagina-kit-editar">
      <header className="pagina-cabecera">
        <button type="button" className="boton-volver" onClick={alCancelar}>← Volver al kit</button>
        <h1>Editar Kit de Marca</h1>
        <p className="kit-meta">
          <strong>{kit.nombreNegocio}</strong> · {kit.sector}
        </p>
      </header>

      {mensaje && <div className={`aviso aviso-${mensaje.tipo}`}>{mensaje.texto}</div>}

      <form className="tarjeta formulario" onSubmit={alEnviar} noValidate>
        <fieldset className="editar-colores">
          <legend>Colores</legend>

          <div className="editar-vista-previa" aria-label="Vista previa de la paleta">
            {formulario.colores.map((color, i) => (
              <span
                key={i}
                className="editar-franja"
                style={{ background: COLOR_VALIDO.test(color.hex.trim()) ? color.hex.trim() : 'transparent' }}
                title={color.hex}
              />
            ))}
          </div>

          {formulario.colores.map((color, i) => {
            const error = errores[`colores[${i}].hex`]
            return (
              <div key={i} className={error ? 'editar-color campo-con-error' : 'editar-color'}>
                <input
                  type="color"
                  className="editar-selector"
                  aria-label={`Elegir color ${i + 1}`}
                  value={COLOR_VALIDO.test(color.hex.trim()) ? color.hex.trim() : '#000000'}
                  onChange={(e) => cambiarColor(i, 'hex', e.target.value.toUpperCase())}
                />
                <input
                  className="editar-hex"
                  aria-label={`Código del color ${i + 1}`}
                  value={color.hex}
                  maxLength={7}
                  placeholder="#2E7D32"
                  onChange={(e) => cambiarColor(i, 'hex', e.target.value)}
                />
                <input
                  className="editar-nombre"
                  aria-label={`Nombre o uso del color ${i + 1}`}
                  value={color.nombre}
                  maxLength={100}
                  placeholder="Verde bosque - Confianza"
                  onChange={(e) => cambiarColor(i, 'nombre', e.target.value)}
                />
                <button
                  type="button"
                  className="editar-quitar"
                  onClick={() => quitarColor(i)}
                  disabled={formulario.colores.length <= 1}
                  aria-label={`Quitar color ${i + 1}`}
                >
                  ✕
                </button>
                {error && <small className="campo-error editar-error">{error}</small>}
              </div>
            )
          })}

          {formulario.colores.length < MAXIMO_COLORES && (
            <button type="button" className="boton-secundario editar-agregar" onClick={agregarColor}>
              + Agregar color
            </button>
          )}
        </fieldset>

        <div className="fila">
          <Campo etiqueta="Tipografía principal" error={errores.tipografiaPrimaria}>
            <input
              value={formulario.tipografiaPrimaria}
              placeholder="Bricolage Grotesque"
              onChange={(e) => cambiarCampo('tipografiaPrimaria', e.target.value)}
            />
          </Campo>
          <Campo etiqueta="Tipografía secundaria (opcional)" error={errores.tipografiaSecundaria}>
            <input
              value={formulario.tipografiaSecundaria}
              placeholder="Plus Jakarta Sans"
              onChange={(e) => cambiarCampo('tipografiaSecundaria', e.target.value)}
            />
          </Campo>
        </div>

        <Campo etiqueta="Concepto del logotipo" error={errores.logoConcepto}>
          <textarea
            rows="4"
            value={formulario.logoConcepto}
            placeholder="Una canasta sencilla con una hoja verde encima..."
            onChange={(e) => cambiarCampo('logoConcepto', e.target.value)}
          />
        </Campo>

        <div className="formulario-acciones">
          <button type="button" className="boton-secundario" onClick={alCancelar} disabled={guardando}>
            Cancelar
          </button>
          <button type="submit" className="boton-primario" disabled={guardando}>
            {guardando ? 'Guardando...' : 'Guardar cambios'}
          </button>
        </div>
      </form>

      {versiones.length > 0 && (
        <section className="tarjeta editar-historial" aria-label="Historial de cambios">
          <h2 className="subtitulo">Historial de cambios</h2>
          <ul>
            {versiones.map((version) => (
              <li key={version.id}>
                <span>{fechaCorta(version.fechaCreacion)}</span>
                <span className="editar-historial-colores">
                  {(version.paletaColores ?? '').split('\n').filter(Boolean).map((linea, i) => (
                    <span key={i} style={{ background: linea.split(' - ')[0].trim() }} />
                  ))}
                </span>
                <span className="editar-historial-tipo">{version.tipografiaPrimaria}</span>
              </li>
            ))}
          </ul>
        </section>
      )}
    </main>
  )
}
