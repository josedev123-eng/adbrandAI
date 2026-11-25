import { guardarOfertaSugerida } from './features/contenido/models/ofertaSugerida'
import { lazy, Suspense, useEffect, useState } from 'react'
import LandingView from './features/landing/views/LandingView'
import PiePagina from './shared/components/PiePagina'
import PaginaNoEncontrada from './shared/components/PaginaNoEncontrada'
import { navegar, pantallaDeRuta, useRuta } from './shared/navegacion/navegacion'
import { marcarTutorialVisto } from './features/tutorial/models/tutorialApi'
import MenuUsuario from './shared/components/MenuUsuario'
import { usePlanDelMenu } from './features/pagos/viewmodels/usePlanDelMenu'
import { useGeneracionEnCurso } from './features/contenido/viewmodels/useGeneracionEnCurso'
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
const VerificarCorreoView = lazy(() => import('./features/auth/views/VerificarCorreoView'))
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
const AyudaView = lazy(() => import('./features/ayuda/views/AyudaView'))
const ContactoView = lazy(() => import('./features/contacto/views/ContactoView'))
const TutorialGuiado = lazy(() => import('./features/tutorial/views/TutorialGuiado'))

const CARGANDO_PANTALLA = (
  <main className="pagina">
    <Cargando texto="Abriendo…" />
  </main>
)

// Dirección de cada pantalla. Con sesión y sin sesión hay mapas distintos; Términos y Privacidad están en los dos.
const RUTAS_PRIVADAS = {
  inicio: '/inicio',
  anuncio: '/crear-anuncio',
  misAnuncios: '/mis-anuncios',
  kit: '/kit-de-marca',
  calendario: '/calendario',
  informes: '/informes',
  perfil: '/mi-negocio',
  planes: '/planes',
  cuenta: '/mi-cuenta',
  terminos: '/terminos',
  privacidad: '/privacidad',
  ayuda: '/ayuda',
  contacto: '/contacto',
}
const RUTAS_PUBLICAS = {
  inicio: '/',
  login: '/iniciar-sesion',
  registro: '/crear-cuenta',
  recuperar: '/recuperar-contrasena',
  verificar: '/confirmar-correo',
  terminos: '/terminos',
  privacidad: '/privacidad',
  ayuda: '/ayuda',
  contacto: '/contacto',
}
const SOLO_SIN_SESION = ['/', '/iniciar-sesion', '/crear-cuenta', '/recuperar-contrasena', '/confirmar-correo']

// Si alguien abre /calendario sin sesión, se le pide entrar y luego se le lleva ahí.
let rutaPendiente = null

// Páginas de información que se abren desde el pie (y se cierran volviendo a donde se estaba).
const NOMBRES_LEGALES = {
  terminos: 'Términos y condiciones',
  privacidad: 'Política de privacidad',
  ayuda: 'Ayuda',
  contacto: 'Contacto',
}
const TEXTO_VOLVER = {
  inicio: 'Volver al inicio',
  login: 'Volver a iniciar sesión',
  registro: 'Volver al registro',
  recuperar: 'Volver',
}

