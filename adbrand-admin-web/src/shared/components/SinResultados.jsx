// Lista sin datos: un ícono simple y el mensaje centrado dentro de la tarjeta.
import './EstadosLista.css'

export default function SinResultados({ children }) {
  return (
    <div className="sin-resultados">
      <svg className="sin-resultados-icono" viewBox="0 0 24 24" aria-hidden="true">
        <path d="M4 7h16v11a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V7Z" />
        <path d="M4 7l2.5-3h11L20 7" />
        <path d="M9 12h6" />
      </svg>
      <p>{children}</p>
    </div>
  )
}
