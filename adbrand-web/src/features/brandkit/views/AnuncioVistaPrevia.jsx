// View: un anuncio de ejemplo pintado con los colores de la marca (HU-30, criterio 1).
// Color 1: fondo del anuncio · color 2: botón · color 3: detalle de la oferta. Si faltan, se repite el primero.
// HU-31: si llegan tipografías, el título usa la principal y el texto la secundaria.
import { textoLegible } from '../models/brandkitApi'
import LogoMarca from './LogoMarca'

export default function AnuncioVistaPrevia({ colores, nombreNegocio, logoDiseno, tipografiaTitulo, tipografiaTexto }) {
  if (colores.length === 0) {
    return <p className="anuncio-vacio">Elige al menos un color válido para ver el anuncio.</p>
  }
  const fondo = colores[0]
  const boton = colores[1] ?? colores[0]
  const detalle = colores[2] ?? boton
  const fuenteTitulo = tipografiaTitulo ? { fontFamily: `'${tipografiaTitulo}', sans-serif` } : undefined
  const fuenteTexto = tipografiaTexto ? { fontFamily: `'${tipografiaTexto}', sans-serif` } : undefined

  return (
    <article
      className="anuncio-vista-previa"
      aria-label="Anuncio de ejemplo con tus colores"
      style={{ backgroundColor: fondo, color: textoLegible(fondo) }}
    >
      <header className="anuncio-cabecera">
        {logoDiseno && <LogoMarca diseno={logoDiseno} tamano={40} />}
        <strong>{nombreNegocio}</strong>
      </header>
      <p className="anuncio-oferta" style={fuenteTitulo}>
        ¡Oferta de la semana! <span style={{ backgroundColor: detalle, color: textoLegible(detalle) }}>20% dscto.</span>
      </p>
      <p className="anuncio-texto" style={fuenteTexto}>Ven a visitarnos y aprovecha antes de que se acabe.</p>
      <span className="anuncio-boton" style={{ backgroundColor: boton, color: textoLegible(boton) }}>
        Ver oferta
      </span>
    </article>
  )
}
