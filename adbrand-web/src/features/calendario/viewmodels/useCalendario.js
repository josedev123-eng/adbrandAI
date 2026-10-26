// ViewModel: carga el calendario del mes, navega entre meses y programa publicaciones (HU 33).
import { useEffect, useState } from 'react'
import { crearPublicacion, obtenerCalendario } from '../models/calendarioApi'

export function mesActual() {
  const hoy = new Date()
  return `${hoy.getFullYear()}-${String(hoy.getMonth() + 1).padStart(2, '0')}`
}

export function desplazarMes(mes, delta) {
  const [anio, numero] = mes.split('-').map(Number)
  const fecha = new Date(anio, numero - 1 + delta, 1)
  return `${fecha.getFullYear()}-${String(fecha.getMonth() + 1).padStart(2, '0')}`
}

export function etiquetaMes(mes) {
  const [anio, numero] = mes.split('-').map(Number)
  const texto = new Intl.DateTimeFormat('es-PE', { month: 'long', year: 'numeric' })
    .format(new Date(anio, numero - 1, 1))
  return texto.charAt(0).toUpperCase() + texto.slice(1)
}

export function diasDelMes(mes) {
  const [anio, numero] = mes.split('-').map(Number)
  return new Date(anio, numero, 0).getDate()
}

// Lunes = 0, domingo = 6, para que la semana empiece como en el calendario de la web.
export function primerDiaSemana(mes) {
  const [anio, numero] = mes.split('-').map(Number)
  return (new Date(anio, numero - 1, 1).getDay() + 6) % 7
}

export function fechaDeDia(mes, dia) {
  return `${mes}-${String(dia).padStart(2, '0')}`
}

export function useCalendario() {
  const [mes, setMes] = useState(mesActual())
  const [publicaciones, setPublicaciones] = useState([])
  const [cargando, setCargando] = useState(true)
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    let activo = true
    obtenerCalendario(mes)
      .then((lista) => {
        if (activo) setPublicaciones(lista)
      })
      .catch(() => {
        if (activo) setError('No se pudo cargar el calendario. Intenta de nuevo.')
      })
      .finally(() => {
        if (activo) setCargando(false)
      })
    return () => {
      activo = false
    }
  }, [mes])

  function cambiarMes(delta) {
    setCargando(true)
    setError(null)
    setMes((actual) => desplazarMes(actual, delta))
  }

  async function programar(datos) {
    setGuardando(true)
    setError(null)
    try {
      await crearPublicacion(datos)
      setPublicaciones(await obtenerCalendario(mes))
      return true
    } catch (fallo) {
      setError(fallo.message)
      return false
    } finally {
      setGuardando(false)
    }
  }

  return { mes, publicaciones, cargando, guardando, error, cambiarMes, programar }
}
