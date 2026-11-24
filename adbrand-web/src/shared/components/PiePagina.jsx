// Pie de página: derechos, Términos y condiciones, Política de privacidad y contacto.
// Sale en la página de presentación, en el inicio de sesión, en el registro y dentro de la app.
import './PiePagina.css'

import { CORREO_CONTACTO } from '../config/contacto'

export default function PiePagina({ abrir, compacto = false }) {
  const anio = new Date().getFullYear()
  return (
    <footer className={compacto ? 'pie-pagina pie-pagina-compacto' : 'pie-pagina'}>
      <span>
        © {anio} AdBrand<b>.AI</b> · Hecho en Perú
      </span>
      <nav aria-label="Información legal">
        <button type="button" className="pie-enlace" onClick={() => abrir('terminos')}>
          Términos y condiciones
        </button>
        <button type="button" className="pie-enlace" onClick={() => abrir('privacidad')}>
          Política de privacidad
        </button>
        <a className="pie-enlace" href={`mailto:${CORREO_CONTACTO}`}>
          Contacto
        </a>
      </nav>
    </footer>
  )
}
