// View (HU 61): vista previa de la imagen final con la marca, en el formato elegido, para descargarla o compartirla.
import { FORMATOS_IMAGEN, useImagenConMarca } from '../viewmodels/useImagenConMarca'
import './ListoParaPublicar.css'

export default function ImagenConMarca({ anuncioId, texto }) {
  const { formato, elegirFormato, url, cargando, error, aviso, descargar, compartir } = useImagenConMarca(anuncioId)

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
        {cargando && <span className="imagen-marca-cargando">Poniendo tu marca en la imagen…</span>}
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
