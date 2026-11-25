// ViewModel: editar el logo con instrucciones cortas, igual que los retoques de anuncios.
// La IA devuelve una versión nueva; solo se guarda si la empresa dice "Usar esta versión".
import { useState } from 'react'
import { retocarLogo } from '../models/brandkitApi'

export const MAXIMO_INSTRUCCION = 300

export function useRetocarLogo(kitId) {
  const [instruccion, setInstruccion] = useState('')
  const [retocando, setRetocando] = useState(false)
  const [version, setVersion] = useState(null)
  const [restantes, setRestantes] = useState(null)
  const [error, setError] = useState(null)

  function cambiarInstruccion(valor) {
    setInstruccion(valor)
    setError(null)
  }

  async function retocar() {
    const texto = instruccion.trim()
    if (!texto) {
      setError('Escribe qué quieres cambiar del logo.')
      return
    }
    setError(null)
    setRetocando(true)
    try {
      const respuesta = await retocarLogo(kitId, texto)
      setVersion(respuesta.imagen)
      setRestantes(respuesta.retoquesRestantes ?? null)
    } catch (e) {
      setError(e?.message || 'No se pudo editar el logo. Inténtalo de nuevo.')
    } finally {
      setRetocando(false)
    }
  }

  // Después de usarla o descartarla, se vuelve a empezar.
  function limpiar() {
    setVersion(null)
    setInstruccion('')
  }

  return { instruccion, cambiarInstruccion, retocando, version, restantes, error, retocar, limpiar }
}
