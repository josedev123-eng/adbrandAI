// View: pago con Yape por Mercado Pago (HU 27). La lógica está en usePagoYapeMercadoPago.
import Aviso from '../../../shared/components/Aviso'
import { usePagoYapeMercadoPago } from '../viewmodels/usePagoYapeMercadoPago'
import { useConfirmarPago } from '../viewmodels/useConfirmarPago'
import ModalConfirmarPago from './ModalConfirmarPago'
import './pagoComun.css'
import './PagoYapeView.css'

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

export default function PagoYapeMercadoPagoView({ plan, publicKey, alConfirmar, alVolver }) {
  const { pedir, modal } = useConfirmarPago()
  const { formulario, errores, mensaje, enviando, cambiarCampo, pagar } = usePagoYapeMercadoPago(
    plan,
    publicKey,
    alConfirmar,
    pedir,
  )

  function alEnviar(evento) {
    evento.preventDefault()
    pagar()
  }

  return (
    <main className="pagina pagina-yape">
      <header className="pagina-cabecera yape-cabecera">
        <span className="yape-kicker">PAGO CON YAPE</span>
        <h1>Paga con tu código de aprobación</h1>
        <p>Pago seguro con Mercado Pago (modo de prueba: no se hace ningún cargo real).</p>
      </header>

      <section className="tarjeta pago-resumen yape-resumen">
        <span className="pago-resumen-etiqueta">Plan {plan.nombre}</span>
        <strong className="pago-resumen-monto">
          {SOLES.format(plan.precio)} <small>/ mes</small>
        </strong>
        <span className="pago-resumen-detalle">
          {Number(plan.limiteTokens).toLocaleString('es-PE')} tokens de IA al mes
        </span>
      </section>

      <form className="tarjeta formulario" onSubmit={alEnviar} noValidate>
        <div className="yape-instrucciones">
          <span className="yape-icono" aria-hidden="true">Y</span>
          <div>
            <strong>¿Dónde está el código?</strong>
            <p>Abre tu app Yape, entra al menú y toca «Código de aprobación». Dura unos minutos.</p>
          </div>
        </div>

        <Campo etiqueta="Celular de Yape" error={errores.celular}>
          <input
            type="tel"
            inputMode="numeric"
            autoComplete="tel-national"
            placeholder="Ej.: 987654321"
            maxLength={11}
            value={formulario.celular}
            onChange={(evento) => cambiarCampo('celular', evento.target.value)}
          />
        </Campo>

        <Campo etiqueta="Código de aprobación" error={errores.codigo} ayuda="6 dígitos">
          <input
            type="text"
            inputMode="numeric"
            autoComplete="one-time-code"
            placeholder="Ej.: 123456"
            maxLength={6}
            value={formulario.codigo}
            onChange={(evento) => cambiarCampo('codigo', evento.target.value)}
          />
        </Campo>

        <Campo etiqueta="Correo" error={errores.correo} ayuda="Para enviarte el comprobante de Mercado Pago">
          <input
            type="email"
            autoComplete="email"
            placeholder="Ej.: tucorreo@ejemplo.com"
            value={formulario.correo}
            maxLength={150}
            onChange={(evento) => cambiarCampo('correo', evento.target.value)}
          />
        </Campo>

        <Aviso mensaje={mensaje} como="p" />

        <div className="pago-acciones">
          <button type="button" className="boton-secundario" onClick={alVolver} disabled={enviando}>
            Volver
          </button>
          <button type="submit" className="boton-primario yape-boton" disabled={enviando}>
            {enviando ? 'Procesando…' : `Pagar ${SOLES.format(plan.precio)} con Yape`}
          </button>
        </div>
      </form>
      <ModalConfirmarPago {...modal} plan={plan} />
    </main>
  )
}
