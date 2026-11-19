// Menú lateral de la web del usuario (mismo estilo que el de la web de administración).
// Solo dibuja; App decide qué pantalla se muestra.
import { useEffect, useState } from 'react'
import MarcaAdbrand from './MarcaAdbrand'
import { colorDeAvatar } from './avatar'
import { iniciales, SECCIONES } from './opcionesMenuUsuario'
import './MenuUsuario.css'

// Trazos de un ícono simple por opción (viewBox 24x24). Se dibujan con línea, sin relleno.
const ICONOS = {
  inicio: ['M3 11l9-7 9 7', 'M5 10v10h14V10', 'M10 20v-6h4v6'],
  anuncio: ['M12 3l1.8 4.6L18.5 9l-4.7 1.4L12 15l-1.8-4.6L5.5 9l4.7-1.4L12 3Z', 'M19 15l.9 2.1L22 18l-2.1.9L19 21l-.9-2.1L16 18l2.1-.9L19 15Z'],
  misAnuncios: ['M4 5h16v14H4z', 'M8 9h8', 'M8 13h5', 'M15.5 15.5l1.5 1.5 3-3'],
  kit: ['M12 3a9 9 0 1 0 0 18c1.1 0 1.5-.8 1.5-1.5 0-1-.8-1.4-.8-2.3 0-.8.7-1.2 1.6-1.2H17a4 4 0 0 0 4-4c0-5-4-9-9-9Z', 'M7.5 11h.01', 'M10 7h.01', 'M14.5 7h.01'],
  calendario: ['M4 6h16v14H4z', 'M4 10h16', 'M8 3v4', 'M16 3v4'],
  informes: ['M4 20V10', 'M10 20V4', 'M16 20v-7', 'M22 20H2'],
  perfil: ['M3 21h18', 'M5 21V8l7-5 7 5v13', 'M9 21v-6h6v6'],
  planes: ['M12 3l2.6 5.3 5.9.9-4.3 4.1 1 5.8L12 16.3 6.8 19.1l1-5.8-4.3-4.1 5.9-.9L12 3Z'],
  cuenta: ['M20 21v-1a5 5 0 0 0-5-5H9a5 5 0 0 0-5 5v1', 'M12 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z'],
  ayuda: ['M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18Z', 'M9.5 9a2.5 2.5 0 0 1 4.9.7c0 1.6-2.4 2.3-2.4 3.8', 'M12 17h.01'],
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

// HU-22: "empresa" es el nombre de la empresa que inició sesión y "alSalir" cierra la sesión.
// "contadores": números junto a una opción, por ejemplo decisiones del moderador sin ver en "Mis anuncios".
// En celular el menú se esconde detrás del botón ☰; "forzarAbierto" lo deja abierto (por ejemplo, durante el tutorial).
export default function MenuUsuario({ actual, alCambiar, empresa, colorAvatar, alSalir, contadores = {}, forzarAbierto = false }) {
  const [abierto, setAbierto] = useState(false)
  const visible = abierto || forzarAbierto
  const totalAvisos = Object.values(contadores).reduce((suma, n) => suma + (n > 0 ? n : 0), 0)

  // Con el menú abierto en celular, Esc lo cierra.
  useEffect(() => {
    if (!abierto) return undefined
    const alTeclear = (evento) => {
      if (evento.key === 'Escape') setAbierto(false)
    }
    window.addEventListener('keydown', alTeclear)
    return () => window.removeEventListener('keydown', alTeclear)
  }, [abierto])

  function elegir(id) {
    setAbierto(false)
    alCambiar(id)
  }

  return (
    <aside className={visible ? 'menu-usuario menu-abierto' : 'menu-usuario'}>
      <div className="menu-cabecera">
        <MarcaAdbrand />
        <button
          type="button"
          className="menu-hamburguesa"
          aria-expanded={visible}
          aria-controls="menu-usuario-contenido"
          aria-label={visible ? 'Cerrar menú' : 'Abrir menú'}
          onClick={() => setAbierto((actual) => !actual)}
        >
          <span aria-hidden="true">{visible ? '✕' : '☰'}</span>
          {!visible && totalAvisos > 0 && <span className="menu-hamburguesa-aviso" aria-hidden="true" />}
        </button>
      </div>
      <div id="menu-usuario-contenido" className="menu-contenido">
        <nav className="menu-secciones" aria-label="Menú principal">
          {SECCIONES.map((seccion) => (
            <div key={seccion.titulo || 'inicio'} className="menu-seccion">
              {seccion.titulo && <span className="menu-seccion-titulo">{seccion.titulo}</span>}
              {seccion.opciones.map((opcion) => (
                <button
                  key={opcion.id}
                  type="button"
                  className={actual === opcion.id ? 'menu-opcion menu-opcion-activa' : 'menu-opcion'}
                  aria-current={actual === opcion.id ? 'page' : undefined}
                  data-tutorial={opcion.id}
                  onClick={() => elegir(opcion.id)}
                >
                  <IconoOpcion id={opcion.id} />
                  {opcion.nombre}
                  {contadores[opcion.id] > 0 && (
                    <span className="menu-contador" aria-label={`${contadores[opcion.id]} sin ver`}>
                      {contadores[opcion.id]}
                    </span>
                  )}
                </button>
              ))}
            </div>
          ))}
        </nav>
        {alSalir && (
          <div className="menu-sesion">
            <span className="menu-avatar" aria-hidden="true" style={{ background: colorDeAvatar(empresa, colorAvatar) }}>
              {iniciales(empresa)}
            </span>
            {empresa && <span className="menu-empresa">{empresa}</span>}
            <button type="button" className="menu-salir" onClick={alSalir}>
              Cerrar sesión
            </button>
          </div>
        )}
      </div>
    </aside>
  )
}
