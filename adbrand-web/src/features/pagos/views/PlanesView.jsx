// View: muestra los planes de suscripción y deja elegir cómo pagarlos (HU 25 a 27).
// La lógica de carga está en usePlanes; la de cada formulario, en su viewmodel.
import { useState } from 'react'
import { usePlanes } from '../viewmodels/usePlanes'
import PagoTarjetaView from './PagoTarjetaView'
import PagoYapeView from './PagoYapeView'
import './pagoComun.css'
import './PlanesView.css'

const SOLES = new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' })

export default function PlanesView() {
  const { planes, cargando, error } = usePlanes()
  const [planElegido, setPlanElegido] = useState(null)
  const [metodo, setMetodo] = useState(null)
  const [pago, setPago] = useState(null)

  function reiniciar() {
    setPago(null)
    setMetodo(null)
    setPlanElegido(null)
  }

  if (pago) {
    return (
      <main className="pagina">
        <section className="tarjeta pago-resumen">
          <span className="pago-resumen-etiqueta">Pago aprobado</span>
          <strong className="pago-resumen-monto">{SOLES.format(pago.monto)}</strong>
          <span className="pago-resumen-detalle">
            Plan {pago.plan} · {pago.metodo === 'YAPE' ? 'Yape' : 'Tarjeta'} · referencia {pago.referencia}
          </span>
          <button type="button" className="boton-primario" onClick={reiniciar}>
            Volver a los planes
          </button>
        </section>
      </main>
    )
  }

  if (planElegido && metodo === 'TARJETA') {
    return <PagoTarjetaView plan={planElegido} alPagar={setPago} alVolver={() => setMetodo(null)} />
  }

  if (planElegido && metodo === 'YAPE') {
    return <PagoYapeView plan={planElegido} alConfirmar={setPago} alVolver={() => setMetodo(null)} />
  }

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
            <div className="plan-acciones">
              <button
                type="button"
                className="boton-primario"
                onClick={() => {
                  setPlanElegido(plan)
                  setMetodo('TARJETA')
                }}
              >
                Pagar con tarjeta
              </button>
              <button
                type="button"
                className="boton-secundario"
                onClick={() => {
                  setPlanElegido(plan)
                  setMetodo('YAPE')
                }}
              >
                Pagar con Yape
              </button>
            </div>
          </article>
        ))}
      </section>
    </main>
  )
}
