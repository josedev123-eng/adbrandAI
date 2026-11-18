// Cuántos contenidos esperan revisión, para el contador del menú.
// Se vuelve a pedir al cambiar de pantalla y cada 30 segundos; si Django falla, el contador no se muestra.
import { useEffect, useState } from 'react'
import { listarDudosos } from '../../features/moderacion/models/moderacionApi'

const CADA_MS = 30000

export function usePendientesRevision(activo, pantallaActual) {
  const [pendientes, setPendientes] = useState(null)

  useEffect(() => {
    if (!activo) return undefined
    let vigente = true
    function consultar() {
      listarDudosos()
        .then((datos) => vigente && setPendientes(Array.isArray(datos) ? datos.length : null))
        .catch(() => vigente && setPendientes(null))
    }
    consultar()
    const intervalo = setInterval(consultar, CADA_MS)
    return () => {
      vigente = false
      clearInterval(intervalo)
    }
  }, [activo, pantallaActual])

  return activo ? pendientes : null
}
