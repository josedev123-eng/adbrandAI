// ViewModel: HU 63 "Tu mes de contenido". Carga las fechas del mes y el plan sugerido, y deja cambiar de mes.
import { useEffect, useState } from 'react'
import { obtenerMesContenido } from '../models/mesContenidoApi'

export function useMesContenido() {
  const hoy = new Date()
  const [periodo, setPeriodo] = useState({ anio: hoy.getFullYear(), mes: hoy.getMonth() + 1 })
  const [datos, setDatos] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    let activo = true
    obtenerMesContenido(periodo.anio, periodo.mes)
      .then((respuesta) => {
        if (!activo) return
        setDatos(respuesta)
        setError(null)
      })
      .catch((e) => activo && setError(e?.message || 'No se pudo cargar tu mes de contenido.'))
      .finally(() => activo && setCargando(false))
    return () => {
      activo = false
    }
  }, [periodo])

  function moverMes(delta) {
    setCargando(true)
    setPeriodo(({ anio, mes }) => {
      const fecha = new Date(anio, mes - 1 + delta, 1)
      return { anio: fecha.getFullYear(), mes: fecha.getMonth() + 1 }
    })
  }

  return { periodo, datos, cargando, error, mesAnterior: () => moverMes(-1), mesSiguiente: () => moverMes(1) }
}
