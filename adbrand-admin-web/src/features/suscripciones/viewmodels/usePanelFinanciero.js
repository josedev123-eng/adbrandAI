// ViewModel: datos del panel financiero del dueño de la aplicación (HU-05).
import { useEffect, useState } from 'react'
import { obtenerSuscripcionesActivas } from '../models/suscripcionesApi'

const ERROR_CARGA = 'No se pudo cargar el panel. Revisa que el servidor de administración esté encendido.'

export function usePanelFinanciero() {
  const [suscripcionesActivas, setSuscripcionesActivas] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [version, setVersion] = useState(0)

  // Se vuelve a ejecutar cada vez que cambia "version" (botón Actualizar).
  useEffect(() => {
    let vigente = true

    obtenerSuscripcionesActivas()
      .then((datos) => {
        if (!vigente) return
        setSuscripcionesActivas(datos?.total ?? 0)
        setError(null)
      })
      .catch(() => {
        if (vigente) setError(ERROR_CARGA)
      })
      .finally(() => {
        if (vigente) setCargando(false)
      })

    return () => {
      vigente = false
    }
  }, [version])

  function recargar() {
    setCargando(true)
    setVersion((actual) => actual + 1)
  }

  return { suscripcionesActivas, cargando, error, recargar }
}
