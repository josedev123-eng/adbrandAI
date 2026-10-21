// View: formulario de pago con tarjeta. Toda la lógica está en usePagoTarjeta (HU 26).
import { usePagoTarjeta } from '../viewmodels/usePagoTarjeta'
import './pagoComun.css'
import './PagoTarjetaView.css'

const SOLES = new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' })

function Campo({ etiqueta, error, ayuda, children }) {
  return (
    <label className={error ? 'campo campo-con-error' : 'campo'}>
      <span>{etiqueta}</span>
      {children}
      {error && <small className="campo-error">{error}</small>}
      {ayuda && !error && <small className="campo-ayuda">{ayuda}</small>}
    </label>
  )
}

export default function PagoTarjetaView({ plan, alPagar, alVolver }) {
  const { formulario, errores, mensaje, enviando, cambiarCampo, pagar } = usePagoTarjeta(plan, alPagar)

  function alEnviar(evento) {
    evento.preventDefault()
    pagar()
  }

  return (
    <main className="pagina">
      <header className="pagina-cabecera">
        <h1>Pagar con tarjeta</h1>
        <p>El cobro es simulado: no se hace ningún cargo real.</p>
      </header>

      <section className="tarjeta pago-resumen">
        <span className="pago-resumen-etiqueta">Plan {plan.nombre}</span>
        <strong className="pago-resumen-monto">{SOLES.format(plan.precio)} / mes</strong>
        <span className="pago-resumen-detalle">
          {Number(plan.limiteTokens).toLocaleString('es-PE')} tokens de IA al mes
        </span>
      </section>

      <form className="tarjeta formulario" onSubmit={alEnviar} noValidate>
        <Campo etiqueta="Número de la tarjeta" error={errores.numeroTarjeta}>
          <input
            type="text"
            inputMode="numeric"
            autoComplete="cc-number"
            placeholder="4111 1111 1111 1111"
            value={formulario.numeroTarjeta}
            onChange={(evento) => cambiarCampo('numeroTarjeta', evento.target.value)}
          />
        </Campo>

        <Campo etiqueta="Nombre del titular" error={errores.titular}>
          <input
            type="text"
            autoComplete="cc-name"
            placeholder="Como aparece en la tarjeta"
            value={formulario.titular}
            onChange={(evento) => cambiarCampo('titular', evento.target.value)}
          />
        </Campo>

        <div className="fila">
          <Campo etiqueta="Vencimiento" error={errores.vencimiento} ayuda="MM/AA">
            <input
              type="text"
              inputMode="numeric"
              autoComplete="cc-exp"
              placeholder="MM/AA"
              maxLength={5}
              value={formulario.vencimiento}
              onChange={(evento) => cambiarCampo('vencimiento', evento.target.value)}
            />
          </Campo>

          <Campo etiqueta="CVV" error={errores.cvv} ayuda="3 o 4 dígitos del reverso">
            <input
              type="password"
              inputMode="numeric"
              autoComplete="cc-csc"
              maxLength={4}
              value={formulario.cvv}
              onChange={(evento) => cambiarCampo('cvv', evento.target.value)}
            />
          </Campo>
        </div>

        {mensaje && <p className={`aviso aviso-${mensaje.tipo}`}>{mensaje.texto}</p>}

        <div className="pago-acciones">
          <button type="button" className="boton-secundario" onClick={alVolver} disabled={enviando}>
            Volver
          </button>
          <button type="submit" className="boton-primario" disabled={enviando}>
            {enviando ? 'Procesando…' : `Pagar ${SOLES.format(plan.precio)}`}
          </button>
        </div>
      </form>
    </main>
  )
}
