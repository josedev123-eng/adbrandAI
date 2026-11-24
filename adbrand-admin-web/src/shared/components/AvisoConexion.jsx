// Franja que avisa cuando se cae el internet y, al volver, confirma unos segundos que ya hay conexión.
import { useEffect, useRef, useState } from 'react'
import { useConexion } from '../hooks/useConexion'
import './Avisos.css'

export const SEGUNDOS_AVISO_RECONECTADO = 4

export default function AvisoConexion() {
  const enLinea = useConexion()
  const [reconectado, setReconectado] = useState(false)
  const estuvoSinConexion = useRef(!enLinea)

  useEffect(() => {
    if (!enLinea) {
      estuvoSinConexion.current = true
      return undefined
    }
    if (!estuvoSinConexion.current) return undefined
    estuvoSinConexion.current = false
    // Se muestra un momento "Conexión restablecida" y luego desaparece solo.
    const mostrar = setTimeout(() => setReconectado(true), 0)
    const ocultar = setTimeout(() => setReconectado(false), SEGUNDOS_AVISO_RECONECTADO * 1000)
    return () => {
      clearTimeout(mostrar)
      clearTimeout(ocultar)
    }
  }, [enLinea])

  if (!enLinea) {
    return (
      <div className="aviso-conexion aviso-conexion-caida" role="alert">
        <strong>Sin conexión a internet.</strong> Revisa tu wifi o tus datos. Lo que hagas no se guardará hasta que vuelva.
      </div>
    )
  }
  if (reconectado) {
    return (
      <div className="aviso-conexion aviso-conexion-ok" role="status">
        Conexión restablecida.
      </div>
    )
  }
  return null
}
