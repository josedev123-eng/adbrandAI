// View: detalle completo de un contenido dudoso y el motivo de la observación (HU-14, criterio 2).
// Botones para aprobar o rechazar el contenido (HU-15) con confirmación y motivo.
import { formatearFecha, NOMBRES_ESTADO, NOMBRES_RED, NOMBRES_TONO } from '../models/moderacionApi'
import { useDetalleContenido } from '../viewmodels/useDetalleContenido'
import { useState } from 'react'
import './DetalleContenidoView.css'

function Dato({ etiqueta, children }) {
  return (
    <div className="dato">
      <dt>{etiqueta}</dt>
      <dd>{children}</dd>
    </div>
  )

}

export default function DetalleContenidoView({ id, alVolver, onActualizado }) {
  const { contenido, error, cargando, aprobar, rechazar, moderando } = useDetalleContenido(id, onActualizado)
  const [moderadorId, setModeradorId] = useState('')
  const [mostrarRechazo, setMostrarRechazo] = useState(false)
  const [motivoRechazo, setMotivoRechazo] = useState('')

  const hayModerador = moderadorId.trim() !== ''

  const manejarAprobar = async (e) => {
    e.preventDefault()
    if (!hayModerador) return
    try {
      await aprobar(Number(moderadorId))
    } catch (_) {
      // El error ya se muestra en el viewmodel
    }
  }

  const abrirRechazo = () => {
    if (!hayModerador) return
    setMostrarRechazo(true)
    setMotivoRechazo('')
  }

  const manejarRechazarConfirmado = async (e) => {
    e.preventDefault()
    if (!hayModerador) return
    try {
      await rechazar(Number(moderadorId), motivoRechazo)
      setMostrarRechazo(false)
    } catch (_) {
      // El error ya se muestra en el viewmodel
    }
  }

  const cancelarRechazo = () => {
    setMostrarRechazo(false)
    setMotivoRechazo('')
  }

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
            {contenido.moderador_id && <Dato etiqueta="Moderado por">Admin #{contenido.moderador_id}</Dato>}
          </dl>

          {contenido.estado === 'DUDOSO' && !mostrarRechazo && (
            <div className="detalle-moderar">
              <div className="moderar-campos">
                <label htmlFor="moderadorId">
                  Tu ID de administrador:
                  <input
                    id="moderadorId"
                    type="number"
                    min="1"
                    step="1"
                    value={moderadorId}
                    onChange={(e) => setModeradorId(e.target.value)}
                    placeholder="Ej: 42"
                    required
                    disabled={moderando}
                  />
                </label>
              </div>
              <div className="moderar-botones">
                <button
                  type="button"
                  className="boton-primario"
                  onClick={manejarAprobar}
                  disabled={moderando || !hayModerador}
                >
                  {moderando ? 'Aprobando...' : '✓ Aprobar'}
                </button>
                <button
                  type="button"
                  className="boton-peligro"
                  onClick={abrirRechazo}
                  disabled={moderando || !hayModerador}
                >
                  ✗ Rechazar
                </button>
              </div>
            </div>
          )}

          {mostrarRechazo && (
            <div className="detalle-moderar rechazo-form">
              <div className="moderar-campos">
                <label htmlFor="moderadorIdRechazo">
                  Tu ID de administrador:
                  <input
                    id="moderadorIdRechazo"
                    type="number"
                    min="1"
                    step="1"
                    value={moderadorId}
                    onChange={(e) => setModeradorId(e.target.value)}
                    placeholder="Ej: 42"
                    required
                    disabled={moderando}
                  />
                </label>
              </div>
              <div className="moderar-campos">
                <label htmlFor="motivoRechazo">
                  Motivo de rechazo (obligatorio):
                  <textarea
                    id="motivoRechazo"
                    value={motivoRechazo}
                    onChange={(e) => setMotivoRechazo(e.target.value)}
                    placeholder="Explica por qué se rechaza el contenido..."
                    rows={3}
                    required
                    disabled={moderando}
                  />
                </label>
              </div>
              <div className="moderar-botones">
                <button
                  type="button"
                  className="boton-peligro"
                  onClick={manejarRechazarConfirmado}
                  disabled={moderando || !hayModerador || !motivoRechazo.trim()}
                >
                  {moderando ? 'Rechazando...' : 'Confirmar rechazo'}
                </button>
                <button
                  type="button"
                  className="boton-secundario"
                  onClick={cancelarRechazo}
                  disabled={moderando}
                >
                  Cancelar
                </button>
              </div>
            </div>
          )}
        </>
      )}
    </section>
  )
}
