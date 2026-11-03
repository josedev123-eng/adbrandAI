// ViewModel: editor solo de colores del kit de marca (HU-30). Cambia la paleta sin tocar lo demás del kit.
import { useState } from 'react'
import { guardarColoresKitMarca, leerPaleta } from '../models/brandkitApi'
import { MAXIMO_COLORES } from './useEditarKitMarca'

const COLOR_VALIDO = /^#[0-9A-Fa-f]{6}$/

// Las mismas reglas que EditarColoresRequest en Spring: de 1 a 8 colores, #RRGGBB y sin repetidos.
function validar(colores) {
  const errores = {}
  const vistos = new Set()
  colores.forEach((color, i) => {
    const hex = color.hex.trim().toUpperCase()
    if (!COLOR_VALIDO.test(hex)) errores[`colores[${i}].hex`] = 'Escribe el color como #RRGGBB.'
    else if (vistos.has(hex)) errores[`colores[${i}].hex`] = `El color ${hex} está repetido.`
    vistos.add(hex)
  })
  return errores
}

export function useEditarColores(kit, alGuardar) {
  const [colores, setColores] = useState(() => {
    const paleta = leerPaleta(kit.paletaColores)
    return paleta.length > 0 ? paleta : [{ hex: '#4F46E5', nombre: '' }]
  })
  const [errores, setErrores] = useState({})
  const [mensaje, setMensaje] = useState(null)
  const [guardando, setGuardando] = useState(false)

  function cambiarColor(indice, campo, valor) {
    setColores((actuales) => actuales.map((color, i) => (i === indice ? { ...color, [campo]: valor } : color)))
    setErrores({})
  }

  function agregarColor() {
    if (colores.length >= MAXIMO_COLORES) return
    setColores((actuales) => [...actuales, { hex: '#FFFFFF', nombre: '' }])
  }

  function quitarColor(indice) {
    if (colores.length <= 1) return
    setColores((actuales) => actuales.filter((_, i) => i !== indice))
    setErrores({})
  }

  // Solo los colores bien escritos entran en la vista previa.
  const coloresValidos = colores.map((color) => color.hex.trim()).filter((hex) => COLOR_VALIDO.test(hex))

  async function guardar() {
    setMensaje(null)
    const erroresLocales = validar(colores)
    setErrores(erroresLocales)
    if (Object.keys(erroresLocales).length > 0) return

    setGuardando(true)
    try {
      const actualizado = await guardarColoresKitMarca(
        kit.id,
        colores.map((color) => ({ hex: color.hex.trim().toUpperCase(), nombre: color.nombre.trim() })),
      )
      alGuardar?.(actualizado)
    } catch (error) {
      if (error.status === 400 && error.campos && Object.keys(error.campos).length > 0) {
        setErrores(error.campos)
      } else if (error.codigo === 'COLOR_REPETIDO') {
        setMensaje({ tipo: 'error', texto: error.message })
      } else if (error.status === 404) {
        setMensaje({ tipo: 'error', texto: 'Este kit ya no existe.' })
      } else {
        setMensaje({ tipo: 'error', texto: 'No se pudieron guardar los colores. Revisa tu conexión.' })
      }
    } finally {
      setGuardando(false)
    }
  }

  return {
    colores,
    coloresValidos,
    errores,
    mensaje,
    guardando,
    puedeAgregar: colores.length < MAXIMO_COLORES,
    cambiarColor,
    agregarColor,
    quitarColor,
    guardar,
  }
}
