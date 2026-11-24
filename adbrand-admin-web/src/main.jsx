import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import './styles/sistema-diseno.css'
import App from './App.jsx'
import { SesionProvider } from './app/context/SesionProvider'
import LimiteDeErrores from './shared/components/LimiteDeErrores'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    {/* Último respaldo: si algo falla fuera de una pantalla, no se queda en blanco */}
    <LimiteDeErrores>
      <SesionProvider>
        <App />
      </SesionProvider>
    </LimiteDeErrores>
  </StrictMode>,
)
