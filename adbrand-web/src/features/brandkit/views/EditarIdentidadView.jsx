// View: editor de la identidad visual de la marca (HU-31). La lógica está en useEditarIdentidad.
// Criterio 1: al lado, un anuncio de ejemplo con las tipografías y la voz, que cambia mientras se editan.
// Criterio 2: lo guardado lo usan los anuncios que se generen después (Spring lo agrega al prompt).
import { leerPaleta, TIPOGRAFIAS_SUGERIDAS } from '../models/brandkitApi'
import { MAXIMO_TIPOGRAFIA, MAXIMO_VOZ, useEditarIdentidad } from '../viewmodels/useEditarIdentidad'
import AnuncioVistaPrevia from './AnuncioVistaPrevia'
import './EditarColoresView.css'
import './EditarIdentidadView.css'

const COLOR_VALIDO = /^#[0-9A-Fa-f]{6}$/

function CampoTipografia({ etiqueta, campo, valor, error, ayuda, alCambiar }) {
  return (
    <label className={error ? 'campo campo-con-error' : 'campo'}>
      <span>{etiqueta}</span>
      <input
        aria-label={etiqueta}
        value={valor}
        list="tipografias-sugeridas"
        maxLength={MAXIMO_TIPOGRAFIA}
        onChange={(e) => alCambiar(campo, e.target.value)}
        style={valor.trim() ? { fontFamily: `'${valor.trim()}', sans-serif` } : undefined}
      />
      {error && <small className="campo-error">{error}</small>}
      {ayuda && !error && <small className="campo-ayuda">{ayuda}</small>}
    </label>
  )
}

export default function EditarIdentidadView({ kit, alGuardar, alCancelar }) {
  const { identidad, errores, mensaje, guardando, cambiarCampo, guardar } = useEditarIdentidad(kit, alGuardar)
  const colores = leerPaleta(kit.paletaColores).map((color) => color.hex).filter((hex) => COLOR_VALIDO.test(hex))
  const tipografiaTexto = identidad.tipografiaSecundaria.trim() || identidad.tipografiaPrimaria.trim()

  function alEnviar(evento) {
    evento.preventDefault()
    guardar()
  }

  return (
    <main className="pagina pagina-colores">
      <header className="pagina-cabecera">
        <button type="button" className="boton-volver" onClick={alCancelar}>← Volver al kit</button>
        <h1>Identidad visual</h1>
        <p className="kit-meta">
          <strong>{kit.nombreNegocio}</strong> · Tus próximos anuncios se escribirán con esta voz de marca.
        </p>
      </header>

      {mensaje && <div className={`aviso aviso-${mensaje.tipo}`}>{mensaje.texto}</div>}

      <div className="colores-editor">
        <form className="tarjeta formulario" onSubmit={alEnviar} noValidate>
          <datalist id="tipografias-sugeridas">
            {TIPOGRAFIAS_SUGERIDAS.map((nombre) => <option key={nombre} value={nombre} />)}
          </datalist>

          <CampoTipografia
            etiqueta="Tipografía principal (títulos)"
            campo="tipografiaPrimaria"
            valor={identidad.tipografiaPrimaria}
            error={errores.tipografiaPrimaria}
            alCambiar={cambiarCampo}
          />
          <CampoTipografia
            etiqueta="Tipografía secundaria (textos)"
            campo="tipografiaSecundaria"
            valor={identidad.tipografiaSecundaria}
            error={errores.tipografiaSecundaria}
            ayuda="Opcional. Si la dejas vacía se usa la principal."
            alCambiar={cambiarCampo}
          />

          <label className={errores.vozMarca ? 'campo campo-con-error' : 'campo'}>
            <span>Voz de marca</span>
            <textarea
              aria-label="Voz de marca"
              rows={5}
              value={identidad.vozMarca}
              maxLength={MAXIMO_VOZ}
              placeholder="Ej.: Cercana y alegre, tutea al cliente y usa palabras sencillas."
              onChange={(e) => cambiarCampo('vozMarca', e.target.value)}
            />
            {errores.vozMarca && <small className="campo-error">{errores.vozMarca}</small>}
            {!errores.vozMarca && (
              <small className="campo-ayuda">{identidad.vozMarca.length}/{MAXIMO_VOZ} caracteres</small>
            )}
          </label>

          <div className="formulario-acciones">
            <button type="submit" className="boton-primario" disabled={guardando}>
              {guardando ? 'Guardando...' : 'Guardar identidad'}
            </button>
            <button type="button" className="boton-secundario" onClick={alCancelar} disabled={guardando}>
              Cancelar
            </button>
          </div>
        </form>

        <section className="tarjeta colores-vista-previa" aria-label="Vista previa de la identidad">
          <h2>Así se ve en un anuncio</h2>
          <AnuncioVistaPrevia
            colores={colores.length > 0 ? colores : ['#4F46E5']}
            nombreNegocio={kit.nombreNegocio}
            logoDiseno={kit.logoDiseno}
            tipografiaTitulo={identidad.tipografiaPrimaria.trim()}
            tipografiaTexto={tipografiaTexto}
          />
          <div className="identidad-voz">
            <strong>Así escribirá la IA tus anuncios</strong>
            <p>{identidad.vozMarca.trim() || 'Describe la voz de tu marca para verla aquí.'}</p>
          </div>
        </section>
      </div>
    </main>
  )
}
