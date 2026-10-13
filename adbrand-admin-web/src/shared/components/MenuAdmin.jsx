// Menú superior de la web de administración. Solo dibuja; App decide qué pantalla se muestra.
import './MenuAdmin.css'

const OPCIONES = [
  { id: 'revision', nombre: 'Revisión de contenido' },
  { id: 'administradores', nombre: 'Administradores' },
  { id: 'lista-administradores', nombre: 'Lista de administradores' },
  { id: 'auditoria', nombre: 'Auditoría' },
  { id: 'panel-financiero', nombre: 'Panel financiero' },
]

export default function MenuAdmin({ actual, alCambiar }) {
  return (
    <nav className="menu-admin">
      <span className="menu-marca">
        AdBrand<b>.AI</b> <small>Admin</small>
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
