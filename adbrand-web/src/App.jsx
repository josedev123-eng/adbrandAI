import { lazy, Suspense, useEffect, useState } from 'react'
import LandingView from './features/landing/views/LandingView'
import PiePagina from './shared/components/PiePagina'
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
// La página de presentación sí va en el paquete principal: es lo primero que se pinta sin sesión.
const cargarLogin = () => import('./features/auth/views/LoginView')
const cargarRegistro = () => import('./features/auth/views/RegistroView')
const LoginView = lazy(cargarLogin)
const RegistroView = lazy(cargarRegistro)
const RecuperarContrasenaView = lazy(() => import('./features/auth/views/RecuperarContrasenaView'))
const InicioView = lazy(() => import('./features/inicio/views/InicioView'))
const GenerarAnuncioView = lazy(() => import('./features/contenido/views/GenerarAnuncioView'))
const PerfilNegocioView = lazy(() => import('./features/negocio/views/PerfilNegocioView'))
const SolicitarKitMarcaView = lazy(() => import('./features/brandkit/views/SolicitarKitMarcaView'))
const MiCuentaView = lazy(() => import('./features/cuenta/views/MiCuentaView'))
const PlanesView = lazy(() => import('./features/pagos/views/PlanesView'))
const CalendarioView = lazy(() => import('./features/calendario/views/CalendarioView'))
const InformeCampanasView = lazy(() => import('./features/reportes/views/InformeCampanasView'))
const MisAnunciosView = lazy(() => import('./features/misAnuncios/views/MisAnunciosView'))
const DocumentoLegalView = lazy(() => import('./features/legal/views/DocumentoLegalView'))

const CARGANDO_PANTALLA = (
  <main className="pagina">
    <Cargando texto="Abriendo…" />
  </main>
)

const NOMBRES_LEGALES = { terminos: 'Términos y condiciones', privacidad: 'Política de privacidad' }
const TEXTO_VOLVER = {
  inicio: 'Volver al inicio',
  login: 'Volver a iniciar sesión',
  registro: 'Volver al registro',
  recuperar: 'Volver',
}

// HU-22: sin sesión se muestran el inicio de sesión y el registro; con sesión, el menú y las pantallas de siempre.
function App() {
  const { usuario, cargando, salir, actualizarUsuario, avisoSalida } = useSesion()
  const [pantalla, setPantalla] = useState('inicio')
  // Sin sesión se empieza por la página de presentación.
  const [pantallaPublica, setPantallaPublica] = useState('inicio')
  const [volverDeLegal, setVolverDeLegal] = useState('inicio')
  // HU-24: mensaje para el inicio de sesión, por ejemplo "Tu contraseña se cambió, inicia sesión".
  const [avisoLogin, setAvisoLogin] = useState(null)
  // Nombre de la pantalla en la pestaña del navegador.
  const nombrePantalla = usuario
    ? (NOMBRES_LEGALES[pantalla] ?? SECCIONES.flatMap((seccion) => seccion.opciones).find((opcion) => opcion.id === pantalla)?.nombre)
    : { login: 'Iniciar sesión', registro: 'Crear cuenta', recuperar: 'Recuperar contraseña', ...NOMBRES_LEGALES }[pantallaPublica]
  useTituloPagina(nombrePantalla)
  // Decisiones del moderador sin ver: el número junto a "Mis anuncios".
  const { avisos, limpiarAvisos } = useAvisosModeracion(Boolean(usuario), pantalla)
  // Si la sesión se cerró sola (venció o la cuenta fue suspendida), se muestra el inicio de sesión con el aviso.
  const [avisoAtendido, setAvisoAtendido] = useState(avisoSalida)
  if (avisoSalida !== avisoAtendido) {
    setAvisoAtendido(avisoSalida)
    if (avisoSalida) setPantallaPublica('login')
  }

  // Sin sesión: cuando la página de presentación ya se pintó, se bajan en segundo plano el login y el registro
  // para que abran al instante al hacer clic.
  useEffect(() => {
    if (usuario || cargando) return undefined
    const temporizador = setTimeout(() => {
      cargarLogin()
      cargarRegistro()
    }, 1500)
    return () => clearTimeout(temporizador)
  }, [usuario, cargando])

  function irALogin(aviso = null) {
    setAvisoLogin(aviso)
    setPantallaPublica('login')
  }

  if (cargando) {
    return <PantallaArranque texto="Revisando tu sesión…" />
  }

  // Términos o Privacidad: al cerrarlos se vuelve a la pantalla desde donde se abrieron.
  function abrirLegal(tipo) {
    const actual = usuario ? pantalla : pantallaPublica
    if (!NOMBRES_LEGALES[actual]) setVolverDeLegal(actual)
    if (usuario) setPantalla(tipo)
    else setPantallaPublica(tipo)
  }

  if (!usuario) {
    const enLegal = pantallaPublica === 'terminos' || pantallaPublica === 'privacidad'
    // Si se abren los Términos desde el registro, el formulario se oculta (no se desmonta) para no perder lo escrito.
    const registroVisible = pantallaPublica === 'registro' || (enLegal && volverDeLegal === 'registro')

    if (pantallaPublica === 'inicio') {
      return (
        <>
          <AvisoConexion />
          <LandingView
            irARegistro={() => setPantallaPublica('registro')}
            irALogin={() => irALogin()}
            abrir={abrirLegal}
          />
        </>
      )
    }

    return (
      <div className="pantalla-publica">
        <AvisoConexion />
        <Suspense fallback={CARGANDO_PANTALLA}>
          {pantallaPublica === 'login' && (
            <LoginView
              key={avisoLogin?.texto ?? avisoSalida ?? 'sin-aviso'}
              aviso={avisoLogin ?? (avisoSalida ? { tipo: 'error', texto: avisoSalida } : null)}
              irARegistro={() => setPantallaPublica('registro')}
              irARecuperar={() => setPantallaPublica('recuperar')}
              irAInicio={() => setPantallaPublica('inicio')}
            />
          )}
          {registroVisible && (
            <div hidden={enLegal}>
              <RegistroView irALogin={() => irALogin()} abrir={abrirLegal} irAInicio={() => setPantallaPublica('inicio')} />
            </div>
          )}
          {pantallaPublica === 'recuperar' && (
            <RecuperarContrasenaView irALogin={() => irALogin()} alTerminar={(aviso) => irALogin(aviso)} />
          )}
        </Suspense>
        {enLegal && (
          <Suspense fallback={CARGANDO_PANTALLA}>
            <DocumentoLegalView
              tipo={pantallaPublica}
              alVolver={() => setPantallaPublica(volverDeLegal)}
              textoVolver={TEXTO_VOLVER[volverDeLegal]}
            />
          </Suspense>
        )}
        <PiePagina abrir={abrirLegal} compacto />
      </div>
    )
  }

  function cerrarSesion() {
    salir()
    setPantalla('inicio')
    setPantallaPublica('inicio')
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
            {(pantalla === 'terminos' || pantalla === 'privacidad') && (
              <DocumentoLegalView tipo={pantalla} alVolver={() => setPantalla(volverDeLegal)} />
            )}
          </Suspense>
        </LimiteDeErrores>
        <PiePagina abrir={abrirLegal} compacto />
      </div>
    </div>
  )
}

export default App
