// ViewModel: editor de la identidad visual del kit (HU-31). Cambia tipografías y voz de marca sin tocar lo demás.
import { useState } from 'react'
import { guardarIdentidadKitMarca } from '../models/brandkitApi'

export const MAXIMO_TIPOGRAFIA = 200
export const MAXIMO_VOZ = 1000

// Las mismas reglas que EditarIdentidadRequest en Spring.
function validar(identidad) {
  const errores = {}
  if (!identidad.tipografiaPrimaria.trim()) errores.tipografiaPrimaria = 'Elige la tipografía principal.'
  else if (identidad.tipografiaPrimaria.trim().length > MAXIMO_TIPOGRAFIA) errores.tipografiaPrimaria = 'Máximo 200 caracteres.'
  if (identidad.tipografiaSecundaria.trim().length > MAXIMO_TIPOGRAFIA) errores.tipografiaSecundaria = 'Máximo 200 caracteres.'
  if (!identidad.vozMarca.trim()) errores.vozMarca = 'Describe la voz de tu marca.'
  else if (identidad.vozMarca.trim().length > MAXIMO_VOZ) errores.vozMarca = 'Máximo 1000 caracteres.'
  return errores
}

export function useEditarIdentidad(kit, alGuardar) {
  const [identidad, setIdentidad] = useState({
    tipografiaPrimaria: kit.tipografiaPrimaria ?? '',
    tipografiaSecundaria: kit.tipografiaSecundaria ?? '',
    vozMarca: kit.vozMarca ?? '',
  })
  const [errores, setErrores] = useState({})
  const [mensaje, setMensaje] = useState(null)
  const [guardando, setGuardando] = useState(false)

  function cambiarCampo(campo, valor) {
    setIdentidad((actual) => ({ ...actual, [campo]: valor }))
    setErrores((actuales) => ({ ...actuales, [campo]: undefined }))
  }

  async function guardar() {
    setMensaje(null)
    const erroresLocales = validar(identidad)
    setErrores(erroresLocales)
    if (Object.keys(erroresLocales).length > 0) return

    setGuardando(true)
    try {
      const actualizado = await guardarIdentidadKitMarca(kit.id, {
        tipografiaPrimaria: identidad.tipografiaPrimaria.trim(),
        tipografiaSecundaria: identidad.tipografiaSecundaria.trim() || null,
        vozMarca: identidad.vozMarca.trim(),
      })
      alGuardar?.(actualizado)
    } catch (error) {
      if (error.status === 400 && error.campos && Object.keys(error.campos).length > 0) {
        setErrores(error.campos)
      } else if (error.status === 404) {
        setMensaje({ tipo: 'error', texto: 'Este kit ya no existe.' })
      } else {
        setMensaje({ tipo: 'error', texto: 'No se pudo guardar la identidad. Revisa tu conexión.' })
      }
    } finally {
      setGuardando(false)
    }
  }

  return { identidad, errores, mensaje, guardando, cambiarCampo, guardar }
}
