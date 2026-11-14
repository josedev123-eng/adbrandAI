// ViewModel: pregunta al backend qué pasarela usar para pagar con tarjeta (HU 26).
// Si el backend no tiene llaves de Mercado Pago, o no responde, se usa el formulario simulado de siempre.
import { useEffect, useState } from 'react'
import { obtenerConfiguracionPago } from '../models/pagosApi'

export const PASARELA_MERCADO_PAGO = 'mercadopago'
export const PASARELA_SIMULADA = 'simulada'

export function useConfiguracionPago() {
  const [configuracion, setConfiguracion] = useState(null)

  useEffect(() => {
    let activo = true
    async function cargar() {
      try {
        const respuesta = await obtenerConfiguracionPago()
        const usarMercadoPago = respuesta?.pasarela === PASARELA_MERCADO_PAGO && respuesta.publicKey
        if (activo) {
          setConfiguracion(
            usarMercadoPago
              ? { pasarela: PASARELA_MERCADO_PAGO, publicKey: respuesta.publicKey }
              : { pasarela: PASARELA_SIMULADA, publicKey: null },
          )
        }
      } catch {
        if (activo) setConfiguracion({ pasarela: PASARELA_SIMULADA, publicKey: null })
      }
    }
    cargar()
    return () => {
      activo = false
    }
  }, [])

  return { configuracion, cargando: configuracion === null }
}
