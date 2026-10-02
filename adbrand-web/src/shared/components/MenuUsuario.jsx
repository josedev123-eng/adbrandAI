// Menú superior de la web del usuario. Solo dibuja; App decide qué pantalla se muestra.
import './MenuUsuario.css'

const OPCIONES = [
  { id: 'perfil', nombre: 'Mi negocio' },
  { id: 'anuncio', nombre: 'Crear anuncio' },
]

export default function MenuUsuario({ actual, alCambiar }) {
  return (
    <nav className="menu-usuario">
      <span className="menu-marca">
        AdBrand<b>.AI</b>
      </span>
      <div className="menu-opciones">
        {OPCIONES.map((opcion) => (
          <button
            key={opcion.id}
            type="button"
            className={actual === opcion.id ? 'menu-opcion menu-opcion-activa' : 'menu-opcion'}
            onClick={() => alCambiar(opcion.id)}
          >
            {opcion.nombre}
          </button>
        ))}
      </div>
    </nav>
  )
}
