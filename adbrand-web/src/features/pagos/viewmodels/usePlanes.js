// ViewModel: carga los planes de suscripción (HU 25).
import { useEffect, useState } from 'react'
import { obtenerConsumoMes, obtenerPlanes } from '../models/pagosApi'

export function usePlanes() {
  const [planes, setPlanes] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  // Plan que la empresa tiene hoy: { nombre } de un plan pagado, o { gratuito: true }. null si no se pudo saber.
  const [planActual, setPlanActual] = useState(null)

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
    obtenerConsumoMes()
      .then((consumo) => {
        if (!activo) return
        if (consumo.origen === 'PLAN') setPlanActual({ nombre: consumo.plan, limite: consumo.limite })
        else if (consumo.origen === 'GRATUITO') setPlanActual({ gratuito: true, limite: consumo.limite })
      })
      .catch(() => {})
    return () => {
      activo = false
    }
  }, [])

  return { planes, cargando, error, planActual }
}
