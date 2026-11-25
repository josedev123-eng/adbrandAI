// View (HU 61): vista previa de la imagen final con la marca, en el formato elegido, para descargarla o compartirla.
import { FORMATOS_IMAGEN, useImagenConMarca } from '../viewmodels/useImagenConMarca'
import './ListoParaPublicar.css'
import '../../../shared/components/EstadosLista.css'

const PROPORCION = { CUADRADO: '1 / 1', VERTICAL: '9 / 16', HORIZONTAL: '1200 / 628' }

export default function ImagenConMarca({ anuncioId, texto, condiciones }) {
  const { formato, elegirFormato, url, cargando, error, aviso, descargar, compartir } = useImagenConMarca(
    anuncioId,
    condiciones,
  )

  return (
    <div className="imagen-marca">
      <div className="imagen-marca-formatos" role="group" aria-label="Formato de la imagen">
        {FORMATOS_IMAGEN.map((f) => (
          <button
            key={f.valor}
            type="button"
            className={formato === f.valor ? 'imagen-marca-formato imagen-marca-formato-activo' : 'imagen-marca-formato'}
            aria-pressed={formato === f.valor}
            onClick={() => elegirFormato(f.valor)}
          >
            {f.nombre} <small>({f.detalle})</small>
          </button>
        ))}
      </div>
      <div className={`imagen-marca-marco imagen-marca-${formato.toLowerCase()}`}>
        {url && <img src={url} alt="Vista previa del anuncio con tu logo, tus colores y tu eslogan" />}
        {cargando && !url && (
          <span
            className="esqueleto-bloque imagen-marca-esqueleto"
            style={{ aspectRatio: PROPORCION[formato] }}
            role="status"
            aria-label="Poniendo tu marca en la imagen…"
          />
        )}
        {error && <span className="campo-error">{error}</span>}
      </div>
      <div className="publicar-acciones">
        <button type="button" className="boton-primario" onClick={() => compartir(texto)} disabled={!url}>
          Compartir imagen con tu marca
        </button>
        <button type="button" className="boton-secundario" onClick={descargar} disabled={!url}>
          Descargar imagen
        </button>
      </div>
      {aviso && <p className="publicar-nota">{aviso}</p>}
    </div>
  )
}
