// View: pago con Yape. Primero se genera un QR; cuando el usuario ya pagó desde su app,
// confirma la operación. La lógica está en usePagoYape (HU 27).
import { usePagoYape } from '../viewmodels/usePagoYape'
import './pagoComun.css'
import './PagoYapeView.css'

const SOLES = new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' })

export default function PagoYapeView({ plan, alConfirmar, alVolver }) {
  const { celular, cambiarCelular, operacion, qr, cargando, confirmando, error, generar, confirmar } =
    usePagoYape(plan, alConfirmar)

  function alGenerar(evento) {
    evento.preventDefault()
    generar()
  }

  return (
    <main className="pagina">
      <header className="pagina-cabecera">
        <h1>Pagar con Yape</h1>
        <p>El cobro es simulado: no se hace ningún cargo real.</p>
      </header>

      <section className="tarjeta pago-resumen">
        <span className="pago-resumen-etiqueta">Plan {plan.nombre}</span>
        <strong className="pago-resumen-monto">{SOLES.format(plan.precio)} / mes</strong>
        <span className="pago-resumen-detalle">
          {Number(plan.limiteTokens).toLocaleString('es-PE')} tokens de IA al mes
        </span>
      </section>

      {error && <p className="aviso aviso-error">{error}</p>}

      {!operacion ? (
        <form className="tarjeta formulario" onSubmit={alGenerar} noValidate>
          <label className="campo">
            <span>Celular de Yape (opcional)</span>
            <input
              type="tel"
              inputMode="numeric"
              maxLength={9}
              placeholder="987654321"
              value={celular}
              onChange={(evento) => cambiarCelular(evento.target.value)}
            />
            <small className="campo-ayuda">Si lo escribes, el QR quedará asociado a tu número.</small>
          </label>

          <div className="pago-acciones">
            <button type="button" className="boton-secundario" onClick={alVolver} disabled={cargando}>
              Volver
            </button>
            <button type="submit" className="boton-primario" disabled={cargando}>
              {cargando ? 'Generando QR…' : 'Generar QR de Yape'}
            </button>
          </div>
        </form>
      ) : (
        <section className="tarjeta yape-qr">
          <p className="yape-qr-titulo">Escanea este QR con la app de Yape</p>
          {qr && <img className="yape-qr-imagen" src={qr} alt="Código QR de Yape para pagar el plan" />}
          <p className="yape-qr-codigo">
            Código de operación: <span className="pago-codigo">{operacion.codigo}</span>
          </p>
          <p className="yape-qr-detalle">
            Monto: <strong>{SOLES.format(operacion.monto)}</strong>
          </p>
          <p className="campo-ayuda">
            Cuando termines el pago en tu app, pulsa “Ya pagué, confirmar”.
          </p>

          <div className="pago-acciones">
            <button type="button" className="boton-secundario" onClick={alVolver} disabled={confirmando}>
              Volver
            </button>
            <button type="button" className="boton-primario" onClick={confirmar} disabled={confirmando}>
              {confirmando ? 'Confirmando…' : 'Ya pagué, confirmar'}
            </button>
          </div>
        </section>
      )}
    </main>
  )
}
