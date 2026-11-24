// Pie de página del panel de administración.
import './PieAdmin.css'

export default function PieAdmin() {
  return (
    <footer className="pie-admin">
      <span>
        © {new Date().getFullYear()} AdBrand<b>.AI</b> · Panel de administración
      </span>
      <a href="mailto:adbrandai.soporte@gmail.com">Soporte: adbrandai.soporte@gmail.com</a>
    </footer>
  )
}
