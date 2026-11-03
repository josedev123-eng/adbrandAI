import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.jsx'
import SesionProvider from './app/context/SesionProvider'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    {/* HU-22: la sesión del usuario PYME está disponible en todas las pantallas */}
    <SesionProvider>
      <App />
    </SesionProvider>
  </StrictMode>,
)
