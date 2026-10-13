import { useState } from 'react'
import AdministradoresView from './features/admin/views/AdministradoresView'
import CrearAdministradorView from './features/admin/views/CrearAdministradorView'
import AuditoriaView from './features/auditoria/views/AuditoriaView'
import BandejaRevisionView from './features/moderacion/views/BandejaRevisionView'
import PanelFinancieroView from './features/suscripciones/views/PanelFinancieroView'
import MenuAdmin from './shared/components/MenuAdmin'

function App() {
  const [pantalla, setPantalla] = useState('revision')

  return (
    <>
      <MenuAdmin actual={pantalla} alCambiar={setPantalla} />
      {pantalla === 'revision' && <BandejaRevisionView />}
      {pantalla === 'administradores' && <CrearAdministradorView />}
      {pantalla === 'lista-administradores' && <AdministradoresView />}
      {pantalla === 'auditoria' && <AuditoriaView />}
      {pantalla === 'panel-financiero' && <PanelFinancieroView />}
    </>
  )
}

export default App