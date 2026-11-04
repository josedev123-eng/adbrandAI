import { useState } from 'react'
import { useSesion } from './app/context/useSesion'
import { NOMBRES_ROL, pantallasDelRol } from './app/routes/pantallasPorRol'
import AdministradoresView from './features/admin/views/AdministradoresView'
import CrearAdministradorView from './features/admin/views/CrearAdministradorView'
import AuditoriaView from './features/auditoria/views/AuditoriaView'
import LoginView from './features/auth/views/LoginView'
import BandejaRevisionView from './features/moderacion/views/BandejaRevisionView'
import HistorialContenidosView from './features/moderacion/views/HistorialContenidosView'
import EstadoServidorView from './features/servidor/views/EstadoServidorView'
import PanelFinancieroView from './features/suscripciones/views/PanelFinancieroView'
import ConsumoTokensView from './features/tokens/views/ConsumoTokensView'
import HistorialTokensView from './features/tokens/views/HistorialTokensView'
import MenuAdmin from './shared/components/MenuAdmin'
import { usePendientesRevision } from './shared/hooks/usePendientesRevision'

// Fecha de hoy para la barra superior, por ejemplo "martes, 10 de noviembre de 2026".
function fechaDeHoy() {
  return new Date().toLocaleDateString('es-PE', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })
}

function App() {
  const { admin, cargando, salir } = useSesion()
  const [pantalla, setPantalla] = useState(null)
  // El contador de "Revisión de contenido" solo se pide si el rol ve esa pantalla.
  const veRevision = pantallasDelRol(admin?.rol?.nombre).some((opcion) => opcion.id === 'revision')
  const pendientes = usePendientesRevision(veRevision, pantalla)

  if (cargando) return <p className="cargando-sesion">Cargando tu sesión…</p>
  if (!admin) return <LoginView />

  const rol = admin.rol?.nombre
  const opciones = pantallasDelRol(rol)
  // Si la pantalla elegida no es de este rol (o aún no eligió), se abre la primera que sí le toca.
  const actual = opciones.some((opcion) => opcion.id === pantalla) ? pantalla : opciones[0]?.id
  const seccionActual = opciones.find((opcion) => opcion.id === actual)?.seccion

  function cerrarSesion() {
    setPantalla(null)
    salir()
  }

  return (
    <div className="layout-admin">
      <MenuAdmin
        opciones={opciones}
        actual={actual}
        alCambiar={setPantalla}
        admin={admin}
        nombreRol={NOMBRES_ROL[rol] ?? rol}
        alSalir={cerrarSesion}
        contadores={{ revision: pendientes }}
      />
      <div className="layout-contenido">
        <div className="barra-superior">
          <span>
            Panel de administración
            {seccionActual && (
              <>
                {' · '}
                <strong>{seccionActual}</strong>
              </>
            )}
          </span>
          <span className="barra-fecha">{fechaDeHoy()}</span>
        </div>
      {actual === 'revision' && <BandejaRevisionView />}
      {actual === 'historial-contenidos' && <HistorialContenidosView />}
      {actual === 'administradores' && <CrearAdministradorView />}
      {actual === 'lista-administradores' && <AdministradoresView />}
      {actual === 'auditoria' && <AuditoriaView />}
      {actual === 'panel-financiero' && <PanelFinancieroView />}
      {actual === 'consumo-tokens' && <ConsumoTokensView />}
      {actual === 'historial-tokens' && <HistorialTokensView />}
      {actual === 'estado-servidor' && <EstadoServidorView />}
      {!actual && (
        <main className="sin-pantallas">
          <h1>Hola, {admin.nombres}</h1>
          <p>Tu rol todavía no tiene pantallas. Llegan con las historias del Sprint 2.</p>
        </main>
      )}
      </div>
    </div>
  )
}

export default App
