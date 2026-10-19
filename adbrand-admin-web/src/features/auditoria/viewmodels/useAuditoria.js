// ViewModel: carga el historial de acciones y maneja los filtros por administrador y fechas (HU-04).
import { useEffect, useState } from 'react'
import { listarAdministradores } from '../../admin/models/administradoresApi'
import { listarAuditoria } from '../models/auditoriaApi'

const SIN_FILTROS = { adminId: '', desde: '', hasta: '' }
const ERROR_CARGA = 'No se pudo cargar el historial. Revisa que el servidor de administración esté encendido.'

export function useAuditoria() {
  const [registros, setRegistros] = useState([])
  const [administradores, setAdministradores] = useState([])
  // "filtros" es lo que se está escribiendo; "aplicados" es lo último que se consultó.
  const [filtros, setFiltros] = useState(SIN_FILTROS)
  const [aplicados, setAplicados] = useState(SIN_FILTROS)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [errorFiltros, setErrorFiltros] = useState(null)

  // La lista de administradores llena el selector del filtro.
  useEffect(() => {
    listarAdministradores()
      .then((datos) => setAdministradores(datos ?? []))
      .catch(() => setAdministradores([]))
  }, [])

  // Se vuelve a consultar cada vez que cambian los filtros aplicados.
  useEffect(() => {
    let vigente = true

    listarAuditoria(aplicados)
      .then((datos) => {
        if (!vigente) return
        setRegistros(datos ?? [])
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
  }, [aplicados])

  function cambiarFiltro(campo, valor) {
    setFiltros((actuales) => ({ ...actuales, [campo]: valor }))
    setErrorFiltros(null)
  }

  function aplicarFiltros() {
    // Las fechas llegan como AAAA-MM-DD, así que se pueden comparar como texto.
    if (filtros.desde && filtros.hasta && filtros.desde > filtros.hasta) {
      setErrorFiltros('La fecha inicial no puede ser mayor que la final.')
      return
    }
    setErrorFiltros(null)
    setCargando(true)
    setAplicados({ ...filtros })
  }

  function limpiarFiltros() {
    setFiltros(SIN_FILTROS)
    setErrorFiltros(null)
    setCargando(true)
    setAplicados({ ...SIN_FILTROS })
  }

  const hayFiltros = Boolean(aplicados.adminId || aplicados.desde || aplicados.hasta)

  return {
    registros,
    administradores,
    filtros,
    hayFiltros,
    cargando,
    error,
    errorFiltros,
    cambiarFiltro,
    aplicarFiltros,
    limpiarFiltros,
  }
}
