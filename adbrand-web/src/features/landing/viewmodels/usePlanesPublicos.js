// ViewModel: planes para la página de presentación. GET /planes es público (no necesita sesión).
// Si el servidor no responde, la página se sigue viendo: solo se oculta la lista de planes de pago.
import { useEffect, useState } from 'react'
import { obtenerPlanes } from '../../pagos/models/pagosApi'

export const LIMITE_GRATUITO = 10000

export function formatearPrecio(precio) {
  return `S/ ${Number(precio).toFixed(2)}`
}

export function formatearTokens(tokens) {
  return Number(tokens).toLocaleString('es-PE')
}

export function usePlanesPublicos() {
  const [planes, setPlanes] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(false)

  useEffect(() => {
    let vigente = true
    obtenerPlanes()
      .then((lista) => vigente && setPlanes(Array.isArray(lista) ? lista : []))
      .catch(() => vigente && setError(true))
      .finally(() => vigente && setCargando(false))
    return () => {
      vigente = false
    }
  }, [])

  return { planes, cargando, error }
}
