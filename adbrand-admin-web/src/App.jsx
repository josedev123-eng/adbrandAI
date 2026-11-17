import { lazy, Suspense, useEffect, useState } from 'react'
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
import PaginaNoEncontrada from './shared/components/PaginaNoEncontrada'
import PieAdmin from './shared/components/PieAdmin'
import { navegar, useRuta } from './shared/navegacion/navegacion'
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
const EmpresasView = lazy(() => import('./features/empresas/views/EmpresasView'))
const MiCuentaAdminView = lazy(() => import('./features/cuenta/views/MiCuentaAdminView'))
const RecuperarContrasenaView = lazy(() => import('./features/auth/views/RecuperarContrasenaView'))

// Cada pantalla tiene su dirección: /inicio, /revision, /empresas, /mi-cuenta… (el id de la pantalla).
const RUTA_RECUPERAR = '/recuperar-contrasena'
const rutaDe = (id) => `/${id}`

// Fecha de hoy para la barra superior, por ejemplo "martes, 10 de noviembre de 2026".
function fechaDeHoy() {
  return new Date().toLocaleDateString('es-PE', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })
}

function App() {
  const { admin, cargando, salir } = useSesion()
  // La pantalla sale de la dirección del navegador; así funcionan F5, Atrás/Adelante y los enlaces.
  const ruta = useRuta()
  const pantalla = ruta === '/' ? null : ruta.slice(1)
  const setPantalla = (id) => navegar(rutaDe(id))
  // "Tu contraseña se cambió": aviso para el login después de recuperarla.
  const [avisoLogin, setAvisoLogin] = useState(null)
  // El contador de "Revisión de contenido" solo se pide si el rol ve esa pantalla.
  const veRevision = pantallasDelRol(admin?.rol?.nombre).some((opcion) => opcion.id === 'revision')
  const pendientes = usePendientesRevision(veRevision, pantalla)
  // Nombre de la pantalla en la pestaña del navegador.
  const opcionesDelRol = pantallasDelRol(admin?.rol?.nombre)
  const idActual = opcionesDelRol.some((opcion) => opcion.id === pantalla) ? pantalla : opcionesDelRol[0]?.id
  const nombreDePantalla =
    pantalla === 'mi-cuenta' ? 'Mi cuenta' : opcionesDelRol.find((opcion) => opcion.id === idActual)?.nombre
  const esDesconocida = admin && pantalla && pantalla !== 'mi-cuenta' && !opcionesDelRol.some((opcion) => opcion.id === pantalla)
  useTituloPagina(
    admin ? (esDesconocida ? 'Página no encontrada' : nombreDePantalla) : ruta === RUTA_RECUPERAR ? 'Recuperar contraseña' : 'Iniciar sesión',
  )

  // Con sesión, la raíz y la pantalla de recuperar llevan a la primera pantalla del rol.
  useEffect(() => {
    if (admin && (ruta === '/' || ruta === RUTA_RECUPERAR) && opcionesDelRol[0]) {
      navegar(rutaDe(opcionesDelRol[0].id), { reemplazar: true })
    }
  }, [admin, ruta, opcionesDelRol])

  if (cargando) return <PantallaArranque texto="Revisando tu sesión…" />
  // Sin sesión se muestra el login en cualquier dirección; al entrar, se abre esa misma pantalla.
  if (!admin) {
    return (
      <>
        <AvisoConexion />
        {ruta === RUTA_RECUPERAR ? (
          <Suspense fallback={<PantallaArranque texto="Abriendo…" />}>
            <RecuperarContrasenaView
              irALogin={() => navegar('/')}
              alTerminar={(texto) => {
                setAvisoLogin(texto)
                navegar('/', { reemplazar: true })
              }}
            />
          </Suspense>
        ) : (
          <LoginView
            aviso={avisoLogin}
            irARecuperar={() => {
              setAvisoLogin(null)
              navegar(RUTA_RECUPERAR)
            }}
          />
        )}
      </>
    )
  }

  // Primer inicio con la contraseña temporal del superadmin: antes de todo, debe elegir una propia.
  if (admin.debe_cambiar_contrasena) {
    return (
      <Suspense fallback={<PantallaArranque texto="Abriendo…" />}>
        <MiCuentaAdminView
          obligatorio
          alSalir={() => {
            navegar('/')
            salir()
          }}
        />
      </Suspense>
    )
  }

  const rol = admin.rol?.nombre
  const opciones = pantallasDelRol(rol)
  // Pantalla a mostrar: la de la dirección si es de este rol; "mi-cuenta" la ven todos; otra dirección es un 404.
  const actual = pantalla === null ? opciones[0]?.id : esDesconocida ? 'noEncontrada' : pantalla
  const seccionActual = opciones.find((opcion) => opcion.id === actual)?.seccion

  function cerrarSesion() {
    navegar('/')
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
            {actual === 'empresas' && <EmpresasView />}
            {actual === 'administradores' && <CrearAdministradorView />}
            {actual === 'lista-administradores' && <AdministradoresView />}
            {actual === 'auditoria' && <AuditoriaView />}
            {actual === 'panel-financiero' && <PanelFinancieroView />}
            {actual === 'consumo-tokens' && <ConsumoTokensView />}
            {actual === 'historial-tokens' && <HistorialTokensView />}
            {actual === 'estado-servidor' && <EstadoServidorView />}
            {actual === 'mi-cuenta' && <MiCuentaAdminView />}
            {actual === 'noEncontrada' && (
              <PaginaNoEncontrada
                alVolver={() => setPantalla(opciones[0]?.id ?? 'inicio')}
                textoVolver="Ir al inicio del panel"
              />
            )}
            {!actual && (
              <main className="sin-pantallas">
                <h1>Hola, {admin.nombres}</h1>
                <p>Tu rol todavía no tiene pantallas. Llegan con las historias del Sprint 2.</p>
              </main>
            )}
          </Suspense>
        </LimiteDeErrores>
        <PieAdmin />
      </div>
    </div>
  )
}

export default App
