// Mientras llegan los datos: filas grises que parpadean y el texto de siempre debajo.
import './EstadosLista.css'

export default function Cargando({ texto, filas = 3 }) {
  return (
    <div className="cargando-esqueleto" role="status">
      {Array.from({ length: filas }, (_, i) => (
        <span key={i} className="esqueleto-fila" aria-hidden="true" />
      ))}
      <p className="cargando-texto">{texto}</p>
    </div>
  )
}
