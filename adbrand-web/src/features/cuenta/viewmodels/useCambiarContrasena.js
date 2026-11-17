// ViewModel: Mi cuenta → cambiar la contraseña escribiendo la actual.
import { useState } from 'react'
import { problemaDeContrasena } from '../../auth/viewmodels/validaciones'
import { cambiarMiContrasena } from '../models/cuentaApi'

const VACIO = { actual: '', nueva: '', confirmar: '' }

export function useCambiarContrasena() {
  const [datos, setDatos] = useState(VACIO)
  const [errores, setErrores] = useState({})
  const [mensaje, setMensaje] = useState(null)
  const [guardando, setGuardando] = useState(false)

  function cambiar(campo, valor) {
    setDatos((actuales) => ({ ...actuales, [campo]: valor }))
    setErrores((actuales) => ({ ...actuales, [campo]: undefined }))
  }

  function validar() {
    const nuevos = {}
    if (!datos.actual) nuevos.actual = 'Escribe tu contraseña actual.'
    const problema = problemaDeContrasena(datos.nueva)
    if (problema) nuevos.nueva = problema
    else if (datos.nueva === datos.actual) nuevos.nueva = 'La contraseña nueva debe ser distinta de la actual.'
    if (!nuevos.nueva && datos.confirmar !== datos.nueva) nuevos.confirmar = 'Las contraseñas no coinciden.'
    setErrores(nuevos)
    return Object.keys(nuevos).length === 0
  }

  async function guardar(evento) {
    evento?.preventDefault()
    setMensaje(null)
    if (!validar()) return

    setGuardando(true)
    try {
      const respuesta = await cambiarMiContrasena(datos.actual, datos.nueva)
      setDatos(VACIO)
      setMensaje({ tipo: 'exito', texto: respuesta?.mensaje ?? 'Tu contraseña se cambió.' })
    } catch (error) {
      if (error.codigo === 'CONTRASENA_ACTUAL_INCORRECTA') setErrores({ actual: error.message })
      else if (error.codigo === 'CONTRASENA_IGUAL') setErrores({ nueva: error.message })
      else if (error.status === 400 && error.campos) setErrores(error.campos)
      else setMensaje({ tipo: 'error', texto: error?.message || 'No se pudo cambiar la contraseña.' })
    } finally {
      setGuardando(false)
    }
  }

  return { datos, errores, mensaje, guardando, cambiar, guardar }
}
