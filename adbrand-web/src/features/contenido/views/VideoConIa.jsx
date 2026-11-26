// View HU 66: botón "Video con IA" del plan Premium. Con otro plan sale con candado y lleva a Planes.
import { useVideoConIa } from '../viewmodels/useVideoConIa'

export default function VideoConIa({ anuncioId, irAPlanes }) {
  const { disponibilidad, video, creando, error, crear } = useVideoConIa(anuncioId)
  if (!disponibilidad) return null

  const quedan = Math.max(0, disponibilidad.limite - disponibilidad.usados)

  if (disponibilidad.motivo === 'PLAN') {
    return (
      <div className="video-ia">
        <button
          type="button"
          className="boton-secundario boton-bloqueado"
          onClick={irAPlanes}
          disabled={!irAPlanes}
          title="La IA anima la foto de tu anuncio: es del plan Premium"
        >
          🔒 Video con IA · <span>plan Premium</span>
        </button>
      </div>
    )
  }

  return (
    <div className="video-ia">
      <button type="button" className="boton-ia" onClick={crear} disabled={creando || quedan === 0}>
        {creando ? '✨ La IA está creando tu video…' : '✨ Video con IA'}
      </button>
      <p className="publicar-ayuda">
        {quedan === 0
          ? `Ya usaste tus ${disponibilidad.limite} videos con IA de este mes. Se renuevan el día 1.`
          : `La IA anima la foto de tu anuncio y le pone tu marca. Te quedan ${quedan} de ${disponibilidad.limite} este mes.`}
      </p>
      {creando && (
        <div className="video-ia-avance" role="status">
          <div className="video-ia-barra" style={{ width: `${Math.max(8, video?.progreso ?? 8)}%` }} />
          <span>Tarda de 1 a 3 minutos. Puedes seguir aquí; se descargará solo al terminar.</span>
        </div>
      )}
      {video?.estado === 'LISTO' && <p className="publicar-nota">¡Listo! Tu video con IA se descargó.</p>}
      {error && <p className="campo-error">{error}</p>}
    </div>
  )
}
