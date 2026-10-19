// ViewModel: estado del backend, la base de datos y la IA (HU-18).
// Se vuelve a verificar solo cada 15 segundos para que el panel esté "en tiempo real".
import { useCallback, useEffect, useRef, useState } from 'react'
import { verificarEstado } from '../models/servidorApi'

export const CADA_CUANTO_MS = 15_000
const ERROR_CARGA = 'No se pudo consultar el estado. Revisa que el servidor de administración esté encendido.'

export function useEstadoServidor() {
  const [estado, setEstado] = useState(null)
  const [error, setError] = useState(null)
  const [verificando, setVerificando] = useState(false)
  // Evita guardar una respuesta que llega después de salir de la pantalla.
  const vigente = useRef(true)

  const pedir = useCallback(
    () =>
      verificarEstado()
        .then((datos) => {
          if (!vigente.current) return
          setEstado(datos)
          setError(null)
        })
        .catch(() => {
          if (vigente.current) setError(ERROR_CARGA)
        })
        .finally(() => {
          if (vigente.current) setVerificando(false)
        }),
    [],
  )

  useEffect(() => {
    vigente.current = true
    pedir()
    const intervalo = setInterval(pedir, CADA_CUANTO_MS)
    // Al salir de la pantalla se deja de verificar.
    return () => {
      vigente.current = false
      clearInterval(intervalo)
    }
  }, [pedir])

  function verificarAhora() {
    setVerificando(true)
    return pedir()
  }

  return { estado, cargando: estado === null && !error, error, verificando, verificarAhora }
}
