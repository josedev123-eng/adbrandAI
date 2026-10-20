import { useState } from 'react'
import GenerarAnuncioView from './features/contenido/views/GenerarAnuncioView'
import PerfilNegocioView from './features/negocio/views/PerfilNegocioView'
import SolicitarKitMarcaView from './features/brandkit/views/SolicitarKitMarcaView'
import RegistroView from './features/auth/views/RegistroView'
import LoginView from './features/auth/views/LoginView'
import MenuUsuario from './shared/components/MenuUsuario'

function App() {
  const [pantalla, setPantalla] = useState('anuncio')

  return (
    <>
      <MenuUsuario actual={pantalla} alCambiar={setPantalla} />
      {pantalla === 'perfil' && <PerfilNegocioView />}
      {pantalla === 'anuncio' && <GenerarAnuncioView irAPerfil={() => setPantalla('perfil')} />}
      {pantalla === 'kit' && <SolicitarKitMarcaView />}
      {pantalla === 'registro' && <RegistroView irALogin={() => setPantalla('login')} />}
      {pantalla === 'login' && <LoginView irARegistro={() => setPantalla('registro')} />}
    </>
  )
}

export default App