// View: "Listo para publicar". El texto con hashtags para copiar, compartir por WhatsApp y el afiche con QR.
import { useListoParaPublicar } from '../viewmodels/useListoParaPublicar'
import ImagenConMarca from './ImagenConMarca'
import './ListoParaPublicar.css'

// HU 64: "Tu oferta, sin multas". Lo que conviene corregir según las reglas de publicidad (INDECOPI),
// con campos para completar hasta cuándo vale y cuántas unidades hay; se agregan solos al texto.
function RevisionLegal({ observaciones, condiciones, alCambiar }) {
  if (observaciones.length === 0) {
    return <p className="publicar-legal-ok">✓ Tu anuncio cumple las reglas básicas de publicidad de promociones.</p>
  }
  const pideFecha = observaciones.some((o) => o.codigo === 'FALTA_VIGENCIA')
  const pideStock = observaciones.some((o) => o.codigo === 'FALTA_STOCK')
  const completo = (!pideFecha || condiciones.hasta) && (!pideStock || condiciones.stock)
  return (
    <details className="publicar-legal" role="note">
      <summary>
        {completo ? '✓' : 'ⓘ'} Tu oferta, sin multas:{' '}
        {completo
          ? 'listo, las condiciones van en el texto y en la imagen.'
          : `${observaciones.length} ${observaciones.length === 1 ? 'sugerencia' : 'sugerencias'} de INDECOPI`}
      </summary>
      <ul>
        {observaciones.map((o) => (
          <li key={o.codigo}>
            <b>{o.titulo}.</b> {o.detalle}
          </li>
        ))}
      </ul>
      {(pideFecha || pideStock) && (
        <div className="publicar-legal-campos">
          {pideFecha && (
            <label className="campo">
              <span>Válido hasta</span>
              <input type="date" value={condiciones.hasta} onChange={(e) => alCambiar('hasta', e.target.value)} />
            </label>
          )}
          {pideStock && (
            <label className="campo">
              <span>Unidades disponibles</span>
              <input
                type="number"
                min="1"
                inputMode="numeric"
                placeholder="Ej.: 50"
                value={condiciones.stock}
                onChange={(e) => alCambiar('stock', e.target.value)}
              />
            </label>
          )}
        </div>
      )}
      <small>Se agregan solas al texto y, en letra chica, abajo de la imagen. Son recomendaciones, no una asesoría legal.</small>
    </details>
  )
}

// "irAMiCuenta" lleva a poner el celular si falta. El afiche y la imagen usan el logo, los colores y el eslogan del kit.
// "irAPlanes": con el plan gratis el video sale con candado y lleva a Planes.
export default function ListoParaPublicar({ anuncioId, irAMiCuenta, irAPlanes }) {
  const {
    publicacion,
    textoFinal,
    condiciones,
    cambiarCondicion,
    cargando,
    error,
    copiado,
    abrir,
    copiar,
    compartirWhatsapp,
    imprimirAfiche,
    creandoVideo,
    descargarVideo,
    subiendoEstado,
    avisoEstado,
    subirAEstado,
  } = useListoParaPublicar(anuncioId)

  if (!publicacion) {
    return (
      <div className="publicar">
        <button type="button" className="boton-primario" onClick={abrir} disabled={cargando}>
          {cargando ? 'Preparando…' : '📣 Listo para publicar'}
        </button>
        {error && <p className="campo-error">{error}</p>}
      </div>
    )
  }

  return (
    <section className="publicar publicar-abierto" aria-label="Listo para publicar">
      <h3>Listo para publicar</h3>
      <p className="publicar-ayuda">Copia el texto y pégalo en Instagram o Facebook, o envíalo directo por WhatsApp.</p>
      {/* HU 61: la imagen final con el logo, los colores y el eslogan del kit de marca. */}
      <ImagenConMarca anuncioId={anuncioId} texto={textoFinal} condiciones={condiciones} />
      <RevisionLegal observaciones={publicacion.observaciones ?? []} condiciones={condiciones} alCambiar={cambiarCondicion} />
      <pre className="publicar-texto">{textoFinal}</pre>
      <ul className="publicar-hashtags" aria-label="Hashtags">
        {publicacion.hashtags.map((h) => (
          <li key={h}>{h}</li>
        ))}
      </ul>
      <div className="publicar-acciones">
        <button type="button" className="boton-primario" onClick={copiar}>
          {copiado ? '¡Copiado!' : 'Copiar texto con hashtags'}
        </button>
        <button type="button" className="boton-whatsapp" onClick={compartirWhatsapp}>
          Enviar por WhatsApp
        </button>
        <button type="button" className="boton-whatsapp" onClick={subirAEstado} disabled={subiendoEstado}>
          {subiendoEstado ? 'Preparando tu estado…' : 'Subir a mi estado'}
        </button>
        {publicacion.videoDisponible === false ? (
          <button
            type="button"
            className="boton-secundario boton-bloqueado"
            onClick={irAPlanes}
            disabled={!irAPlanes}
            title="El video para Reels y Estados está en los planes de pago"
          >
            🔒 Video para Reels y Estados · <span>planes de pago</span>
          </button>
        ) : (
          <button type="button" className="boton-ia" onClick={descargarVideo} disabled={creandoVideo}>
            {creandoVideo ? '🎬 Creando tu video…' : '🎬 Video para Reels y Estados'}
          </button>
        )}
        {publicacion.tieneWhatsapp && (
          <button type="button" className="boton-secundario" onClick={imprimirAfiche}>
            🖨 Afiche con QR para tu local
          </button>
        )}
      </div>
      {!publicacion.tieneWhatsapp && (
        <p className="publicar-nota">
          Agrega tu celular en{' '}
          {irAMiCuenta ? (
            <button type="button" className="boton-enlace" onClick={irAMiCuenta}>
              Mi Cuenta
            </button>
          ) : (
            'Mi Cuenta'
          )}{' '}
          y tus anuncios llevarán un enlace para que te pidan por WhatsApp, y podrás imprimir un afiche con QR.
        </p>
      )}
      {creandoVideo && (
        <p className="publicar-ayuda">Armando tu video de 8 segundos con tu marca… suele tardar menos de medio minuto.</p>
      )}
      {avisoEstado && (
        <p className="publicar-nota" role="status">
          {avisoEstado}
        </p>
      )}
      {error && <p className="campo-error">{error}</p>}
    </section>
  )
}
