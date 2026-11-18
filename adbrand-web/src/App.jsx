import { useState } from 'react'
import GenerarAnuncioView from './features/contenido/views/GenerarAnuncioView'
import PerfilNegocioView from './features/negocio/views/PerfilNegocioView'
import SolicitarKitMarcaView from './features/brandkit/views/SolicitarKitMarcaView'
import RegistroView from './features/auth/views/RegistroView'
import LoginView from './features/auth/views/LoginView'
import RecuperarContrasenaView from './features/auth/views/RecuperarContrasenaView'
import MiCuentaView from './features/cuenta/views/MiCuentaView'
import InicioView from './features/inicio/views/InicioView'
import PlanesView from './features/pagos/views/PlanesView'
import CalendarioView from './features/calendario/views/CalendarioView'
import InformeCampanasView from './features/reportes/views/InformeCampanasView'
import MenuUsuario from './shared/components/MenuUsuario'
import { SECCIONES } from './shared/components/opcionesMenuUsuario'
import { useSesion } from './app/context/useSesion'

// HU-22: sin sesión se muestran el inicio de sesión y el registro; con sesión, el menú y las pantallas de siempre.
function App() {
  const { usuario, cargando, salir } = useSesion()
  const [pantalla, setPantalla] = useState('inicio')
  const [pantallaPublica, setPantallaPublica] = useState('login')
  // HU-24: mensaje para el inicio de sesión, por ejemplo "Tu contraseña se cambió, inicia sesión".
  const [avisoLogin, setAvisoLogin] = useState(null)

  function irALogin(aviso = null) {
    setAvisoLogin(aviso)
    setPantallaPublica('login')
  }

  if (cargando) {
    return (
      <main className="pagina">
        <p className="cargando">Cargando tu sesión…</p>
      </main>
    )
  }

  if (!usuario) {
    return (
      <>
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
      <MenuUsuario actual={pantalla} alCambiar={setPantalla} empresa={usuario.nombreEmpresa} alSalir={cerrarSesion} />
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
      {pantalla === 'inicio' && <InicioView empresa={usuario.nombreEmpresa} alAbrir={setPantalla} />}
      {pantalla === 'perfil' && <PerfilNegocioView />}
      {pantalla === 'anuncio' && <GenerarAnuncioView irAPerfil={() => setPantalla('perfil')} />}
      {pantalla === 'kit' && <SolicitarKitMarcaView />}
      {pantalla === 'planes' && <PlanesView />}
      {pantalla === 'calendario' && <CalendarioView />}
      {pantalla === 'informes' && <InformeCampanasView />}
      {pantalla === 'cuenta' && <MiCuentaView />}
      </div>
    </div>
  )
}

export default App
