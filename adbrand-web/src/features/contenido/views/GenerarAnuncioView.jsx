// View: "Generar → Elegir". Formulario de la oferta, 4 opciones (texto + imagen) para elegir
// y la tarjeta del anuncio elegido. La lógica está en useGenerarAnuncio.
import { NOMBRES_TONO, REDES } from '../models/anunciosApi'
import { MAXIMO_OFERTA, useGenerarAnuncio } from '../viewmodels/useGenerarAnuncio'
import './GenerarAnuncioView.css'

function TarjetaOpcion({ opcion, eligiendoId, alElegir }) {
  const eligiendo = eligiendoId === opcion.id
  return (
    <article className={opcion.elegida ? 'opcion opcion-elegida' : 'opcion'} aria-label={`Opción ${opcion.numero}`}>
      <div className="opcion-imagen">
        <img src={opcion.imagen} loading="lazy" decoding="async" alt={`Imagen de la opción ${opcion.numero} (${opcion.estilo})`} />
        <span className="opcion-estilo">{opcion.estilo}</span>
        {opcion.elegida && <span className="opcion-marca">✓ Elegida</span>}
      </div>
      <p className="opcion-texto">{opcion.texto}</p>
      <div className="opcion-pie">
        {(opcion.imagenSimulada || opcion.textoSimulado) && <span className="etiqueta-simulado">De prueba</span>}
        <button
          type="button"
          className={opcion.elegida ? 'boton-secundario' : 'boton-primario'}
          onClick={() => alElegir(opcion.id)}
          disabled={eligiendoId !== null}
        >
          {eligiendo ? 'Guardando...' : opcion.elegida ? 'Elegida' : 'Elegir esta'}
        </button>
      </div>
    </article>
  )
}

