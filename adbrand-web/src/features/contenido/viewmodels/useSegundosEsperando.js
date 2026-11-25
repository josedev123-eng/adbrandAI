// Cuenta los segundos de una espera (por ejemplo, mientras la IA genera) desde "desde" (milisegundos).
// Como "desde" es la hora en que se pidió, si el dueño sale de la pantalla y vuelve, el contador sigue
// desde donde iba en vez de volver a 0.
import { useEffect, useState } from 'react'

export function useSegundosEsperando(activo, desde) {
  const [ahora, setAhora] = useState(() => Date.now())

  useEffect(() => {
    if (!activo) return undefined
    const tic = () => setAhora(Date.now())
    const primero = setTimeout(tic, 0)
    const reloj = setInterval(tic, 1000)
    return () => {
      clearTimeout(primero)
      clearInterval(reloj)
    }
  }, [activo])

  if (!activo || desde == null) return 0
  return Math.max(0, Math.floor((ahora - desde) / 1000))
}
