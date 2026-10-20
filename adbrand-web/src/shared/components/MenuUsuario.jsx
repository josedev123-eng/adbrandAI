// Menú superior de la web del usuario. Solo dibuja; App decide qué pantalla se muestra.
import './MenuUsuario.css'

const OPCIONES = [
  { id: 'perfil', nombre: 'Mi negocio' },
  { id: 'anuncio', nombre: 'Crear anuncio' },
  { id: 'kit', nombre: 'Kit de Marca' },
  { id: 'planes', nombre: 'Planes' },
  { id: 'cuenta', nombre: 'Mi cuenta' },
]

// HU-22: "empresa" es el nombre de la empresa que inició sesión y "alSalir" cierra la sesión.
export default function MenuUsuario({ actual, alCambiar, empresa, alSalir }) {
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
      {alSalir && (
        <div className="menu-sesion">
          {empresa && <span className="menu-empresa">{empresa}</span>}
          <button type="button" className="menu-salir" onClick={alSalir}>
            Cerrar sesión
          </button>
        </div>
      )}
    </nav>
  )
}
