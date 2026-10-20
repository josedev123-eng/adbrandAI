import { useState } from 'react'
import GenerarAnuncioView from './features/contenido/views/GenerarAnuncioView'
import PerfilNegocioView from './features/negocio/views/PerfilNegocioView'
import SolicitarKitMarcaView from './features/brandkit/views/SolicitarKitMarcaView'
import RegistroView from './features/auth/views/RegistroView'
import LoginView from './features/auth/views/LoginView'
import RecuperarContrasenaView from './features/auth/views/RecuperarContrasenaView'
import MenuUsuario from './shared/components/MenuUsuario'
import { useSesion } from './app/context/useSesion'

// HU-22: sin sesión se muestran el inicio de sesión y el registro; con sesión, el menú y las pantallas de siempre.
function App() {
  const { usuario, cargando, salir } = useSesion()
  const [pantalla, setPantalla] = useState('anuncio')
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
    setPantalla('anuncio')
    setPantallaPublica('login')
  }

  return (
    <>
      <MenuUsuario actual={pantalla} alCambiar={setPantalla} empresa={usuario.nombreEmpresa} alSalir={cerrarSesion} />
      {pantalla === 'perfil' && <PerfilNegocioView />}
      {pantalla === 'anuncio' && <GenerarAnuncioView irAPerfil={() => setPantalla('perfil')} />}
      {pantalla === 'kit' && <SolicitarKitMarcaView />}
    </>
  )
}

export default App
