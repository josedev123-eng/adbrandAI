// View: solo dibuja el perfil. La lógica está en usePerfilNegocio.
// Con perfil guardado se ve el resumen; el botón "Editar" abre el formulario.
import Aviso from '../../../shared/components/Aviso'
import Cargando from '../../../shared/components/Cargando'
import { RUBROS, TONOS } from '../models/perfilNegocioApi'
import { MAXIMO_DESCRIPCION, usePerfilNegocio } from '../viewmodels/usePerfilNegocio'
import './PerfilNegocioView.css'

function Campo({ etiqueta, error, ayuda, children }) {
  return (
    <label className={error ? 'campo campo-con-error' : 'campo'}>
      <span>{etiqueta}</span>
      {children}
      {error ? <small className="campo-error">{error}</small> : ayuda && <small className="campo-ayuda">{ayuda}</small>}
    </label>
  )
}

// Resumen de lo que ya está guardado en la base de datos.
function ResumenNegocio({ guardado, alEditar }) {
  const tono = TONOS.find((t) => t.valor === guardado.tono)?.nombre ?? guardado.tono
  return (
    <section className="tarjeta resumen" aria-label="Tu negocio">
      <div className="datos-lectura-cabecera">
        <div>
          <span className="resumen-etiqueta">Tu negocio</span>
          <h2 className="resumen-nombre">{guardado.nombreComercial}</h2>
        </div>
        <button type="button" className="boton-secundario" onClick={alEditar}>
          ✎ Editar
        </button>
      </div>
      <dl className="resumen-datos">
        <div>
          <dt>Rubro</dt>
          <dd>{guardado.rubro}</dd>
        </div>
        <div>
          <dt>Tono</dt>
          <dd>{tono}</dd>
        </div>
        <div className="resumen-ancho">
          <dt>Público objetivo</dt>
          <dd>{guardado.publicoObjetivo}</dd>
        </div>
        {guardado.descripcion && (
          <div className="resumen-ancho">
            <dt>Qué vende</dt>
            <dd>{guardado.descripcion}</dd>
          </div>
        )}
      </dl>
    </section>
  )
}

export default function PerfilNegocioView() {
  const {
    perfil,
    guardado,
    errores,
    mensaje,
    cargando,
    guardando,
    existe,
    editando,
    empezarEdicion,
    cancelarEdicion,
    cambiarCampo,
    guardar,
  } = usePerfilNegocio()

  function alEnviar(evento) {
    evento.preventDefault()
    guardar()
  }

  if (cargando) {
    return (
      <main className="pagina">
        <Cargando texto="Cargando tu perfil..." />
      </main>
    )
  }

  return (
    <main className="pagina">
      <header className="pagina-cabecera">
        <h1>Perfil del negocio</h1>
        <p>La IA usa estos datos para escribir anuncios con la voz de tu marca.</p>
      </header>

      <Aviso mensaje={mensaje} />

      {guardado && !editando && <ResumenNegocio guardado={guardado} alEditar={empezarEdicion} />}

      {editando && (
        <form className="tarjeta formulario" onSubmit={alEnviar} noValidate aria-label="Editar perfil del negocio">
          <h2 className="formulario-titulo">{existe ? 'Editar perfil del negocio' : 'Completa el perfil de tu negocio'}</h2>
          <div className="fila">
            <Campo etiqueta="Nombre comercial" error={errores.nombreComercial}>
              <input
                value={perfil.nombreComercial}
                maxLength={120}
                placeholder="Ej.: Panadería Doña Rosa"
                onChange={(e) => cambiarCampo('nombreComercial', e.target.value)}
              />
            </Campo>
            <Campo etiqueta="Rubro" error={errores.rubro}>
              <select value={perfil.rubro} onChange={(e) => cambiarCampo('rubro', e.target.value)}>
                <option value="">Selecciona tu rubro</option>
                {RUBROS.map((rubro) => (
                  <option key={rubro} value={rubro}>
                    {rubro}
                  </option>
                ))}
              </select>
            </Campo>
          </div>

          <Campo etiqueta="Público objetivo" error={errores.publicoObjetivo} ayuda="¿A quién le quieres vender?">
            <input
              value={perfil.publicoObjetivo}
              maxLength={300}
              placeholder="Ej.: Familias del barrio y oficinistas que pasan temprano"
              onChange={(e) => cambiarCampo('publicoObjetivo', e.target.value)}
            />
          </Campo>

          <fieldset className={errores.tono ? 'tonos campo-con-error' : 'tonos'}>
            <legend>Tono de tu marca</legend>
            <div className="tonos-opciones">
              {TONOS.map((tono) => (
                <label key={tono.valor} className={perfil.tono === tono.valor ? 'tono tono-activo' : 'tono'}>
                  <input
                    type="radio"
                    name="tono"
                    value={tono.valor}
                    checked={perfil.tono === tono.valor}
                    onChange={(e) => cambiarCampo('tono', e.target.value)}
                  />
                  <b>{tono.nombre}</b>
                  <small>{tono.ejemplo}</small>
                </label>
              ))}
            </div>
            {errores.tono && <small className="campo-error">{errores.tono}</small>}
          </fieldset>

          <Campo
            etiqueta="¿Qué vendes? (opcional)"
            error={errores.descripcion}
            ayuda={`${perfil.descripcion.length} / ${MAXIMO_DESCRIPCION}`}
          >
            <textarea
              rows="3"
              value={perfil.descripcion}
              maxLength={500}
              placeholder="Ej.: Pan artesanal recién horneado, tortas por encargo y panetones de campaña."
              onChange={(e) => cambiarCampo('descripcion', e.target.value)}
            />
          </Campo>

          <div className="formulario-acciones">
            {existe && (
              <button type="button" className="boton-secundario" onClick={cancelarEdicion} disabled={guardando}>
                Cancelar
              </button>
            )}
            <button className="boton-primario" type="submit" disabled={guardando}>
              {guardando ? 'Guardando...' : existe ? 'Guardar cambios' : 'Guardar perfil'}
            </button>
          </div>
        </form>
      )}
    </main>
  )
}
