// ViewModel: historial de consumo de tokens por periodo (HU-20).
// Criterio 2: si el inicio es posterior al fin se muestra un aviso y no se llama a la API.
import { useCallback, useEffect, useState } from 'react'
import { obtenerHistorial } from '../models/tokensApi'

export const AVISO_PERIODO = 'La fecha de inicio no puede ser posterior a la fecha de fin'
const AVISO_FALTAN = 'Elige el mes de inicio y el de fin.'
const ERROR_CARGA = 'No se pudo cargar el historial. Revisa que el servidor de administración esté encendido.'

// Mes como "AAAA-MM", el mismo formato que usan los <input type="month">.
function comoMes(fecha) {
  return `${fecha.getFullYear()}-${String(fecha.getMonth() + 1).padStart(2, '0')}`
}

// Por defecto: los últimos 6 meses contando el actual.
export function periodoPorDefecto(hoy = new Date()) {
  return {
    desde: comoMes(new Date(hoy.getFullYear(), hoy.getMonth() - 5, 1)),
    hasta: comoMes(hoy),
  }
}

// Django responde { desde: ["mensaje"] } cuando el periodo no es válido.
function mensajeDeError(error) {
  const detalles = error?.detalles
  return detalles?.desde?.[0] || detalles?.hasta?.[0] || ERROR_CARGA
}

export function useHistorialTokens() {
  const [periodo, setPeriodo] = useState(periodoPorDefecto)
  const [historial, setHistorial] = useState(null)
  const [aviso, setAviso] = useState(null)
  const [error, setError] = useState(null)
  const [cargando, setCargando] = useState(true)

  const pedir = useCallback(
    ({ desde, hasta }) =>
      obtenerHistorial(desde, hasta)
        .then((datos) => {
          setHistorial(datos)
          setError(null)
        })
        .catch((e) => setError(mensajeDeError(e)))
        .finally(() => setCargando(false)),
    [],
  )

  // Al abrir la pantalla se consultan los últimos 6 meses.
  useEffect(() => {
    pedir(periodoPorDefecto())
  }, [pedir])

  function cambiarPeriodo(campo, valor) {
    setPeriodo((actual) => ({ ...actual, [campo]: valor }))
    setAviso(null)
  }

  function consultar(evento) {
    evento?.preventDefault()
    if (!periodo.desde || !periodo.hasta) {
      setAviso(AVISO_FALTAN)
      return null
    }
    // Los meses "AAAA-MM" se pueden comparar como texto.
    if (periodo.desde > periodo.hasta) {
      setAviso(AVISO_PERIODO)
      return null
    }
    setAviso(null)
    setCargando(true)
    return pedir(periodo)
  }

  return { periodo, historial, aviso, error, cargando, cambiarPeriodo, consultar }
}
