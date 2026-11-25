// ViewModel: "Tu uso este mes" (consumo de IA, anuncios creados y retoques). null mientras carga o si falla:
// en ese caso la tarjeta simplemente no se muestra y la pantalla de planes sigue funcionando.
import { useEffect, useState } from 'react'
import { obtenerResumenUso } from '../models/pagosApi'

export function useUsoDelMes() {
  const [uso, setUso] = useState(null)

  useEffect(() => {
    let vigente = true
    obtenerResumenUso()
      .then((datos) => {
        if (vigente && datos?.consumo) setUso(datos)
      })
      .catch(() => {})
    return () => {
      vigente = false
    }
  }, [])

  return uso
}
