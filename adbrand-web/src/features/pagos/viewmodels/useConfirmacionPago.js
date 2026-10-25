// ViewModel: pide el comprobante del pago recién confirmado (HU 28).
import { useEffect, useState } from 'react'
import { obtenerComprobante } from '../models/pagosApi'

export function useConfirmacionPago(pago) {
  const [comprobante, setComprobante] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    let activo = true

    obtenerComprobante(pago.id)
      .then((datos) => {
        if (activo) setComprobante(datos)
      })
      .catch(() => {
        if (activo) setError('No se pudo cargar el comprobante. Intenta de nuevo.')
      })
      .finally(() => {
        if (activo) setCargando(false)
      })

    return () => {
      activo = false
    }
  }, [pago.id])

  return { comprobante, cargando, error }
}
