// ViewModel: lista suscripciones con filtro por estado (HU-08).
import { useEffect, useState } from 'react'
import { listarSuscripciones } from '../models/suscripcionesApi'

export function useSuscripciones() {
  const [suscripciones, setSuscripciones] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [filtroEstado, setFiltroEstado] = useState('')

  useEffect(() => {
    listarSuscripciones()
      .then(setSuscripciones)
      .catch(() => setError('No se pudieron cargar las suscripciones.'))
      .finally(() => setCargando(false))
  }, [])

  const filtradas = suscripciones.filter(s =>
    !filtroEstado || s.estado === filtroEstado
  )

  return { suscripciones: filtradas, cargando, error, filtroEstado, setFiltroEstado }
}