import { useState } from 'react'
import { useSesion } from './app/context/useSesion'
import { NOMBRES_ROL, pantallasDelRol } from './app/routes/pantallasPorRol'
import AdministradoresView from './features/admin/views/AdministradoresView'
import CrearAdministradorView from './features/admin/views/CrearAdministradorView'
import AuditoriaView from './features/auditoria/views/AuditoriaView'
import LoginView from './features/auth/views/LoginView'
import BandejaRevisionView from './features/moderacion/views/BandejaRevisionView'
import PanelFinancieroView from './features/suscripciones/views/PanelFinancieroView'
import MenuAdmin from './shared/components/MenuAdmin'

function App() {
  const { admin, cargando, salir } = useSesion()
  const [pantalla, setPantalla] = useState(null)

  if (cargando) return <p className="cargando-sesion">Cargando tu sesión…</p>
  if (!admin) return <LoginView />

  const rol = admin.rol?.nombre
  const opciones = pantallasDelRol(rol)
  // Si la pantalla elegida no es de este rol (o aún no eligió), se abre la primera que sí le toca.
  const actual = opciones.some((opcion) => opcion.id === pantalla) ? pantalla : opciones[0]?.id

  function cerrarSesion() {
    setPantalla(null)
    salir()
  }

  return (
    <>
      <MenuAdmin
        opciones={opciones}
        actual={actual}
        alCambiar={setPantalla}
        admin={admin}
        nombreRol={NOMBRES_ROL[rol] ?? rol}
        alSalir={cerrarSesion}
      />
      {actual === 'revision' && <BandejaRevisionView />}
      {actual === 'administradores' && <CrearAdministradorView />}
      {actual === 'lista-administradores' && <AdministradoresView />}
      {actual === 'auditoria' && <AuditoriaView />}
      {actual === 'panel-financiero' && <PanelFinancieroView />}
      {!actual && (
        <main className="sin-pantallas">
          <h1>Hola, {admin.nombres}</h1>
          <p>Tu rol todavía no tiene pantallas. Llegan con las historias del Sprint 2.</p>
        </main>
      )}
    </>
  )
}

export default App
