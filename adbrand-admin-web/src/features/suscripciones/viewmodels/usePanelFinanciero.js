// ViewModel: datos del panel financiero del dueño de la aplicación (HU-05, HU-06, HU-07).
import { useEffect, useState } from 'react'
import { obtenerIngresosMensuales, obtenerSuscripcionesActivas } from '../models/suscripcionesApi'

const ERROR_CARGA = 'No se pudo cargar el panel. Revisa que el servidor de administración esté encendido.'

// HU-07: rango de meses para comparar periodos. Los mismos límites que valida Django.
const SIN_RANGO = { desde: '', hasta: '' }
const MES_VALIDO = /^\d{4}-(0[1-9]|1[0-2])$/
const MAXIMO_MESES = 24

function mesesEntre(desde, hasta) {
  const [anioDesde, mesDesde] = desde.split('-').map(Number)
  const [anioHasta, mesHasta] = hasta.split('-').map(Number)
  return (anioHasta - anioDesde) * 12 + (mesHasta - mesDesde) + 1
}

// Devuelve el mensaje de error, o null si el rango se puede consultar.
function validarRango({ desde, hasta }) {
  if (!desde || !hasta) return 'Elige el mes inicial y el mes final.'
  if (!MES_VALIDO.test(desde) || !MES_VALIDO.test(hasta)) return 'Escribe los meses con el formato AAAA-MM.'
  // Los meses llegan como AAAA-MM, así que se pueden comparar como texto.
  if (desde > hasta) return 'El mes inicial no puede ser mayor que el final.'
  if (mesesEntre(desde, hasta) > MAXIMO_MESES) return `El rango no puede pasar de ${MAXIMO_MESES} meses.`
  return null
}

// Django responde { campo: ["mensaje"] } cuando rechaza el rango.
function mensajeDelServidor(detalles) {
  const mensajes = Object.values(detalles ?? {}).flat()
  return mensajes[0] ?? 'El rango de meses no es válido.'
}

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
  // "rango" es lo que se está escribiendo; "rangoAplicado" es lo último que se consultó.
  const [rango, setRango] = useState(SIN_RANGO)
  const [rangoAplicado, setRangoAplicado] = useState(SIN_RANGO)
  const [errorRango, setErrorRango] = useState(null)

  // Se vuelve a ejecutar al pulsar Actualizar ("version") o al aplicar otro rango.
  useEffect(() => {
    let vigente = true

    Promise.all([obtenerSuscripcionesActivas(), obtenerIngresosMensuales(rangoAplicado)])
      .then(([activas, filas]) => {
        if (!vigente) return
        setSuscripcionesActivas(activas?.total ?? 0)
        setIngresos(prepararIngresos(filas))
        setError(null)
      })
      .catch((fallo) => {
        if (!vigente) return
        // Un 400 es un rango que Django no acepta; lo demás es que no se pudo conectar.
        if (fallo?.status === 400) setErrorRango(mensajeDelServidor(fallo.detalles))
        else setError(ERROR_CARGA)
      })
      .finally(() => {
        if (vigente) setCargando(false)
      })

    return () => {
      vigente = false
    }
  }, [version, rangoAplicado])

  function recargar() {
    setCargando(true)
    setVersion((actual) => actual + 1)
  }

  function cambiarRango(campo, valor) {
    setRango((actual) => ({ ...actual, [campo]: valor }))
    setErrorRango(null)
  }

  // HU-07: valida antes de llamar a la API; si el rango es inválido no se consulta nada.
  function aplicarRango() {
    const problema = validarRango(rango)
    if (problema) {
      setErrorRango(problema)
      return
    }
    setErrorRango(null)
    setCargando(true)
    setRangoAplicado({ ...rango })
  }

  function quitarRango() {
    setRango(SIN_RANGO)
    setErrorRango(null)
    setCargando(true)
    setRangoAplicado({ ...SIN_RANGO })
  }

  // Se suma en céntimos para no arrastrar decimales sueltos.
  const totalIngresos = ingresos.reduce((suma, ingreso) => suma + Math.round(ingreso.total * 100), 0) / 100
  const hayRango = Boolean(rangoAplicado.desde && rangoAplicado.hasta)

  return {
    suscripcionesActivas,
    ingresos,
    totalIngresos,
    cargando,
    error,
    recargar,
    rango,
    hayRango,
    errorRango,
    cambiarRango,
    aplicarRango,
    quitarRango,
  }
}
