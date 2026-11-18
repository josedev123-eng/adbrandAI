// ViewModel: carga el calendario del mes, navega entre meses y programa publicaciones (HU 33).
// HU 35: también carga las campañas, crea campañas nuevas y filtra el calendario por campaña.
// HU 34: carga los anuncios aprobados para elegir cuál programar y no deja programar en un día que ya pasó.
import { useEffect, useState } from 'react'
import { listarAnuncios } from '../models/anunciosApi'
import { crearPublicacion, obtenerCalendario, reprogramarPublicacion } from '../models/calendarioApi'
import { crearCampana, listarCampanas } from '../models/campanasApi'

export function hoyIso() {
  const hoy = new Date()
  return `${hoy.getFullYear()}-${String(hoy.getMonth() + 1).padStart(2, '0')}-${String(hoy.getDate()).padStart(2, '0')}`
}

// HU 34: aquí solo se revisa el día; la hora exacta la revisa Spring (FECHA_PASADA).
export function esDiaPasado(fecha) {
  return Boolean(fecha) && fecha < hoyIso()
}

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

// HU 35: campañas que están vigentes ese día (fechaInicio <= fecha <= fechaFin), para pintar la celda.
// Las fechas llegan como AAAA-MM-DD, así que se comparan como texto.
export function campanasDelDia(campanas, fecha) {
  return campanas.filter((campana) => campana.fechaInicio <= fecha && fecha <= campana.fechaFin)
}

// "2026-12-01" → "2026-12": el mes al que salta el calendario al elegir una campaña.
export function mesDeFecha(fecha) {
  return fecha.slice(0, 7)
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

  // HU 34: solo los anuncios aprobados se pueden programar.
  const [anuncios, setAnuncios] = useState([])
  const [errorProgramar, setErrorProgramar] = useState(null)

  // HU 36: aviso cuando una publicación no se pudo mover y volvió a su día.
  const [avisoArrastre, setAvisoArrastre] = useState(null)

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

  // Si los anuncios no cargan, se puede programar sin elegir uno, como en la HU 33.
  useEffect(() => {
    let activo = true
    listarAnuncios()
      .then((lista) => {
        if (activo && Array.isArray(lista)) setAnuncios(lista.filter((anuncio) => anuncio.estado === 'APROBADO'))
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

  // HU 35: al elegir una campaña, el calendario va al mes en que empieza.
  // Con "Todas las publicaciones" se queda en el mes que se estaba viendo.
  function filtrarPorCampana(campanaId) {
    setCargando(true)
    setError(null)
    setCampanaFiltro(campanaId)
    const campana = campanas.find((item) => String(item.id) === String(campanaId))
    if (campana) setMes(mesDeFecha(campana.fechaInicio))
  }

  // HU 34: el error se muestra junto al formulario (errorProgramar), no arriba del calendario.
  async function programar(datos) {
    setError(null)
    setErrorProgramar(null)
    if (esDiaPasado(datos.fecha)) {
      setErrorProgramar('No puedes programar en una fecha u hora que ya pasó.')
      return false
    }

    setGuardando(true)
    try {
      await crearPublicacion(datos)
      setPublicaciones(await pedirCalendario(mes, campanaFiltro))
      return true
    } catch (fallo) {
      setErrorProgramar(fallo.message)
      return false
    } finally {
      setGuardando(false)
    }
  }

  // HU 36: al soltar una publicación en otro día se guarda la fecha nueva y se actualiza esa publicación.
  // Se mueve en pantalla de inmediato; si Spring no acepta el cambio, vuelve a su día y sale el aviso.
  async function moverPublicacion(id, fecha) {
    const publicacion = publicaciones.find((item) => item.id === id)
    if (!publicacion || publicacion.fecha === fecha) return false

    setAvisoArrastre(null)
    // HU 36, criterio 2: a un día pasado no se mueve; ni siquiera se llama a Spring.
    if (esDiaPasado(fecha)) {
      setAvisoArrastre('No puedes mover una publicación a un día que ya pasó.')
      return false
    }

    const fechaOriginal = publicacion.fecha
    setPublicaciones((actuales) => actuales.map((item) => (item.id === id ? { ...item, fecha } : item)))
    try {
      const actualizada = await reprogramarPublicacion(id, fecha)
      setPublicaciones((actuales) => actuales.map((item) => (item.id === id ? actualizada : item)))
      return true
    } catch (fallo) {
      setPublicaciones((actuales) =>
        actuales.map((item) => (item.id === id ? { ...item, fecha: fechaOriginal } : item)),
      )
      setAvisoArrastre(fallo.message)
      return false
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
    anuncios,
    errorProgramar,
    moverPublicacion,
    avisoArrastre,
  }
}
