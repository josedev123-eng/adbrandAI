// View: pago con Yape. Genera el QR de la cuenta de AdBrand y luego confirma la operación.
import { usePagoYape } from '../viewmodels/usePagoYape'
import './pagoComun.css'
import './PagoYapeView.css'

const SOLES = new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' })

export default function PagoYapeView({ plan, alConfirmar, alVolver }) {
  const { operacion, qr, cargando, confirmando, error, generar, confirmar } = usePagoYape(plan, alConfirmar)

  function alGenerar(evento) {
    evento.preventDefault()
    generar()
  }

  return (
    <main className="pagina pagina-yape">
      <header className="pagina-cabecera yape-cabecera">
        <span className="yape-kicker">PAGO CON YAPE</span>
        <h1>Activa tu plan en minutos</h1>
        <p>Escanea el QR desde Yape y confirma tu pago para continuar.</p>
      </header>

      <section className="tarjeta pago-resumen yape-resumen">
        <span className="pago-resumen-etiqueta">Plan {plan.nombre}</span>
        <strong className="pago-resumen-monto">{SOLES.format(plan.precio)} <small>/ mes</small></strong>
        <span className="pago-resumen-detalle">{Number(plan.limiteTokens).toLocaleString('es-PE')} tokens de IA al mes</span>
      </section>

      {error && <p className="aviso aviso-error">{error}</p>}

      {!operacion ? (
        <form className="tarjeta formulario yape-inicio" onSubmit={alGenerar} noValidate>
          <div className="yape-instrucciones">
            <span className="yape-icono" aria-hidden="true">Y</span>
            <div>
              <strong>Cuenta Yape de AdBrand</strong>
              <p>El código QR se asociará al número <b>987 654 321</b>.</p>
            </div>
          </div>
          <div className="pago-acciones">
            <button type="button" className="boton-secundario" onClick={alVolver} disabled={cargando}>Volver</button>
            <button type="submit" className="boton-primario yape-boton" disabled={cargando}>
              {cargando ? 'Preparando tu QR…' : 'Generar QR de pago'}
            </button>
          </div>
        </form>
      ) : (
        <section className="tarjeta yape-qr">
          <span className="yape-estado">Operación lista</span>
          <p className="yape-qr-titulo">Escanea con Yape para pagar a AdBrand</p>
          {qr && <img className="yape-qr-imagen" src={qr} alt="Código QR de Yape para pagar el plan" />}
          <p className="yape-destino">Yape destino <strong>987 654 321</strong></p>
          <p className="yape-qr-codigo">Código de operación: <span className="pago-codigo">{operacion.codigo}</span></p>
          <p className="yape-qr-detalle">Monto: <strong>{SOLES.format(operacion.monto)}</strong></p>
          <p className="campo-ayuda">Cuando termines el pago en tu app, pulsa “Ya pagué, confirmar”.</p>
          <div className="pago-acciones">
            <button type="button" className="boton-secundario" onClick={alVolver} disabled={confirmando}>Volver</button>
            <button type="button" className="boton-primario yape-boton" onClick={confirmar} disabled={confirmando}>
              {confirmando ? 'Confirmando…' : 'Ya pagué, confirmar'}
            </button>
          </div>
        </section>
      )}
    </main>
  )
}
