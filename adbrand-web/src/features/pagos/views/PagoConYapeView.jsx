// View: elige cómo pagar con Yape (HU 27). Con llaves de Mercado Pago, Yape real con el código de aprobación;
// si no, el QR simulado de siempre.
import Cargando from '../../../shared/components/Cargando'
import { PASARELA_MERCADO_PAGO, useConfiguracionPago } from '../viewmodels/useConfiguracionPago'
import PagoYapeMercadoPagoView from './PagoYapeMercadoPagoView'
import PagoYapeView from './PagoYapeView'

export default function PagoConYapeView({ plan, alConfirmar, alVolver }) {
  const { configuracion, cargando } = useConfiguracionPago()

  if (cargando) {
    return (
      <main className="pagina">
        <Cargando texto="Preparando el pago..." filas={2} />
      </main>
    )
  }

  if (configuracion.pasarela === PASARELA_MERCADO_PAGO) {
    return (
      <PagoYapeMercadoPagoView
        plan={plan}
        publicKey={configuracion.publicKey}
        alConfirmar={alConfirmar}
        alVolver={alVolver}
      />
    )
  }

  return <PagoYapeView plan={plan} alConfirmar={alConfirmar} alVolver={alVolver} />
}
