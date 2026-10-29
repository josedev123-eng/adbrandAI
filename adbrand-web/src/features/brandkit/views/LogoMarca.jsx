// View: dibuja el logo diseñado en el editor (HU-29) a partir de sus datos.
// Se arma con elementos SVG de React (no se inserta HTML), así un texto raro no puede ejecutar nada.
const RADIO_ESQUINA = { CIRCULO: 50, CUADRADO: 0, REDONDEADO: 22 }

export default function LogoMarca({ diseno, tamano = 160 }) {
  if (!diseno) return null
  const { texto, forma, colorFondo, colorTexto, tipografia } = diseno
  // Cuanto más largo el texto, más chica la letra para que entre en el logo.
  const letra = texto.length <= 2 ? 44 : texto.length <= 4 ? 30 : Math.max(10, Math.round(120 / texto.length))

  return (
    <svg
      className="logo-marca"
      width={tamano}
      height={tamano}
      viewBox="0 0 100 100"
      role="img"
      aria-label={`Logo: ${texto}`}
    >
      <rect x="0" y="0" width="100" height="100" rx={RADIO_ESQUINA[forma] ?? 0} fill={colorFondo} />
      <text
        x="50"
        y="50"
        textAnchor="middle"
        dominantBaseline="central"
        fill={colorTexto}
        fontFamily={tipografia}
        fontSize={letra}
        fontWeight="700"
      >
        {texto}
      </text>
    </svg>
  )
}
