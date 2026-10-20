// ViewModel: carga los planes de suscripción (HU 25).
import { useEffect, useState } from 'react'
import { obtenerPlanes } from '../models/pagosApi'

export function usePlanes() {
  const [planes, setPlanes] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    let activo = true
    obtenerPlanes()
      .then((lista) => {
        if (activo) setPlanes(lista)
      })
      .catch(() => {
        if (activo) setError('No se pudieron cargar los planes. Intenta de nuevo.')
      })
      .finally(() => {
        if (activo) setCargando(false)
      })
    return () => {
      activo = false
    }
  }, [])

  return { planes, cargando, error }
}
