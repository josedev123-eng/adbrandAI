// ViewModel: carga el informe consolidado de campañas de la empresa (HU-37).
import { useEffect, useState } from 'react'
import { obtenerInformeCampanas } from '../models/reportesApi'

export function useInformeCampanas() {
  const [informe, setInforme] = useState(null)
  const [error, setError] = useState(null)
  const [cargando, setCargando] = useState(true)

  useEffect(() => {
    obtenerInformeCampanas()
      .then(setInforme)
      .catch(() => setError('No se pudo cargar el informe. Revisa tu conexión e inténtalo de nuevo.'))
      .finally(() => setCargando(false))
  }, [])

  // Criterio 2: sin campañas no hay nada que descargar; la vista muestra un aviso.
  const sinCampanas = informe !== null && informe.total === 0

  return { informe, error, cargando, sinCampanas }
}
