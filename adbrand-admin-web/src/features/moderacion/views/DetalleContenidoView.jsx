// View: detalle completo de un contenido dudoso y el motivo de la observación (HU-14, criterio 2).
import { formatearFecha, NOMBRES_ESTADO, NOMBRES_RED, NOMBRES_TONO } from '../models/moderacionApi'
import { useDetalleContenido } from '../viewmodels/useDetalleContenido'
import './DetalleContenidoView.css'

function Dato({ etiqueta, children }) {
  return (
    <div className="dato">
      <dt>{etiqueta}</dt>
      <dd>{children}</dd>
    </div>
  )
}

export default function DetalleContenidoView({ id, alVolver }) {
  const { contenido, error, cargando } = useDetalleContenido(id)

  return (
    <section className="tarjeta detalle">
      <button type="button" className="boton-volver" onClick={alVolver}>
        ← Volver a la bandeja
      </button>

      {cargando && <p className="bandeja-vacia">Cargando detalle...</p>}
      {error && <div className="aviso aviso-error">{error}</div>}

      {contenido && (
        <>
          <div className="detalle-cabecera">
            <h2>{contenido.oferta}</h2>
            <span className={`etiqueta-estado estado-${contenido.estado.toLowerCase()}`}>
              {NOMBRES_ESTADO[contenido.estado] ?? contenido.estado}
            </span>
          </div>

          <div className="detalle-motivo">
            <strong>Motivo de la observación</strong>
            <p>{contenido.motivo_revision ?? 'Sin observaciones.'}</p>
          </div>

          <div>
            <h3 className="detalle-subtitulo">Texto generado por la IA</h3>
            <p className="detalle-texto">{contenido.texto}</p>
          </div>

          <dl className="detalle-datos">
            <Dato etiqueta="Negocio">
              {contenido.negocio ? `${contenido.negocio.nombre} (${contenido.negocio.rubro})` : 'Sin perfil registrado'}
            </Dato>
            <Dato etiqueta="Red social">{NOMBRES_RED[contenido.red_social] ?? contenido.red_social}</Dato>
            <Dato etiqueta="Tono">{NOMBRES_TONO[contenido.tono] ?? contenido.tono}</Dato>
            <Dato etiqueta="Generado">{formatearFecha(contenido.fecha_creacion)}</Dato>
            <Dato etiqueta="Código">#{contenido.id}</Dato>
          </dl>
        </>
      )}
    </section>
  )
}
