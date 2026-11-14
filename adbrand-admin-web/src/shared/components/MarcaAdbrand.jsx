// Logo de AdBrand.AI: cuadrado índigo plano (el color de los botones) con "AB" en blanco y el punto coral del ".AI" (el mismo del
// favicon y de la app móvil). Sin degradados: colores planos, un color principal y un acento.
import './MarcaAdbrand.css'

export function IconoAdbrand({ tamano = 32 }) {
  return (
    <svg className="marca-icono" width={tamano} height={tamano} viewBox="0 0 64 64" aria-hidden="true" focusable="false">
      <rect width="64" height="64" rx="16" fill="#4F46E5" />
      <text x="29" y="42" textAnchor="middle" fontSize="27" fontWeight="800" fill="#FFFFFF" letterSpacing="-1">
        AB
      </text>
      <circle cx="50" cy="40" r="5" fill="#F97316" />
    </svg>
  )
}

export default function MarcaAdbrand({ etiqueta }) {
  return (
    <span className="menu-marca marca-adbrand">
      <IconoAdbrand />
      <span>
        AdBrand<b>.AI</b>
      </span>
      {etiqueta && <small>{etiqueta}</small>}
    </span>
  )
}
