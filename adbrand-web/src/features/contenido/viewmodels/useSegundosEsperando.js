// Cuenta los segundos mientras "activo" es true (por ejemplo, mientras la IA genera). Vuelve a 0 al empezar de nuevo.
import { useEffect, useState } from 'react'

export function useSegundosEsperando(activo) {
  const [segundos, setSegundos] = useState(0)
  const [activoAntes, setActivoAntes] = useState(activo)

  // Al empezar o terminar una espera, el contador se reinicia (durante el render, sin efecto).
  if (activo !== activoAntes) {
    setActivoAntes(activo)
    setSegundos(0)
  }

  useEffect(() => {
    if (!activo) return undefined
    const reloj = setInterval(() => setSegundos((s) => s + 1), 1000)
    return () => clearInterval(reloj)
  }, [activo])

  return segundos
}
