// View: pago con tarjeta por Mercado Pago (HU 26). El formulario lo dibuja Mercado Pago dentro del
// contenedor; la lógica está en usePagoMercadoPago.
import Aviso from '../../../shared/components/Aviso'
import Cargando from '../../../shared/components/Cargando'
import { ID_CONTENEDOR, usePagoMercadoPago } from '../viewmodels/usePagoMercadoPago'
import { useConfirmarPago } from '../viewmodels/useConfirmarPago'
import ModalConfirmarPago from './ModalConfirmarPago'
import './pagoComun.css'
import './PagoTarjetaView.css'

const SOLES = new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' })

export default function PagoMercadoPagoView({ plan, publicKey, alPagar, alVolver }) {
  const { pedir, modal } = useConfirmarPago()
  const { listo, mensaje, enviando } = usePagoMercadoPago(plan, publicKey, alPagar, pedir)

  return (
    <main className="pagina">
      <header className="pagina-cabecera">
        <h1>Pagar con tarjeta</h1>
        <p>Pago seguro con Mercado Pago (modo de prueba: no se hace ningún cargo real).</p>
      </header>

      <section className="tarjeta pago-resumen">
        <span className="pago-resumen-etiqueta">Plan {plan.nombre}</span>
        <strong className="pago-resumen-monto">{SOLES.format(plan.precio)} / mes</strong>
        <span className="pago-resumen-detalle">
          {Number(plan.limiteTokens).toLocaleString('es-PE')} tokens de IA al mes
        </span>
      </section>

      <section className="tarjeta formulario" aria-label="Datos de la tarjeta">
        {!listo && !mensaje && <Cargando texto="Cargando el formulario de Mercado Pago..." filas={2} />}
        <div id={ID_CONTENEDOR} />
        {enviando && <p className="aviso">Procesando el pago...</p>}
        <Aviso mensaje={mensaje} como="p" />
        <p className="campo-ayuda">Tus datos de tarjeta los recibe Mercado Pago; AdBrand no los guarda.</p>

        <div className="pago-acciones">
          <button type="button" className="boton-secundario" onClick={alVolver} disabled={enviando}>
            Volver
          </button>
        </div>
      </section>
      <ModalConfirmarPago {...modal} plan={plan} />
    </main>
  )
}
