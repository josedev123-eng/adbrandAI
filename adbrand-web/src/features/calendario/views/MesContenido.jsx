// View: HU 63 "Tu mes de contenido". Fechas peruanas que venden, con una idea según tu rubro,
// y un plan de 3 publicaciones por semana. "Crear anuncio" lleva a Crear con la idea ya escrita.
import Cargando from '../../../shared/components/Cargando'
import { useMesContenido } from '../viewmodels/useMesContenido'
import './MesContenido.css'

const MESES = ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio', 'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre']
const DIA = new Intl.DateTimeFormat('es-PE', { weekday: 'short', day: 'numeric', month: 'short', timeZone: 'UTC' })
// Qué es cada tipo de publicación del plan (se explica en la leyenda y al pasar el mouse).
const TIPOS = {
  OFERTA: {
    nombre: 'Oferta',
    clase: 'mes-tipo-oferta',
    explicacion: 'Una promoción para vender ya: descuento, 2x1 o combo, con precio y hasta cuándo vale.',
  },
  PRODUCTO_ESTRELLA: {
    nombre: 'Producto estrella',
    clase: 'mes-tipo-producto',
    explicacion: 'Presume lo que más te piden o te hace único, sin descuento.',
  },
  DETRAS_DE_CAMARAS: {
    nombre: 'Detrás de cámaras',
    clase: 'mes-tipo-detras',
    explicacion: 'Muestra tu día a día (cómo preparas, tu equipo, tu local). Genera confianza y la gente compra más a quien conoce.',
  },
  FECHA_ESPECIAL: {
    nombre: 'Fecha especial',
    clase: 'mes-tipo-fecha',
    explicacion: 'Una fecha que vende en el Perú: la publicación se ambienta en esa festividad.',
  },
}

const HOY = new Date().toISOString().slice(0, 10)

function dia(fecha) {
  return DIA.format(new Date(`${fecha}T00:00:00Z`))
}

function cuando(diasFaltan) {
  if (diasFaltan === 0) return '¡Es hoy!'
  if (diasFaltan === 1) return 'Mañana'
  if (diasFaltan > 1) return `Faltan ${diasFaltan} días`
  return 'Ya pasó'
}

export default function MesContenido({ alCrearAnuncio }) {
  const { periodo, datos, cargando, error, mesAnterior, mesSiguiente } = useMesContenido()

  return (
    <section className="tarjeta mes-contenido" aria-labelledby="mes-contenido-titulo">
      <header className="mes-cabecera">
        <div>
          <h2 id="mes-contenido-titulo">Tu mes de contenido</h2>
          <p>Fechas del Perú que venden, con ideas para tu negocio, y qué publicar cada semana.</p>
        </div>
        <div className="mes-navegacion">
          <button type="button" className="boton-secundario" onClick={mesAnterior} aria-label="Mes anterior">
            ←
          </button>
          <strong>
            {MESES[periodo.mes - 1]} {periodo.anio}
          </strong>
          <button type="button" className="boton-secundario" onClick={mesSiguiente} aria-label="Mes siguiente">
            →
          </button>
        </div>
      </header>

      {error && <p className="aviso aviso-error">{error}</p>}
      {cargando && !datos && <Cargando texto="Armando tu mes de contenido…" filas={4} />}

      {datos && (
        <div className="mes-columnas">
          <div>
            <h3>Fechas que venden</h3>
            {datos.fechas.length === 0 ? (
              <p className="kit-vacio">Este mes no hay fechas especiales: aprovecha para tus ofertas de siempre.</p>
            ) : (
              <ul className="mes-fechas">
                {datos.fechas.map((f) => (
                  <li key={`${f.fecha}-${f.nombre}`} className={f.diasFaltan < 0 ? 'mes-fecha mes-fecha-pasada' : 'mes-fecha'}>
                    <div className="mes-fecha-cabecera">
                      <span className="mes-fecha-dia">{dia(f.fecha)}</span>
                      <span className="mes-fecha-falta">{cuando(f.diasFaltan)}</span>
                    </div>
                    <strong>{f.nombre}</strong>
                    <p>{f.idea}</p>
                    {f.diasFaltan >= 0 && alCrearAnuncio && (
                      <button type="button" className="boton-ia" onClick={() => alCrearAnuncio(f.idea, f.nombre)}>
                        ✦ Crear anuncio para esta fecha
                      </button>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </div>

          <div>
            <h3>Plan sugerido: 3 publicaciones por semana</h3>
            {/* Leyenda: qué significa cada tipo, para que el dueño sepa qué publicar. */}
            <ul className="mes-leyenda" aria-label="Qué es cada tipo de publicación">
              {['OFERTA', 'PRODUCTO_ESTRELLA', 'DETRAS_DE_CAMARAS'].map((clave) => (
                <li key={clave}>
                  <span className={`mes-tipo ${TIPOS[clave].clase}`}>{TIPOS[clave].nombre}</span>
                  <span>{TIPOS[clave].explicacion}</span>
                </li>
              ))}
            </ul>
            <ol className="mes-plan">
              {datos.plan.map((p) => {
                const tipo = TIPOS[p.tipo] ?? { nombre: p.tipo, clase: '' }
                return (
                  <li key={`${p.fecha}-${p.tipo}`}>
                    <span className="mes-plan-dia">{dia(p.fecha)}</span>
                    <span className={`mes-tipo ${tipo.clase}`} title={tipo.explicacion}>
                      {tipo.nombre}
                    </span>
                    <span className="mes-plan-idea" title={p.idea}>
                      {p.tipo === 'FECHA_ESPECIAL' ? p.titulo : p.idea}
                    </span>
                    {/* Crear el anuncio de ese día con la idea ya escrita (solo hoy o fechas por venir). */}
                    {alCrearAnuncio && p.fecha >= HOY ? (
                      <button
                        type="button"
                        className="boton-enlace mes-plan-crear"
                        onClick={() => alCrearAnuncio(p.idea, p.tipo === 'FECHA_ESPECIAL' ? p.titulo : undefined)}
                        aria-label={`Crear anuncio: ${p.idea}`}
                      >
                        ✦ Crear
                      </button>
                    ) : (
                      <span />
                    )}
                  </li>
                )
              })}
            </ol>
          </div>
        </div>
      )}
    </section>
  )
}
