// View: elige el formulario de tarjeta (HU 26). Con llaves de Mercado Pago en el backend usa su formulario;
// si no, el formulario simulado de siempre.
import Cargando from '../../../shared/components/Cargando'
import { PASARELA_MERCADO_PAGO, useConfiguracionPago } from '../viewmodels/useConfiguracionPago'
import PagoMercadoPagoView from './PagoMercadoPagoView'
import PagoTarjetaView from './PagoTarjetaView'

export default function PagoConTarjetaView({ plan, alPagar, alVolver }) {
  const { configuracion, cargando } = useConfiguracionPago()

  if (cargando) {
    return (
      <main className="pagina">
        <Cargando texto="Preparando el pago..." filas={2} />
      </main>
    )
  }

  if (configuracion.pasarela === PASARELA_MERCADO_PAGO) {
    return <PagoMercadoPagoView plan={plan} publicKey={configuracion.publicKey} alPagar={alPagar} alVolver={alVolver} />
  }

  return <PagoTarjetaView plan={plan} alPagar={alPagar} alVolver={alVolver} />
}
