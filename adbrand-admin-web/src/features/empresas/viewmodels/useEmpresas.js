// ViewModel: lista de empresas, búsqueda y suspender/reactivar con su motivo.
import { useCallback, useEffect, useState } from 'react'
import { listarEmpresas, MOTIVO_MAXIMO, MOTIVO_MINIMO, reactivarEmpresa, suspenderEmpresa } from '../models/empresasApi'

export function validarMotivo(motivo) {
  const texto = motivo.trim()
  if (!texto) return 'Escribe el motivo de la suspensión.'
  if (texto.length < MOTIVO_MINIMO) return `Explica el motivo en al menos ${MOTIVO_MINIMO} caracteres.`
  if (texto.length > MOTIVO_MAXIMO) return `Máximo ${MOTIVO_MAXIMO} caracteres.`
  return null
}

export function useEmpresas() {
  const [empresas, setEmpresas] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [mensaje, setMensaje] = useState(null)
  const [busqueda, setBusqueda] = useState('')
  const [estado, setEstado] = useState('')
  // Empresa que se está suspendiendo (abre la ventana del motivo) o reactivando (pide confirmar).
  const [aSuspender, setASuspender] = useState(null)
  const [aReactivar, setAReactivar] = useState(null)
  const [motivo, setMotivo] = useState('')
  const [errorMotivo, setErrorMotivo] = useState(null)
  const [procesando, setProcesando] = useState(false)

  const cargar = useCallback(async (filtros) => {
    setCargando(true)
    setError(null)
    try {
      setEmpresas((await listarEmpresas(filtros)) ?? [])
    } catch (falla) {
      setError(falla?.message || 'No se pudieron cargar las empresas.')
    } finally {
      setCargando(false)
    }
  }, [])

  useEffect(() => {
    // Se busca un momento después de dejar de escribir, para no llamar a Django en cada tecla.
    const espera = setTimeout(() => cargar({ busqueda: busqueda.trim(), estado }), 300)
    return () => clearTimeout(espera)
  }, [busqueda, estado, cargar])

  function abrirSuspender(empresa) {
    setMensaje(null)
    setMotivo('')
    setErrorMotivo(null)
    setASuspender(empresa)
  }

  function cerrarVentanas() {
    setASuspender(null)
    setAReactivar(null)
  }

  function cambiarMotivo(valor) {
    setMotivo(valor)
    setErrorMotivo(null)
  }

  function actualizarFila(actualizada) {
    setEmpresas((actuales) => actuales.map((empresa) => (empresa.id === actualizada.id ? { ...empresa, ...actualizada } : empresa)))
  }

  async function confirmarSuspension() {
    const problema = validarMotivo(motivo)
    if (problema) {
      setErrorMotivo(problema)
      return
    }
    setProcesando(true)
    try {
      const actualizada = await suspenderEmpresa(aSuspender.id, motivo.trim())
      actualizarFila(actualizada)
      setMensaje({ tipo: 'exito', texto: `${aSuspender.nombre_empresa} quedó suspendida. Le enviamos un correo con el motivo.` })
      setASuspender(null)
    } catch (falla) {
      const delCampo = falla?.detalles?.motivo?.[0]
      if (delCampo) setErrorMotivo(delCampo)
      else setMensaje({ tipo: 'error', texto: falla?.message || 'No se pudo suspender la empresa.' })
    } finally {
      setProcesando(false)
    }
  }

  async function confirmarReactivacion() {
    setProcesando(true)
    try {
      const actualizada = await reactivarEmpresa(aReactivar.id)
      actualizarFila(actualizada)
      setMensaje({ tipo: 'exito', texto: `${aReactivar.nombre_empresa} ya puede volver a iniciar sesión.` })
      setAReactivar(null)
    } catch (falla) {
      setMensaje({ tipo: 'error', texto: falla?.message || 'No se pudo reactivar la empresa.' })
    } finally {
      setProcesando(false)
    }
  }

  return {
    empresas,
    cargando,
    error,
    mensaje,
    busqueda,
    setBusqueda,
    estado,
    setEstado,
    aSuspender,
    aReactivar,
    motivo,
    errorMotivo,
    procesando,
    abrirSuspender,
    abrirReactivar: (empresa) => {
      setMensaje(null)
      setAReactivar(empresa)
    },
    cerrarVentanas,
    cambiarMotivo,
    confirmarSuspension,
    confirmarReactivacion,
  }
}
