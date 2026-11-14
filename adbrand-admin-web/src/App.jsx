import { lazy, Suspense, useState } from 'react'
import { useSesion } from './app/context/useSesion'
import { NOMBRES_ROL, pantallasDelRol } from './app/routes/pantallasPorRol'
import LoginView from './features/auth/views/LoginView'
import InicioView from './features/inicio/views/InicioView'
import { tarjetasDelRol } from './features/inicio/viewmodels/useInicio'
import MenuAdmin from './shared/components/MenuAdmin'
import AvisoConexion from './shared/components/AvisoConexion'
import LimiteDeErrores from './shared/components/LimiteDeErrores'
import PantallaArranque from './shared/components/PantallaArranque'
import Cargando from './shared/components/Cargando'
import { usePendientesRevision } from './shared/hooks/usePendientesRevision'
import { useTituloPagina } from './shared/hooks/useTituloPagina'

// Optimización: cada pantalla se descarga recién cuando se abre (code splitting).
const AdministradoresView = lazy(() => import('./features/admin/views/AdministradoresView'))
const CrearAdministradorView = lazy(() => import('./features/admin/views/CrearAdministradorView'))
const AuditoriaView = lazy(() => import('./features/auditoria/views/AuditoriaView'))
const BandejaRevisionView = lazy(() => import('./features/moderacion/views/BandejaRevisionView'))
const HistorialContenidosView = lazy(() => import('./features/moderacion/views/HistorialContenidosView'))
const EstadoServidorView = lazy(() => import('./features/servidor/views/EstadoServidorView'))
const PanelFinancieroView = lazy(() => import('./features/suscripciones/views/PanelFinancieroView'))
const ConsumoTokensView = lazy(() => import('./features/tokens/views/ConsumoTokensView'))
const HistorialTokensView = lazy(() => import('./features/tokens/views/HistorialTokensView'))

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
  // Nombre de la pantalla en la pestaña del navegador.
  const opcionesDelRol = pantallasDelRol(admin?.rol?.nombre)
  const idActual = opcionesDelRol.some((opcion) => opcion.id === pantalla) ? pantalla : opcionesDelRol[0]?.id
  useTituloPagina(admin ? opcionesDelRol.find((opcion) => opcion.id === idActual)?.nombre : 'Iniciar sesión')

  if (cargando) return <PantallaArranque texto="Revisando tu sesión…" />
  if (!admin) {
    return (
      <>
        <AvisoConexion />
        <LoginView />
      </>
    )
  }

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
        <AvisoConexion />
        {/* key: al cambiar de pantalla, el aviso de error se reinicia */}
        <LimiteDeErrores key={actual ?? 'ninguna'} alVolverAlInicio={() => setPantalla('inicio')}>
          <Suspense
            fallback={
              <main className="pagina">
                <Cargando texto="Abriendo…" />
              </main>
            }
          >
            {actual === 'inicio' && <InicioView admin={admin} claves={tarjetasDelRol(opciones)} alAbrir={setPantalla} />}
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
          </Suspense>
        </LimiteDeErrores>
      </div>
    </div>
  )
}

export default App
