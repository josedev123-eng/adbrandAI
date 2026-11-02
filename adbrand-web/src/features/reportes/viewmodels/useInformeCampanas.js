// ViewModel: carga el informe consolidado de campañas de la empresa (HU-37) y lo descarga en PDF (HU-38).
import { useEffect, useState } from 'react'
import { descargarInformePdf, guardarArchivo, nombreArchivoInforme, obtenerInformeCampanas } from '../models/reportesApi'

export function useInformeCampanas() {
  const [informe, setInforme] = useState(null)
  const [error, setError] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [descargando, setDescargando] = useState(false)

  useEffect(() => {
    obtenerInformeCampanas()
      .then(setInforme)
      .catch(() => setError('No se pudo cargar el informe. Revisa tu conexión e inténtalo de nuevo.'))
      .finally(() => setCargando(false))
  }, [])

  // Criterio 2: sin campañas no hay nada que descargar; la vista muestra un aviso.
  const sinCampanas = informe !== null && informe.total === 0

  async function descargarPdf() {
    setError(null)
    setDescargando(true)
    try {
      guardarArchivo(await descargarInformePdf(), nombreArchivoInforme('pdf'))
    } catch {
      setError('No se pudo descargar el PDF. Revisa tu conexión e inténtalo de nuevo.')
    } finally {
      setDescargando(false)
    }
  }

  return { informe, error, cargando, sinCampanas, descargando, descargarPdf }
}
