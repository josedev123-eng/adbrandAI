// Pantalla de arranque mientras se revisa la sesión: el logo late suave y debajo un esqueleto del contenido.
// Evita el "parpadeo" de texto suelto y la pantalla en blanco al abrir la web.
import { IconoAdbrand } from './MarcaAdbrand'
import './PantallaArranque.css'

export default function PantallaArranque({ texto = 'Preparando tu espacio…' }) {
  return (
    <div className="arranque" role="status" aria-live="polite">
      <div className="arranque-logo">
        <IconoAdbrand tamano={56} />
      </div>
      <div className="arranque-barra" aria-hidden="true">
        <span />
      </div>
      <p className="arranque-texto">{texto}</p>
    </div>
  )
}
