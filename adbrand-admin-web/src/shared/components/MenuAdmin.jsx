// Menú lateral de la web de administración. Solo dibuja; App decide qué opciones y qué pantalla se muestran.
import './MenuAdmin.css'

// Trazos de un ícono simple por opción (viewBox 24x24). Se dibujan con línea, sin relleno.
const ICONOS = {
  revision: ['M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z', 'M12 9a3 3 0 1 0 0 6 3 3 0 0 0 0-6Z'],
  'historial-contenidos': ['M12 7v5l3 2', 'M3 12a9 9 0 1 0 3-6.7', 'M3 4v4h4'],
  administradores: ['M15 20v-1a4 4 0 0 0-4-4H7a4 4 0 0 0-4 4v1', 'M9 11a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z', 'M19 8v6', 'M16 11h6'],
  'lista-administradores': ['M9 6h11', 'M9 12h11', 'M9 18h11', 'M4 6h.01', 'M4 12h.01', 'M4 18h.01'],
  auditoria: ['M12 3l8 3v6c0 4.5-3.4 8-8 9-4.6-1-8-4.5-8-9V6l8-3Z', 'M9 12l2 2 4-4'],
  'panel-financiero': ['M3 7h18v10H3z', 'M12 9.5a2.5 2.5 0 1 0 0 5 2.5 2.5 0 0 0 0-5Z', 'M6 10v4', 'M18 10v4'],
  'consumo-tokens': ['M7 7h10v10H7z', 'M10 3v4', 'M14 3v4', 'M10 17v4', 'M14 17v4', 'M3 10h4', 'M3 14h4', 'M17 10h4', 'M17 14h4'],
  'historial-tokens': ['M4 20V10', 'M10 20V4', 'M16 20v-7', 'M22 20H2'],
  'estado-servidor': ['M4 4h16v6H4z', 'M4 14h16v6H4z', 'M8 7h.01', 'M8 17h.01'],
}

function IconoOpcion({ id }) {
  const trazos = ICONOS[id]
  if (!trazos) return null
  return (
    <svg className="menu-icono" viewBox="0 0 24 24" aria-hidden="true">
      {trazos.map((d) => (
        <path key={d} d={d} />
      ))}
    </svg>
  )
}

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
                <IconoOpcion id={opcion.id} />
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
