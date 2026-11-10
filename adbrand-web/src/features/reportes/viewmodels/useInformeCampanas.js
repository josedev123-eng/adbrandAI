// ViewModel: carga el informe consolidado de campañas de la empresa (HU-37) y lo descarga en PDF (HU-38).
// HU-40: filtra el informe (y el PDF) por periodo y por campaña.
// HU-39: también lo descarga en Word, con los mismos filtros.
import { useEffect, useState } from 'react'
import { listarCampanas } from '../../calendario/models/campanasApi'
import {
  descargarInformePdf,
  descargarInformeWord,
  guardarArchivo,
  nombreArchivoInforme,
  obtenerInformeCampanas,
} from '../models/reportesApi'

const SIN_FILTROS = { desde: '', hasta: '', campanaId: '' }

export function hayFiltros(filtros) {
  return Boolean(filtros.desde || filtros.hasta || filtros.campanaId)
}

// HU-40, criterio 2: la misma regla que revisa Spring (FECHAS_INVALIDAS), para avisar sin consultar.
// Las fechas llegan como AAAA-MM-DD, así que se pueden comparar como texto.
export function validarPeriodo(filtros) {
  if (filtros.desde && filtros.hasta && filtros.desde > filtros.hasta) {
    return 'La fecha de inicio no puede ser posterior a la fecha de fin.'
  }
  return null
}

export function useInformeCampanas() {
  const [informe, setInforme] = useState(null)
  const [error, setError] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [descargando, setDescargando] = useState(false)
  const [descargandoWord, setDescargandoWord] = useState(false)

  // HU-40: "filtros" es lo que está escrito en el formulario; "aplicados" es lo que muestra el informe y usa el PDF.
  const [filtros, setFiltros] = useState(SIN_FILTROS)
  const [aplicados, setAplicados] = useState(SIN_FILTROS)
  const [actualizando, setActualizando] = useState(false)
  const [campanas, setCampanas] = useState([])

  useEffect(() => {
    obtenerInformeCampanas()
      .then(setInforme)
      .catch(() => setError('No se pudo cargar el informe. Revisa tu conexión e inténtalo de nuevo.'))
      .finally(() => setCargando(false))
  }, [])

  // Las campañas del calendario (HU 35) para el filtro. Si no cargan, se filtra solo por periodo.
  useEffect(() => {
    listarCampanas()
      .then((lista) => {
        if (Array.isArray(lista)) setCampanas(lista)
      })
      .catch(() => {})
  }, [])

  // Criterio 2: sin campañas no hay nada que descargar; la vista muestra un aviso.
  const sinCampanas = informe !== null && informe.total === 0
  const conFiltros = hayFiltros(aplicados)

  function cambiarFiltro(campo, valor) {
    setFiltros((actuales) => ({ ...actuales, [campo]: valor }))
  }

  async function consultar(nuevos) {
    setError(null)
    setActualizando(true)
    try {
      setInforme(await obtenerInformeCampanas(nuevos))
      setAplicados(nuevos)
      return true
    } catch (fallo) {
      setError(fallo.message ?? 'No se pudo cargar el informe. Revisa tu conexión e inténtalo de nuevo.')
      return false
    } finally {
      setActualizando(false)
    }
  }

  // Con el periodo al revés no se genera el informe: se avisa y el informe anterior queda como estaba.
  function aplicarFiltros() {
    const problema = validarPeriodo(filtros)
    if (problema) {
      setError(problema)
      return Promise.resolve(false)
    }
    return consultar(filtros)
  }

  function quitarFiltros() {
    setFiltros(SIN_FILTROS)
    return consultar(SIN_FILTROS)
  }

  async function descargarPdf() {
    setError(null)
    setDescargando(true)
    try {
      const pdf = conFiltros ? await descargarInformePdf(aplicados) : await descargarInformePdf()
      guardarArchivo(pdf, nombreArchivoInforme('pdf'))
    } catch {
      setError('No se pudo descargar el PDF. Revisa tu conexión e inténtalo de nuevo.')
    } finally {
      setDescargando(false)
    }
  }

  // HU-39: el Word sale con los filtros aplicados, igual que el PDF.
  async function descargarWord() {
    setError(null)
    setDescargandoWord(true)
    try {
      guardarArchivo(await descargarInformeWord(aplicados), nombreArchivoInforme('docx'))
    } catch {
      setError('No se pudo descargar el Word. Revisa tu conexión e inténtalo de nuevo.')
    } finally {
      setDescargandoWord(false)
    }
  }

  return {
    informe,
    error,
    cargando,
    sinCampanas,
    descargando,
    descargarPdf,
    descargandoWord,
    descargarWord,
    filtros,
    cambiarFiltro,
    aplicarFiltros,
    quitarFiltros,
    conFiltros,
    actualizando,
    campanas,
  }
}
