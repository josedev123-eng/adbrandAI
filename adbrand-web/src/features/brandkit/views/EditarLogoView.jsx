// View: editor interactivo del logo con vista previa en vivo (HU-29). La lógica está en useEditarLogo.
import { FORMAS_LOGO, TIPOGRAFIAS_LOGO } from '../models/brandkitApi'
import { useEditarLogo } from '../viewmodels/useEditarLogo'
import LogoMarca from './LogoMarca'
import './EditarLogoView.css'

function Campo({ etiqueta, error, children }) {
  return (
    <label className={error ? 'campo campo-con-error' : 'campo'}>
      <span>{etiqueta}</span>
      {children}
      {error && <small className="campo-error">{error}</small>}
    </label>
  )
}

function CampoColor({ etiqueta, valor, error, alCambiar }) {
  return (
    <Campo etiqueta={etiqueta} error={error}>
      <span className="logo-color">
        {/* El selector solo acepta colores válidos; el texto permite escribir el código a mano. */}
        <input
          type="color"
          aria-label={`${etiqueta} (selector)`}
          value={/^#[0-9A-Fa-f]{6}$/.test(valor) ? valor : '#000000'}
          onChange={(e) => alCambiar(e.target.value.toUpperCase())}
        />
        <input type="text" value={valor} maxLength={7} onChange={(e) => alCambiar(e.target.value.trim())} />
      </span>
    </Campo>
  )
}

export default function EditarLogoView({ kit, alGuardar, alCancelar }) {
  const { diseno, vistaPrevia, errores, mensaje, guardando, cambiar, guardar } = useEditarLogo(kit, alGuardar)

  function alEnviar(evento) {
    evento.preventDefault()
    guardar()
  }

  return (
    <main className="pagina pagina-logo">
      <header className="pagina-cabecera">
        <button type="button" className="boton-volver" onClick={alCancelar}>← Volver al kit</button>
        <h1>Diseñar logo</h1>
        <p className="kit-meta">
          <strong>{kit.nombreNegocio}</strong> · Cada logo que guardes queda como una versión nueva de tu kit.
        </p>
      </header>

      {mensaje && <div className={`aviso aviso-${mensaje.tipo}`}>{mensaje.texto}</div>}

      <div className="logo-editor">
        <form className="tarjeta formulario" onSubmit={alEnviar} noValidate>
          <Campo etiqueta="Texto o iniciales" error={errores.texto}>
            <input type="text" value={diseno.texto} maxLength={20} onChange={(e) => cambiar('texto', e.target.value)} />
          </Campo>

          <fieldset className="logo-formas">
            <legend>Forma</legend>
            {FORMAS_LOGO.map((forma) => (
              <label key={forma.valor} className={diseno.forma === forma.valor ? 'logo-forma activa' : 'logo-forma'}>
                <input
                  type="radio"
                  name="forma"
                  value={forma.valor}
                  checked={diseno.forma === forma.valor}
                  onChange={() => cambiar('forma', forma.valor)}
                />
                {forma.nombre}
              </label>
            ))}
          </fieldset>

          <CampoColor
            etiqueta="Color de fondo"
            valor={diseno.colorFondo}
            error={errores.colorFondo}
            alCambiar={(valor) => cambiar('colorFondo', valor)}
          />
          <CampoColor
            etiqueta="Color del texto"
            valor={diseno.colorTexto}
            error={errores.colorTexto}
            alCambiar={(valor) => cambiar('colorTexto', valor)}
          />

          <Campo etiqueta="Tipografía" error={errores.tipografia}>
            <select value={diseno.tipografia} onChange={(e) => cambiar('tipografia', e.target.value)}>
              {TIPOGRAFIAS_LOGO.map((tipografia) => (
                <option key={tipografia} value={tipografia}>{tipografia}</option>
              ))}
            </select>
          </Campo>

          <div className="formulario-acciones">
            <button type="submit" className="boton-primario" disabled={guardando}>
              {guardando ? 'Guardando...' : 'Guardar logo'}
            </button>
            <button type="button" className="boton-secundario" onClick={alCancelar} disabled={guardando}>
              Cancelar
            </button>
          </div>
        </form>

        <section className="tarjeta logo-vista-previa" aria-label="Vista previa del logo">
          <h2>Vista previa</h2>
          <LogoMarca diseno={vistaPrevia} tamano={180} />
          <div className="logo-tamanos" aria-hidden="true">
            <LogoMarca diseno={vistaPrevia} tamano={64} />
            <LogoMarca diseno={vistaPrevia} tamano={32} />
          </div>
          <small>Así se verá en tus anuncios y como ícono pequeño.</small>
        </section>
      </div>
    </main>
  )
}
