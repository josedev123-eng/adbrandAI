// Menú superior de la web de administración. Solo dibuja; App decide qué opciones y qué pantalla se muestran.
import './MenuAdmin.css'

export default function MenuAdmin({ opciones, actual, alCambiar, admin, nombreRol, alSalir }) {
  return (
    <nav className="menu-admin">
      <span className="menu-marca">
        AdBrand<b>.AI</b> <small>Admin</small>
      </span>
      <div className="menu-opciones">
        {opciones.map((opcion) => (
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
      {admin && (
        <div className="menu-cuenta">
          <span className="menu-usuario">
            {admin.nombres} {admin.apellidos}
            <small>{nombreRol}</small>
          </span>
          <button type="button" className="menu-salir" onClick={alSalir}>
            Cerrar sesión
          </button>
        </div>
      )}
    </nav>
  )
}
