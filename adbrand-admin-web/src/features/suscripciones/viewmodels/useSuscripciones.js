// ViewModel: lista las suscripciones con deuda y permite filtrar por estado (HU-08).
import { useEffect, useState } from 'react'
import { listarSuscripcionesPorEstado } from '../models/suscripcionesApi'

export function useSuscripciones() {
  const [suscripciones, setSuscripciones] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [filtroEstado, setFiltroEstado] = useState('')

  useEffect(() => {
    let vigente = true
    setCargando(true)
    setError(null)

    listarSuscripcionesPorEstado(filtroEstado)
      .then(datos => {
        if (vigente) setSuscripciones(datos ?? [])
      })
      .catch(() => {
        if (vigente) setError('No se pudieron cargar las suscripciones.')
      })
      .finally(() => {
        if (vigente) setCargando(false)
      })

    return () => {
      vigente = false
    }
  }, [filtroEstado])

  return { suscripciones, cargando, error, filtroEstado, setFiltroEstado }
}
