// View: formulario para pedir el anuncio y tarjeta con el resultado. La lógica está en useGenerarAnuncio.
import { NOMBRES_TONO, REDES } from '../models/anunciosApi'
import { MAXIMO_OFERTA, useGenerarAnuncio } from '../viewmodels/useGenerarAnuncio'
import './GenerarAnuncioView.css'

export default function GenerarAnuncioView({ irAPerfil }) {
  const { solicitud, errores, anuncio, aviso, faltaPerfil, generando, regenerando, copiado, cambiarCampo, generar, regenerar, copiar } =
    useGenerarAnuncio()

  // HU 13: si el filtro lo marcó como dudoso, no se puede copiar para publicarlo.
  const enRevision = anuncio?.estado === 'DUDOSO'
  const cargando = generando || regenerando

  function alEnviar(evento) {
    evento.preventDefault()
    generar()
  }

  return (
    <main className="pagina pagina-anuncio">
      <header className="pagina-cabecera">
        <h1>Crear anuncio con IA</h1>
        <p>Escribe tu oferta y la IA arma el texto con el tono de tu marca.</p>
      </header>

      {faltaPerfil && (
        <div className="aviso aviso-error aviso-con-accion">
          <span>Primero completa el perfil de tu negocio para que la IA sepa cómo escribir.</span>
          <button type="button" className="boton-secundario" onClick={irAPerfil}>
            Completar mi perfil
          </button>
        </div>
      )}
      {aviso && <div className="aviso aviso-error">{aviso}</div>}

      <div className="anuncio-columnas">
        <form className="tarjeta formulario" onSubmit={alEnviar} noValidate>
          <label className={errores.oferta ? 'campo campo-con-error' : 'campo'}>
            <span>¿Qué quieres anunciar?</span>
            <textarea
              rows="4"
              value={solicitud.oferta}
              placeholder="2 panetones por S/ 35 hasta el 24 de diciembre"
              onChange={(e) => cambiarCampo('oferta', e.target.value)}
            />
            {errores.oferta ? (
              <small className="campo-error">{errores.oferta}</small>
            ) : (
              <small className="campo-ayuda">
                {solicitud.oferta.length} / {MAXIMO_OFERTA}
              </small>
            )}
          </label>

          <fieldset className="redes">
            <legend>Red social</legend>
            <div className="redes-opciones">
              {REDES.map((red) => (
                <label key={red.valor} className={solicitud.redSocial === red.valor ? 'red red-activa' : 'red'}>
                  <input
                    type="radio"
                    name="redSocial"
                    value={red.valor}
                    checked={solicitud.redSocial === red.valor}
                    onChange={(e) => cambiarCampo('redSocial', e.target.value)}
                  />
                  {red.nombre}
                </label>
              ))}
            </div>
            {errores.redSocial && <small className="campo-error">{errores.redSocial}</small>}
          </fieldset>

          <button className="boton-ia" type="submit" disabled={cargando}>
            {cargando ? 'Generando...' : '✦ Generar con IA'}
          </button>
        </form>

        <section className="tarjeta resultado" aria-live="polite">
          <div className="resultado-cabecera">
            <h2>Tu anuncio</h2>
            {anuncio && <span className="etiqueta-ia">✦ Generado con IA</span>}
          </div>

          {cargando && <p className="resultado-vacio">La IA está escribiendo tu anuncio...</p>}

          {!cargando && !anuncio && (
            <p className="resultado-vacio">Aquí aparecerá el texto listo para copiar y pegar en tu red social.</p>
          )}

          {!cargando && anuncio && (
            <>
              {enRevision && (
                <div className="aviso aviso-revision">
                  <strong>Tu anuncio quedó en revisión.</strong> El filtro automático encontró algo que un
                  moderador debe revisar antes de que puedas publicarlo.
                  <span className="aviso-motivo">Motivo: {anuncio.motivoRevision}</span>
                </div>
              )}
              <p className={enRevision ? 'resultado-texto resultado-texto-revision' : 'resultado-texto'}>
                {anuncio.texto}
              </p>
              <div className="resultado-datos">
                <span>Tono: {NOMBRES_TONO[anuncio.tono]}</span>
                <span>{REDES.find((red) => red.valor === anuncio.redSocial)?.nombre}</span>
                {anuncio.simulado && <span className="etiqueta-simulado">Respuesta simulada</span>}
              </div>
              <div className="resultado-acciones">
                {!enRevision && (
                  <button type="button" className="boton-primario" onClick={copiar}>
                    {copiado ? '¡Copiado!' : 'Copiar texto'}
                  </button>
                )}
                <button type="button" className="boton-secundario" onClick={generar} disabled={cargando}>
                  Generar otra versión
                </button>
                {/* HU 12: Botón para regenerar con los mismos parámetros */}
                <button
                  type="button"
                  className="boton-secundario"
                  onClick={regenerar}
                  disabled={cargando}
                  title="Regenerar con los mismos parámetros"
                >
                  {regenerando ? '🔄 Regenerando...' : '🔄 Regenerar'}
                </button>
              </div>
            </>
          )}
        </section>
      </div>
    </main>
  )
}
