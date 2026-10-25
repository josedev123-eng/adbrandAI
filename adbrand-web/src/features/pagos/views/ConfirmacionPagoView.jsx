// View: confirmación del pago con su comprobante (HU 28). La descarga y el envío por
// correo los resuelve el backend; aquí solo se muestra el resultado.
import { useConfirmacionPago } from '../viewmodels/useConfirmacionPago'
import './pagoComun.css'
import './ConfirmacionPagoView.css'

const SOLES = new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' })

const FECHA = new Intl.DateTimeFormat('es-PE', { dateStyle: 'long' })

function fecha(valor) {
  if (!valor) return '—'
  return FECHA.format(new Date(`${valor}T00:00:00`))
}

export default function ConfirmacionPagoView({ pago, alVolver }) {
  const { comprobante, cargando, error } = useConfirmacionPago(pago)

  return (
    <main className="pagina">
      <header className="pagina-cabecera confirmacion-cabecera">
        <span className="confirmacion-check" aria-hidden="true">✓</span>
        <div>
          <h1>Pago confirmado</h1>
          <p>Tu suscripción ya está activa. Gracias por confiar en AdBrand.AI.</p>
        </div>
      </header>

      {cargando && <p className="cargando">Generando tu comprobante…</p>}
      {error && <p className="aviso aviso-error">{error}</p>}

      {comprobante && (
        <section className="tarjeta comprobante">
          <div className="comprobante-cabecera">
            <div>
              <span className="comprobante-etiqueta">Comprobante</span>
              <strong className="comprobante-numero">{comprobante.numeroComprobante}</strong>
            </div>
            <span className="comprobante-fecha">{fecha(comprobante.fechaPago)}</span>
          </div>

          <dl className="comprobante-detalle">
            <div>
              <dt>Cliente</dt>
              <dd>{comprobante.cliente}</dd>
            </div>
            <div>
              <dt>Plan</dt>
              <dd>{comprobante.plan}</dd>
            </div>
            <div>
              <dt>Método de pago</dt>
              <dd>{comprobante.metodo === 'YAPE' ? 'Yape' : 'Tarjeta'}</dd>
            </div>
            <div>
              <dt>Referencia</dt>
              <dd className="pago-codigo">{comprobante.referencia ?? '—'}</dd>
            </div>
          </dl>

          <p className="comprobante-total">
            <span>Total pagado</span>
            <strong>{SOLES.format(comprobante.monto)}</strong>
          </p>

          <p className="campo-ayuda">
            {comprobante.correoEnviado
              ? `Enviamos tu comprobante a ${comprobante.correo}.`
              : 'Puedes guardar este comprobante desde tu navegador.'}
          </p>
        </section>
      )}

      <div className="pago-acciones">
        <button type="button" className="boton-secundario" onClick={() => window.print()} disabled={!comprobante}>
          Imprimir comprobante
        </button>
        <button type="button" className="boton-primario" onClick={alVolver}>
          Volver a los planes
        </button>
      </div>
    </main>
  )
}
