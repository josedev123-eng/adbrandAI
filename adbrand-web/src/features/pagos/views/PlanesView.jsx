// View: muestra los planes de suscripción con su precio y su límite de tokens (HU 25).
// La lógica de carga está en usePlanes.
import { usePlanes } from '../viewmodels/usePlanes'
import './PlanesView.css'

const SOLES = new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' })

export default function PlanesView() {
  const { planes, cargando, error } = usePlanes()

  if (cargando) {
    return (
      <main className="pagina">
        <p className="cargando">Cargando planes…</p>
      </main>
    )
  }

  return (
    <main className="pagina">
      <header className="pagina-cabecera">
        <h1>Planes de suscripción</h1>
        <p>Elige el plan que mejor se adapte a tu negocio. Puedes cambiarlo cuando quieras.</p>
      </header>

      {error && <p className="aviso aviso-error">{error}</p>}

      <section className="planes-grid">
        {planes.map((plan) => (
          <article key={plan.id} className="tarjeta plan-tarjeta">
            <h2 className="plan-nombre">{plan.nombre}</h2>
            <p className="plan-precio">
              {SOLES.format(plan.precio)}
              <span> / mes</span>
            </p>
            <p className="plan-descripcion">{plan.descripcion}</p>
            <p className="plan-tokens">
              {Number(plan.limiteTokens).toLocaleString('es-PE')} tokens de IA al mes
            </p>
          </article>
        ))}
      </section>
    </main>
  )
}
