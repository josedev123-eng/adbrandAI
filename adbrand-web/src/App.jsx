import { lazy, Suspense, useState } from 'react'
import RegistroView from './features/auth/views/RegistroView'
import LoginView from './features/auth/views/LoginView'
import RecuperarContrasenaView from './features/auth/views/RecuperarContrasenaView'
import InicioView from './features/inicio/views/InicioView'
import MenuUsuario from './shared/components/MenuUsuario'
import AvisoConexion from './shared/components/AvisoConexion'
import LimiteDeErrores from './shared/components/LimiteDeErrores'
import PantallaArranque from './shared/components/PantallaArranque'
import Cargando from './shared/components/Cargando'
import { SECCIONES } from './shared/components/opcionesMenuUsuario'
import { useSesion } from './app/context/useSesion'
import { useTituloPagina } from './shared/hooks/useTituloPagina'
import { useAvisosModeracion } from './shared/hooks/useAvisosModeracion'

// Optimización: cada pantalla se descarga recién cuando el usuario la abre (code splitting).
const GenerarAnuncioView = lazy(() => import('./features/contenido/views/GenerarAnuncioView'))
const PerfilNegocioView = lazy(() => import('./features/negocio/views/PerfilNegocioView'))
const SolicitarKitMarcaView = lazy(() => import('./features/brandkit/views/SolicitarKitMarcaView'))
const MiCuentaView = lazy(() => import('./features/cuenta/views/MiCuentaView'))
const PlanesView = lazy(() => import('./features/pagos/views/PlanesView'))
const CalendarioView = lazy(() => import('./features/calendario/views/CalendarioView'))
const InformeCampanasView = lazy(() => import('./features/reportes/views/InformeCampanasView'))
const MisAnunciosView = lazy(() => import('./features/misAnuncios/views/MisAnunciosView'))

// HU-22: sin sesión se muestran el inicio de sesión y el registro; con sesión, el menú y las pantallas de siempre.
function App() {
  const { usuario, cargando, salir, actualizarUsuario } = useSesion()
  const [pantalla, setPantalla] = useState('inicio')
  const [pantallaPublica, setPantallaPublica] = useState('login')
  // HU-24: mensaje para el inicio de sesión, por ejemplo "Tu contraseña se cambió, inicia sesión".
  const [avisoLogin, setAvisoLogin] = useState(null)
  // Nombre de la pantalla en la pestaña del navegador.
  const nombrePantalla = usuario
    ? SECCIONES.flatMap((seccion) => seccion.opciones).find((opcion) => opcion.id === pantalla)?.nombre
    : { login: 'Iniciar sesión', registro: 'Crear cuenta', recuperar: 'Recuperar contraseña' }[pantallaPublica]
  useTituloPagina(nombrePantalla)
  // Decisiones del moderador sin ver: el número junto a "Mis anuncios".
  const { avisos, limpiarAvisos } = useAvisosModeracion(Boolean(usuario), pantalla)

  function irALogin(aviso = null) {
    setAvisoLogin(aviso)
    setPantallaPublica('login')
  }

  if (cargando) {
    return <PantallaArranque texto="Revisando tu sesión…" />
  }

  if (!usuario) {
    return (
      <>
        <AvisoConexion />
        {pantallaPublica === 'login' && (
          <LoginView
            key={avisoLogin?.texto ?? 'sin-aviso'}
            aviso={avisoLogin}
            irARegistro={() => setPantallaPublica('registro')}
            irARecuperar={() => setPantallaPublica('recuperar')}
          />
        )}
        {pantallaPublica === 'registro' && <RegistroView irALogin={() => irALogin()} />}
        {pantallaPublica === 'recuperar' && (
          <RecuperarContrasenaView irALogin={() => irALogin()} alTerminar={(aviso) => irALogin(aviso)} />
        )}
      </>
    )
  }

  function cerrarSesion() {
    salir()
    setPantalla('inicio')
    setPantallaPublica('login')
  }

  // Sección de la pantalla actual para la barra superior ("Crear", "Planificar" o "Mi empresa").
  const seccionActual = SECCIONES.find((seccion) => seccion.opciones.some((opcion) => opcion.id === pantalla))?.titulo
  const fechaDeHoy = new Date().toLocaleDateString('es-PE', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })

  return (
    <div className="layout-usuario">
      <MenuUsuario
        actual={pantalla}
        alCambiar={setPantalla}
        empresa={usuario.nombreEmpresa}
        colorAvatar={usuario.colorAvatar}
        alSalir={cerrarSesion}
        contadores={{ misAnuncios: avisos }}
      />
      <div className="layout-contenido">
        <div className="barra-superior">
          <span>
            {usuario.nombreEmpresa ?? 'Tu negocio'}
            {seccionActual && (
              <>
                {' · '}
                <strong>{seccionActual}</strong>
              </>
            )}
          </span>
          <span className="barra-fecha">{fechaDeHoy}</span>
        </div>
        <AvisoConexion />
        {/* key: al cambiar de pantalla, el aviso de error se reinicia */}
        <LimiteDeErrores key={pantalla} alVolverAlInicio={() => setPantalla('inicio')}>
          <Suspense
            fallback={
              <main className="pagina">
                <Cargando texto="Abriendo…" />
              </main>
            }
          >
            {pantalla === 'inicio' && <InicioView empresa={usuario.nombreEmpresa} alAbrir={setPantalla} />}
            {pantalla === 'perfil' && <PerfilNegocioView />}
            {pantalla === 'anuncio' && <GenerarAnuncioView irAPerfil={() => setPantalla('perfil')} irAPlanes={() => setPantalla('planes')} />}
            {pantalla === 'misAnuncios' && (
              <MisAnunciosView alVerAvisos={limpiarAvisos} irACrear={() => setPantalla('anuncio')} />
            )}
            {pantalla === 'kit' && <SolicitarKitMarcaView />}
            {pantalla === 'planes' && <PlanesView />}
            {pantalla === 'calendario' && <CalendarioView />}
            {pantalla === 'informes' && <InformeCampanasView />}
            {pantalla === 'cuenta' && (
              <MiCuentaView alGuardar={(empresa) => actualizarUsuario({ colorAvatar: empresa.colorAvatar })} />
            )}
          </Suspense>
        </LimiteDeErrores>
      </div>
    </div>
  )
}

export default App
