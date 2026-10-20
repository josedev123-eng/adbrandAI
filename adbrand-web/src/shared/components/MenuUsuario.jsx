// Menú superior de la web del usuario. Solo dibuja; App decide qué pantalla se muestra.
import './MenuUsuario.css'

const OPCIONES = [
  { id: 'perfil', nombre: 'Mi negocio' },
  { id: 'anuncio', nombre: 'Crear anuncio' },
  { id: 'kit', nombre: 'Kit de Marca' },
  // HU-21: hasta que exista el inicio de sesión (HU-22), el registro se abre desde el menú.
  { id: 'registro', nombre: 'Crear cuenta' },
  { id: 'login', nombre: 'Iniciar sesión' },
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