export default function GenerarAnuncioView({ irAPerfil, irAPlanes }) {
  const {
    solicitud,
    errores,
    opciones,
    anuncio,
    opcionElegida,
    aviso,
    faltaPerfil,
    sinCupo,
    generando,
    eligiendoId,
    regenerando,
    copiado,
    instruccion,
    errorInstruccion,
    historial,
    retocando,
    cambiarCampo,
    cambiarInstruccion,
    retocar,
    volverAVersion,
    generar,
    elegir,
    regenerar,
    copiar,
  } = useGenerarAnuncio()

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
        <span className="anuncio-kicker">TU ESPACIO CREATIVO</span>
        <h1>Crear anuncio con IA</h1>
        <p>Escribe tu oferta, la IA te propone 4 opciones con texto e imagen y tú eliges la que más te gusta.</p>
      </header>

      <section className="anuncio-bienvenida" aria-label="Herramientas de creación">
        <div className="anuncio-bienvenida-icono" aria-hidden="true">✦</div>
        <div>
          <strong>Una idea puede convertirse en tu próxima campaña.</strong>
          <p>Elige una red, cuéntanos tu oferta y compara 4 propuestas con el estilo de tu marca.</p>
        </div>
        <span className="anuncio-bienvenida-etiqueta">Listo para crear</span>
      </section>

      {faltaPerfil && (
        <div className="aviso aviso-error aviso-con-accion">
          <span>Primero completa el perfil de tu negocio para que la IA sepa cómo escribir.</span>
          <button type="button" className="boton-secundario" onClick={irAPerfil}>
            Completar mi perfil
          </button>
        </div>
      )}
      {sinCupo && (
        <div className="aviso aviso-error aviso-con-accion" role="alert">
          <span>{sinCupo}</span>
          <button type="button" className="boton-primario" onClick={irAPlanes}>
            Ver planes
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
              maxLength={300}
              placeholder="Ej.: 2 panetones por S/ 35 hasta el 24 de diciembre"
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

          <button className="boton-ia" type="submit" disabled={cargando || eligiendoId !== null}>
            {generando ? 'Creando 4 opciones...' : '✦ Generar 4 opciones'}
          </button>
        </form>

        <section className="tarjeta resultado" aria-live="polite">
          <div className="resultado-cabecera">
            <h2>Tu anuncio</h2>
            {anuncio && <span className="etiqueta-ia">✦ Generado con IA</span>}
          </div>

          {!anuncio && (
            <p className="resultado-vacio">
              {opciones.length > 0
                ? 'Elige una de las 4 opciones de abajo y aquí aparecerá tu anuncio listo para usar.'
                : 'Aquí aparecerá el anuncio que elijas, con su imagen y su texto listo para copiar.'}
            </p>
          )}

          {anuncio && (
            <>
              {opcionElegida && (
                <div className={retocando ? 'resultado-imagen-marco resultado-imagen-cargando' : 'resultado-imagen-marco'}>
                  <img className="resultado-imagen" src={opcionElegida.imagen} alt={`Imagen del anuncio (${opcionElegida.estilo})`} />
                  {retocando && <span className="resultado-imagen-aviso">La IA está retocando tu imagen...</span>}
                </div>
              )}

              {/* "Retocar con prompts cortos": cada instrucción crea una versión nueva de la imagen */}
              {opcionElegida && (
                <form
                  className="retoque"
                  onSubmit={(evento) => {
                    evento.preventDefault()
                    retocar()
                  }}
                  noValidate
                >
                  <label className={errorInstruccion ? 'campo campo-con-error' : 'campo'}>
                    <span>¿Qué quieres cambiar de la imagen?</span>
                    <div className="retoque-fila">
                      <input
                        type="text"
                        value={instruccion}
                        maxLength={300}
                        placeholder="Ej.: pon el fondo más claro"
                        onChange={(e) => cambiarInstruccion(e.target.value)}
                        disabled={retocando}
                      />
                      <button type="submit" className="boton-ia" disabled={retocando}>
                        {retocando ? 'Retocando...' : '✦ Retocar'}
                      </button>
                    </div>
                    {errorInstruccion && <small className="campo-error">{errorInstruccion}</small>}
                    {!errorInstruccion && historial?.retoquesRestantes != null && (
                      <small className="campo-ayuda">
                        Te quedan {historial.retoquesRestantes} retoques este mes.
                      </small>
                    )}
                  </label>
                </form>
              )}

              {historial && historial.versiones.length > 1 && (
                <div className="versiones" aria-label="Versiones de la imagen">
                  <span className="versiones-titulo">Versiones</span>
                  <div className="versiones-lista">
                    {historial.versiones.map((version) => (
                      <button
                        key={version.numero}
                        type="button"
                        className={version.actual ? 'version version-actual' : 'version'}
                        onClick={() => volverAVersion(version.numero)}
                        disabled={retocando || version.actual}
                        title={version.instruccion ?? 'Imagen original'}
                        aria-label={`Usar la versión ${version.numero}${version.instruccion ? `: ${version.instruccion}` : ' (original)'}`}
                      >
                        <img src={version.imagen} alt="" loading="lazy" decoding="async" />
                        <span>v{version.numero}</span>
                      </button>
                    ))}
                  </div>
                </div>
              )}
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
                {/* HU 12: regenerar el texto con los mismos parámetros */}
                <button
                  type="button"
                  className="boton-secundario"
                  onClick={regenerar}
                  disabled={cargando}
                  title="Regenerar el texto con los mismos parámetros"
                >
                  {regenerando ? '🔄 Regenerando...' : '🔄 Regenerar texto'}
                </button>
              </div>
            </>
          )}
        </section>
      </div>

      {generando && (
        <section className="opciones" aria-label="Creando opciones">
          {[1, 2, 3, 4].map((numero) => (
            <div key={numero} className="opcion opcion-cargando" aria-hidden="true">
              <div className="opcion-imagen" />
              <span className="opcion-linea" />
              <span className="opcion-linea opcion-linea-corta" />
            </div>
          ))}
          <p className="opciones-ayuda">La IA está escribiendo los textos y dibujando las imágenes...</p>
        </section>
      )}

      {!generando && opciones.length > 0 && (
        <section className="opciones-bloque" aria-label="Opciones de anuncio">
          <div className="opciones-cabecera">
            <h2>Elige tu opción favorita</h2>
            <p>Cada opción tiene un texto y un estilo de imagen distinto.</p>
          </div>
          <div className="opciones">
            {opciones.map((opcion) => (
              <TarjetaOpcion key={opcion.id} opcion={opcion} eligiendoId={eligiendoId} alElegir={elegir} />
            ))}
          </div>
        </section>
      )}
    </main>
  )
}
