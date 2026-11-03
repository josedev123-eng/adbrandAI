// ViewModel: carga el calendario del mes, navega entre meses y programa publicaciones (HU 33).
// HU 35: también carga las campañas, crea campañas nuevas y filtra el calendario por campaña.
import { useEffect, useState } from 'react'
import { crearPublicacion, obtenerCalendario } from '../models/calendarioApi'
import { crearCampana, listarCampanas } from '../models/campanasApi'

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

// HU 35: las mismas reglas que valida Spring, para avisar antes de enviar.
export function validarCampana(datos) {
  const errores = {}
  if (!datos.nombre.trim()) errores.nombre = 'Escribe el nombre de la campaña.'
  if (!/^#[0-9A-Fa-f]{6}$/.test(datos.color)) errores.color = 'Escribe el color como #RRGGBB.'
  if (!datos.fechaInicio) errores.fechaInicio = 'Selecciona la fecha de inicio.'
  if (!datos.fechaFin) errores.fechaFin = 'Selecciona la fecha de fin.'
  if (datos.fechaInicio && datos.fechaFin && datos.fechaFin < datos.fechaInicio) {
    errores.fechaFin = 'La fecha de fin no puede ser anterior a la fecha de inicio.'
  }
  return errores
}

// Sin filtro se llama como en la HU 33 (solo con el mes).
function pedirCalendario(mes, campanaId) {
  return campanaId ? obtenerCalendario(mes, campanaId) : obtenerCalendario(mes)
}

export function useCalendario() {
  const [mes, setMes] = useState(mesActual())
  const [publicaciones, setPublicaciones] = useState([])
  const [cargando, setCargando] = useState(true)
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState(null)

  // HU 35
  const [campanas, setCampanas] = useState([])
  const [campanaFiltro, setCampanaFiltro] = useState('')
  const [erroresCampana, setErroresCampana] = useState({})
  const [guardandoCampana, setGuardandoCampana] = useState(false)

  useEffect(() => {
    let activo = true
    pedirCalendario(mes, campanaFiltro)
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
  }, [mes, campanaFiltro])

  // Si las campañas no cargan, el calendario se sigue usando sin ellas.
  useEffect(() => {
    let activo = true
    listarCampanas()
      .then((lista) => {
        if (activo && Array.isArray(lista)) setCampanas(lista)
      })
      .catch(() => {})
    return () => {
      activo = false
    }
  }, [])

  function cambiarMes(delta) {
    setCargando(true)
    setError(null)
    setMes((actual) => desplazarMes(actual, delta))
  }

  function filtrarPorCampana(campanaId) {
    setCargando(true)
    setError(null)
    setCampanaFiltro(campanaId)
  }

  async function programar(datos) {
    setGuardando(true)
    setError(null)
    try {
      await crearPublicacion(datos)
      setPublicaciones(await pedirCalendario(mes, campanaFiltro))
      return true
    } catch (fallo) {
      setError(fallo.message)
      return false
    } finally {
      setGuardando(false)
    }
  }

  async function agregarCampana(datos) {
    const errores = validarCampana(datos)
    setErroresCampana(errores)
    if (Object.keys(errores).length > 0) return false

    setGuardandoCampana(true)
    try {
      const nueva = await crearCampana({ ...datos, nombre: datos.nombre.trim() })
      setCampanas((actuales) =>
        [...actuales, nueva].sort((a, b) => a.fechaInicio.localeCompare(b.fechaInicio)),
      )
      return true
    } catch (fallo) {
      const campos = fallo.campos ?? {}
      setErroresCampana(Object.keys(campos).length > 0 ? campos : { general: fallo.message })
      return false
    } finally {
      setGuardandoCampana(false)
    }
  }

  return {
    mes,
    publicaciones,
    cargando,
    guardando,
    error,
    cambiarMes,
    programar,
    campanas,
    campanaFiltro,
    filtrarPorCampana,
    agregarCampana,
    erroresCampana,
    guardandoCampana,
  }
}
