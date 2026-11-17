// ViewModel: Mi cuenta del administrador. Cambiar la propia contraseña escribiendo la actual.
import { useState } from 'react'
import { useSesion } from '../../../app/context/useSesion'
import { cambiarMiContrasena } from '../../auth/models/authApi'
import { erroresDeDjango, problemaDeContrasena } from '../../auth/viewmodels/reglasContrasena'

const VACIO = { actual: '', nueva: '', confirmar: '' }

export function useMiCuentaAdmin() {
  const { admin, actualizarAdmin } = useSesion()
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
    else if (datos.nueva === datos.actual) nuevos.nueva = 'La nueva contraseña debe ser distinta de la actual.'
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
      // Si era la contraseña temporal, la cuenta queda libre y se abre el panel.
      if (admin?.debe_cambiar_contrasena) actualizarAdmin?.({ debe_cambiar_contrasena: false })
      setMensaje({ tipo: 'exito', texto: respuesta?.detail ?? 'Tu contraseña se cambió.' })
    } catch (error) {
      if (error.status === 400) setErrores(erroresDeDjango(error.detalles))
      else setMensaje({ tipo: 'error', texto: error.message })
    } finally {
      setGuardando(false)
    }
  }

  return { datos, errores, mensaje, guardando, cambiar, guardar }
}
