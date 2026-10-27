// ViewModel: historial de contenidos aprobados y rechazados con filtros y páginas (HU-16).
import { useEffect, useState } from 'react'
import { listarHistorial } from '../models/moderacionApi'

const SIN_FILTROS = { estado: '', desde: '', hasta: '' }
export const AVISO_FECHAS = 'La fecha de inicio no puede ser posterior a la fecha de fin'
const ERROR_CARGA = 'No se pudo cargar el historial. Revisa que el servidor de administración esté encendido.'

export function useHistorialContenidos() {
  // "filtros" es lo que se está eligiendo; "aplicados" es lo último que se consultó.
  const [filtros, setFiltros] = useState(SIN_FILTROS)
  const [aplicados, setAplicados] = useState(SIN_FILTROS)
  const [pagina, setPagina] = useState(1)
  const [historial, setHistorial] = useState(null)
  const [aviso, setAviso] = useState(null)
  const [error, setError] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [abiertoId, setAbiertoId] = useState(null)

  // Se vuelve a consultar cada vez que cambian los filtros aplicados o la página.
  useEffect(() => {
    let vigente = true

    listarHistorial({ ...aplicados, pagina })
      .then((datos) => {
        if (!vigente) return
        setHistorial(datos)
        setError(null)
      })
      .catch((e) => {
        if (vigente) setError(e?.detalles?.desde?.[0] || e?.detalles?.hasta?.[0] || ERROR_CARGA)
      })
      .finally(() => {
        if (vigente) setCargando(false)
      })

    return () => {
      vigente = false
    }
  }, [aplicados, pagina])

  function cambiarFiltro(campo, valor) {
    setFiltros((actuales) => ({ ...actuales, [campo]: valor }))
    setAviso(null)
  }

  function filtrar(evento) {
    evento?.preventDefault()
    // Criterio 2 con aviso: las fechas llegan como AAAA-MM-DD y se pueden comparar como texto.
    if (filtros.desde && filtros.hasta && filtros.desde > filtros.hasta) {
      setAviso(AVISO_FECHAS)
      return
    }
    setAviso(null)
    setCargando(true)
    setAbiertoId(null)
    setPagina(1)
    setAplicados({ ...filtros })
  }

  function irAPagina(numero) {
    setCargando(true)
    setAbiertoId(null)
    setPagina(numero)
  }

  // Al hacer clic en una fila se ve (o se oculta) el texto completo.
  function alternarTexto(id) {
    setAbiertoId((actual) => (actual === id ? null : id))
  }

  return { filtros, historial, aviso, error, cargando, abiertoId, cambiarFiltro, filtrar, irAPagina, alternarTexto }
}
