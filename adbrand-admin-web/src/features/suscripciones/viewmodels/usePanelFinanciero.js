// ViewModel: datos del panel financiero del dueño de la aplicación (HU-05, HU-06).
import { useEffect, useState } from 'react'
import { obtenerIngresosMensuales, obtenerSuscripcionesActivas } from '../models/suscripcionesApi'

const ERROR_CARGA = 'No se pudo cargar el panel. Revisa que el servidor de administración esté encendido.'

// HU-06: convierte los totales a número y calcula el ancho de cada barra frente al mejor mes.
function prepararIngresos(filas) {
  const ingresos = (filas ?? []).map((fila) => ({ mes: fila.mes, total: Number(fila.total) }))
  const mayor = Math.max(0, ...ingresos.map((ingreso) => ingreso.total))

  return ingresos.map((ingreso) => ({
    ...ingreso,
    porcentaje: mayor > 0 ? Math.round((ingreso.total / mayor) * 100) : 0,
  }))
}

export function usePanelFinanciero() {
  const [suscripcionesActivas, setSuscripcionesActivas] = useState(null)
  const [ingresos, setIngresos] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [version, setVersion] = useState(0)

  // Se vuelve a ejecutar cada vez que cambia "version" (botón Actualizar).
  useEffect(() => {
    let vigente = true

    Promise.all([obtenerSuscripcionesActivas(), obtenerIngresosMensuales()])
      .then(([activas, filas]) => {
        if (!vigente) return
        setSuscripcionesActivas(activas?.total ?? 0)
        setIngresos(prepararIngresos(filas))
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

  // Se suma en céntimos para no arrastrar decimales sueltos.
  const totalIngresos = ingresos.reduce((suma, ingreso) => suma + Math.round(ingreso.total * 100), 0) / 100

  return { suscripcionesActivas, ingresos, totalIngresos, cargando, error, recargar }
}
