// Menú lateral de la web de administración. Solo dibuja; App decide qué opciones y qué pantalla se muestran.
import './MenuAdmin.css'

// Agrupa las opciones por su sección, en el mismo orden en que llegan.
function agruparPorSeccion(opciones) {
  const grupos = []
  for (const opcion of opciones) {
    const titulo = opcion.seccion ?? ''
    const grupo = grupos.find((g) => g.titulo === titulo)
    if (grupo) grupo.opciones.push(opcion)
    else grupos.push({ titulo, opciones: [opcion] })
  }
  return grupos
}

function iniciales(admin) {
  return `${admin.nombres?.[0] ?? ''}${admin.apellidos?.[0] ?? ''}`.toUpperCase()
}

export default function MenuAdmin({ opciones, actual, alCambiar, admin, nombreRol, alSalir }) {
  return (
    <aside className="menu-admin">
      <span className="menu-marca">
        AdBrand<b>.AI</b> <small>Admin</small>
      </span>
      <nav className="menu-secciones" aria-label="Menú de administración">
        {agruparPorSeccion(opciones).map((grupo) => (
          <div key={grupo.titulo} className="menu-seccion">
            {grupo.titulo && <span className="menu-seccion-titulo">{grupo.titulo}</span>}
            {grupo.opciones.map((opcion) => (
              <button
                key={opcion.id}
                type="button"
                className={actual === opcion.id ? 'menu-opcion menu-opcion-activa' : 'menu-opcion'}
                aria-current={actual === opcion.id ? 'page' : undefined}
                onClick={() => alCambiar(opcion.id)}
              >
                {opcion.nombre}
              </button>
            ))}
          </div>
        ))}
      </nav>
      {admin && (
        <div className="menu-cuenta">
          <span className="menu-avatar" aria-hidden="true">{iniciales(admin)}</span>
          <span className="menu-usuario">
            {admin.nombres} {admin.apellidos}
            <small>{nombreRol}</small>
          </span>
          <button type="button" className="menu-salir" onClick={alSalir}>
            Cerrar sesión
          </button>
        </div>
      )}
    </aside>
  )
}