// HU-22: sin sesión se muestran el inicio de sesión y el registro; con sesión, el menú y las pantallas de siempre.
function App() {
  const { usuario, cargando, salir, actualizarUsuario, avisoSalida } = useSesion()
  // La pantalla sale de la dirección del navegador (/calendario, /mi-cuenta…), no de un estado interno.
  const ruta = useRuta()
  const pantalla = pantallaDeRuta(RUTAS_PRIVADAS, ruta) ?? 'noEncontrada'
  const pantallaPublica =
    pantallaDeRuta(RUTAS_PUBLICAS, ruta) ?? (pantallaDeRuta(RUTAS_PRIVADAS, ruta) ? 'login' : 'noEncontrada')
  const setPantalla = (id) => navegar(RUTAS_PRIVADAS[id])
  const setPantallaPublica = (id) => navegar(RUTAS_PUBLICAS[id])
  // Desde qué pantalla se abrieron Términos o Privacidad (null si se entró directo con el enlace).
  const [volverDeLegal, setVolverDeLegal] = useState(null)
  // Tutorial guiado: sale solo la primera vez (tutorialVisto=false, V46) o cuando se pide desde Mi cuenta.
  const [verTutorial, setVerTutorial] = useState(false)
  // HU-24: mensaje para el inicio de sesión, por ejemplo "Tu contraseña se cambió, inicia sesión".
  const [avisoLogin, setAvisoLogin] = useState(null)
  // "Confirma tu correo" (V56): a qué correo se mandó el código y si se llegó desde el inicio de sesión.
  const [porVerificar, setPorVerificar] = useState(null)
  // Nombre de la pantalla en la pestaña del navegador.
  const nombrePantalla = usuario
    ? (NOMBRES_LEGALES[pantalla] ?? SECCIONES.flatMap((seccion) => seccion.opciones).find((opcion) => opcion.id === pantalla)?.nombre)
    : { login: 'Iniciar sesión', registro: 'Crear cuenta', recuperar: 'Recuperar contraseña', verificar: 'Confirma tu correo', ...NOMBRES_LEGALES }[pantallaPublica]
  const titulo = (usuario ? pantalla : pantallaPublica) === 'noEncontrada' ? 'Página no encontrada' : nombrePantalla
  useTituloPagina(titulo)
  // Decisiones del moderador sin ver: el número junto a "Mis anuncios".
  const { avisos, limpiarAvisos } = useAvisosModeracion(Boolean(usuario), pantalla)
  // El plan debajo del nombre en el menú ("Plan Gratis", "Plan Pro"…).
  const planDelMenu = usePlanDelMenu(Boolean(usuario), pantalla)
  // Opciones de anuncio que terminaron de crearse mientras el dueño estaba en otra pantalla.
  const generacion = useGeneracionEnCurso()
  // Si la sesión se cerró sola (venció o la cuenta fue suspendida), se muestra el inicio de sesión con el aviso.
  useEffect(() => {
    if (avisoSalida) navegar(RUTAS_PUBLICAS.login, { reemplazar: true })
  }, [avisoSalida])

  // Redirecciones según haya sesión o no:
  // - con sesión, la landing y el login llevan a Inicio (o a la pantalla que se quería abrir antes de entrar);
  // - sin sesión, una pantalla privada lleva al login y se recuerda para después.
  useEffect(() => {
    if (cargando) return
    if (usuario && SOLO_SIN_SESION.includes(ruta)) {
      const destino = rutaPendiente ?? RUTAS_PRIVADAS.inicio
      rutaPendiente = null
      navegar(destino, { reemplazar: true })
    } else if (!usuario && !pantallaDeRuta(RUTAS_PUBLICAS, ruta) && pantallaDeRuta(RUTAS_PRIVADAS, ruta)) {
      rutaPendiente = ruta
      navegar(RUTAS_PUBLICAS.login, { reemplazar: true })
    }
  }, [usuario, cargando, ruta])

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

  function irAVerificar(correo, desdeLogin) {
    setPorVerificar({ correo, desdeLogin })
    setPantallaPublica('verificar')
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

  // "Volver" de Términos/Privacidad: si se abrieron desde la app, es como el botón Atrás; si se entró directo, al inicio.
  function cerrarLegal() {
    if (volverDeLegal) window.history.back()
    else navegar(usuario ? RUTAS_PRIVADAS.inicio : RUTAS_PUBLICAS.inicio)
    setVolverDeLegal(null)
  }

  if (!usuario) {
    const enLegal = pantallaPublica === 'terminos' || pantallaPublica === 'privacidad'
    // Si se abren los Términos desde el registro, el formulario se oculta (no se desmonta) para no perder lo escrito.
    const enAyuda = pantallaPublica === 'ayuda'
    const enContacto = pantallaPublica === 'contacto'
    const registroVisible =
      pantallaPublica === 'registro' || ((enLegal || enAyuda || enContacto) && volverDeLegal === 'registro')

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
              irAVerificar={(correo) => irAVerificar(correo, true)}
            />
          )}
          {registroVisible && (
            <div hidden={enLegal || enAyuda || enContacto}>
              <RegistroView
                irALogin={() => irALogin()}
                abrir={abrirLegal}
                irAInicio={() => setPantallaPublica('inicio')}
                irAVerificar={(correo) => irAVerificar(correo, false)}
              />
            </div>
          )}
          {pantallaPublica === 'verificar' &&
            (porVerificar ? (
              <VerificarCorreoView
                key={porVerificar.correo}
                correo={porVerificar.correo}
                desdeLogin={porVerificar.desdeLogin}
                irALogin={() => irALogin()}
              />
            ) : (
              // Al recargar la página se pierde a qué correo se mandó el código: se pide iniciar sesión,
              // y el login vuelve a abrir esta pantalla con un código nuevo.
              <LoginView
                aviso={{ tipo: 'info', texto: 'Inicia sesión para recibir un código nuevo y confirmar tu correo.' }}
                irARegistro={() => setPantallaPublica('registro')}
                irARecuperar={() => setPantallaPublica('recuperar')}
                irAInicio={() => setPantallaPublica('inicio')}
                irAVerificar={(correo) => irAVerificar(correo, true)}
              />
            ))}
          {pantallaPublica === 'recuperar' && (
            <RecuperarContrasenaView irALogin={() => irALogin()} alTerminar={(aviso) => irALogin(aviso)} />
          )}
        </Suspense>
        {pantallaPublica === 'noEncontrada' && <PaginaNoEncontrada alVolver={() => setPantallaPublica('inicio')} />}
        {enAyuda && (
          <Suspense fallback={CARGANDO_PANTALLA}>
            <AyudaView alVolver={cerrarLegal} />
          </Suspense>
        )}
        {enContacto && (
          <Suspense fallback={CARGANDO_PANTALLA}>
            <ContactoView alVolver={cerrarLegal} irAAyuda={() => setPantallaPublica('ayuda')} />
          </Suspense>
        )}
        {enLegal && (
          <Suspense fallback={CARGANDO_PANTALLA}>
            <DocumentoLegalView
              tipo={pantallaPublica}
              alVolver={cerrarLegal}
              textoVolver={TEXTO_VOLVER[volverDeLegal] ?? 'Ir al inicio'}
            />
          </Suspense>
        )}
        <PiePagina abrir={abrirLegal} compacto />
      </div>
    )
  }

  const tutorialAbierto = verTutorial || usuario.tutorialVisto === false

  // Al terminar o saltar se guarda en la base que ya lo vio; si falla, igual se cierra (no es grave).
  function cerrarTutorial(irAlPerfil) {
    setVerTutorial(false)
    if (!usuario.tutorialVisto) {
      actualizarUsuario({ tutorialVisto: true })
      marcarTutorialVisto().catch(() => {})
    }
    if (irAlPerfil) setPantalla('perfil')
  }

  function cerrarSesion() {
    setVerTutorial(false)
    // Primero se va a la landing y después se cierra la sesión, así no se "recuerda" la pantalla actual para volver.
    // Si hay cambios sin guardar y el usuario decide quedarse, no se cierra la sesión.
    if (!navegar(RUTAS_PUBLICAS.inicio)) return
    salir()
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
        imagenAvatar={usuario.avatarImagen}
        plan={planDelMenu}
        alSalir={cerrarSesion}
        contadores={{ misAnuncios: avisos, anuncio: generacion === 'lista' && pantalla !== 'anuncio' ? 1 : 0 }}
        forzarAbierto={tutorialAbierto}
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
            {pantalla === 'anuncio' && <GenerarAnuncioView irAPerfil={() => setPantalla('perfil')} irAPlanes={() => setPantalla('planes')} irAMiCuenta={() => setPantalla('cuenta')} />}
            {pantalla === 'misAnuncios' && (
              <MisAnunciosView
                alVerAvisos={limpiarAvisos}
                irACrear={() => setPantalla('anuncio')}
                irAPlanes={() => setPantalla('planes')}
              />
            )}
            {pantalla === 'kit' && <SolicitarKitMarcaView />}
            {pantalla === 'planes' && <PlanesView irAMiCuenta={() => setPantalla('cuenta')} />}
            {pantalla === 'calendario' && (
              <CalendarioView
                alCrearAnuncio={(idea, ocasion) => {
                  guardarOfertaSugerida(idea, ocasion)
                  setPantalla('anuncio')
                }}
              />
            )}
            {pantalla === 'informes' && <InformeCampanasView />}
            {pantalla === 'cuenta' && (
              <MiCuentaView
                alGuardar={(empresa) => actualizarUsuario({ colorAvatar: empresa.colorAvatar })}
                alVerTutorial={() => setVerTutorial(true)}
                avatarImagen={usuario.avatarImagen}
                alCambiarAvatar={(avatarImagen) => actualizarUsuario({ avatarImagen })}
                irAPlanes={() => setPantalla('planes')}
              />
            )}
            {(pantalla === 'terminos' || pantalla === 'privacidad') && (
              <DocumentoLegalView tipo={pantalla} alVolver={cerrarLegal} />
            )}
            {pantalla === 'ayuda' && <AyudaView />}
            {pantalla === 'contacto' && (
              <ContactoView
                nombre={usuario.nombre}
                correo={usuario.correo}
                alVolver={cerrarLegal}
                irAAyuda={() => setPantalla('ayuda')}
              />
            )}
            {pantalla === 'noEncontrada' && <PaginaNoEncontrada alVolver={() => setPantalla('inicio')} />}
          </Suspense>
        </LimiteDeErrores>
        <PiePagina abrir={abrirLegal} compacto />
      </div>
      {tutorialAbierto && (
        <Suspense fallback={null}>
          <TutorialGuiado
            empresa={usuario.nombreEmpresa}
            alTerminar={() => cerrarTutorial(true)}
            alSaltar={() => cerrarTutorial(false)}
          />
        </Suspense>
      )}
    </div>
  )
}

export default App
