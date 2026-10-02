import { useState } from 'react'
import CrearAdministradorView from './features/admin/views/CrearAdministradorView'
import BandejaRevisionView from './features/moderacion/views/BandejaRevisionView'
import MenuAdmin from './shared/components/MenuAdmin'

function App() {
  const [pantalla, setPantalla] = useState('revision')

  return (
    <>
      <MenuAdmin actual={pantalla} alCambiar={setPantalla} />
      {pantalla === 'revision' && <BandejaRevisionView />}
      {pantalla === 'administradores' && <CrearAdministradorView />}
    </>
  )
}

export default App