// View: editor de los colores de la marca con selector (HU-30). La lógica está en useEditarColores.
// Usa los estilos del editor de la HU-32 (EditarKitMarcaView.css) para que se vean igual.
import { useEditarColores } from '../viewmodels/useEditarColores'
import './EditarKitMarcaView.css'
import './EditarColoresView.css'

export default function EditarColoresView({ kit, alGuardar, alCancelar }) {
  const {
    colores,
    coloresValidos,
    errores,
    mensaje,
    guardando,
    puedeAgregar,
    cambiarColor,
    agregarColor,
    quitarColor,
    guardar,
  } = useEditarColores(kit, alGuardar)

  function alEnviar(evento) {
    evento.preventDefault()
    guardar()
  }

  return (
    <main className="pagina pagina-colores">
      <header className="pagina-cabecera">
        <button type="button" className="boton-volver" onClick={alCancelar}>← Volver al kit</button>
        <h1>Colores de la marca</h1>
        <p className="kit-meta">
          <strong>{kit.nombreNegocio}</strong> · Cada paleta que guardes queda como una versión nueva de tu kit.
        </p>
      </header>

      {mensaje && <div className={`aviso aviso-${mensaje.tipo}`}>{mensaje.texto}</div>}

      <div className="colores-editor">
        <form className="tarjeta formulario" onSubmit={alEnviar} noValidate>
          <fieldset className="editar-colores">
            <legend>Paleta (hasta 8 colores)</legend>

            <div className="editar-vista-previa" aria-label="Paleta">
              {coloresValidos.map((hex, i) => (
                <span key={i} className="editar-franja" style={{ background: hex }} title={hex} />
              ))}
            </div>

            {colores.map((color, i) => {
              const error = errores[`colores[${i}].hex`]
              return (
                <div key={i} className={error ? 'editar-color campo-con-error' : 'editar-color'}>
                  <input
                    type="color"
                    className="editar-selector"
                    aria-label={`Elegir color ${i + 1}`}
                    value={/^#[0-9A-Fa-f]{6}$/.test(color.hex.trim()) ? color.hex.trim() : '#000000'}
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
                    placeholder="Rojo horno - Ofertas"
                    onChange={(e) => cambiarColor(i, 'nombre', e.target.value)}
                  />
                  <button
                    type="button"
                    className="editar-quitar"
                    onClick={() => quitarColor(i)}
                    disabled={colores.length <= 1}
                    aria-label={`Quitar color ${i + 1}`}
                  >
                    ✕
                  </button>
                  {error && <small className="campo-error editar-error">{error}</small>}
                </div>
              )
            })}

            {puedeAgregar && (
              <button type="button" className="boton-secundario editar-agregar" onClick={agregarColor}>
                + Agregar color
              </button>
            )}
          </fieldset>

          <div className="formulario-acciones">
            <button type="submit" className="boton-primario" disabled={guardando}>
              {guardando ? 'Guardando...' : 'Guardar colores'}
            </button>
            <button type="button" className="boton-secundario" onClick={alCancelar} disabled={guardando}>
              Cancelar
            </button>
          </div>
        </form>
      </div>
    </main>
  )
}
