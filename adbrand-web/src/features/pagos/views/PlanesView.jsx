// View: muestra los planes de suscripción y deja elegir cómo pagarlos (HU 25 a 27).
// La lógica de carga está en usePlanes; la de cada formulario, en su viewmodel.
import Cargando from '../../../shared/components/Cargando'
import { useState } from 'react'
import { conMiles, NUMERO } from '../models/formatoPlanes'
import { usePlanes } from '../viewmodels/usePlanes'
import ConfirmacionPagoView from './ConfirmacionPagoView'
import PagoConTarjetaView from './PagoConTarjetaView'
import PagoConYapeView from './PagoConYapeView'
import './pagoComun.css'
import './PlanesView.css'

const SOLES = new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' })

export default function PlanesView() {
  const { planes, cargando, error, planActual } = usePlanes()
  const [planElegido, setPlanElegido] = useState(null)
  const [metodo, setMetodo] = useState(null)
  const [pago, setPago] = useState(null)

  function reiniciar() {
    setPago(null)
    setMetodo(null)
    setPlanElegido(null)
  }

  if (pago) {
    return <ConfirmacionPagoView pago={pago} alVolver={reiniciar} />
  }

  if (planElegido && metodo === 'TARJETA') {
    return <PagoConTarjetaView plan={planElegido} alPagar={setPago} alVolver={() => setMetodo(null)} />
  }

  if (planElegido && metodo === 'YAPE') {
    return <PagoConYapeView plan={planElegido} alConfirmar={setPago} alVolver={() => setMetodo(null)} />
  }

  if (cargando) {
    return (
      <main className="pagina">
        <Cargando texto="Cargando planes…" filas={4} />
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

      {planActual && (
        <p className="aviso aviso-info plan-actual-aviso">
          Tu plan actual: <b>{planActual.gratuito ? 'Gratuito' : planActual.nombre}</b>
          {planActual.limite ? ` · ${NUMERO.format(planActual.limite)} tokens de IA al mes` : ''}
        </p>
      )}

      <section className="planes-grid">
        {planes.map((plan) => (
          <article
            key={plan.id}
            className={planActual?.nombre === plan.nombre ? 'tarjeta plan-tarjeta plan-tarjeta-actual' : 'tarjeta plan-tarjeta'}
          >
            <h2 className="plan-nombre">
              {plan.nombre}
              {planActual?.nombre === plan.nombre && <span className="plan-actual-etiqueta">Tu plan actual</span>}
            </h2>
            <p className="plan-precio">
              {SOLES.format(plan.precio)}
              <span> / mes</span>
            </p>
            <p className="plan-descripcion">{conMiles(plan.descripcion)}</p>
            <p className="plan-tokens">
              {NUMERO.format(Number(plan.limiteTokens))} tokens de IA al mes
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
