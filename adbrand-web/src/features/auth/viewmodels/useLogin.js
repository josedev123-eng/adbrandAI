// ViewModel: formulario de inicio de sesión, sus validaciones y el envío (HU-22).
import { useState } from 'react'
import { useSesion } from '../../../app/context/useSesion'
import { iniciarSesion } from '../models/authApi'
import { esCorreoValido } from './validaciones'

const FORMULARIO_VACIO = { correo: '', contrasena: '' }

function validar(formulario) {
  const errores = {}
  if (!formulario.correo.trim()) errores.correo = 'Ingresa tu correo.'
  else if (!esCorreoValido(formulario.correo)) errores.correo = 'Ingresa un correo válido.'
  if (!formulario.contrasena) errores.contrasena = 'Ingresa tu contraseña.'
  return errores
}

// "aviso" es un mensaje que llega de otra pantalla, por ejemplo "Cuenta creada, ahora inicia sesión".
export function useLogin(aviso = null) {
  const { entrar } = useSesion()
  const [formulario, setFormulario] = useState(FORMULARIO_VACIO)
  const [errores, setErrores] = useState({})
  const [mensaje, setMensaje] = useState(aviso)
  const [enviando, setEnviando] = useState(false)

  function cambiarCampo(campo, valor) {
    setFormulario((actual) => ({ ...actual, [campo]: valor }))
    setErrores((actuales) => ({ ...actuales, [campo]: undefined }))
  }

  async function enviar() {
    setMensaje(null)
    const erroresLocales = validar(formulario)
    setErrores(erroresLocales)
    if (Object.keys(erroresLocales).length > 0) return

    setEnviando(true)
    try {
      const { token, usuario } = await iniciarSesion(formulario.correo.trim(), formulario.contrasena)
      entrar(token, usuario)
    } catch (error) {
      // Por seguridad, la contraseña escrita se borra si no se pudo entrar.
      setFormulario((actual) => ({ ...actual, contrasena: '' }))
      if (error.status === 401 || error.status === 403) {
        setMensaje({ tipo: 'error', texto: error.message })
      } else if (error.status === 400) {
        setErrores(error.campos)
      } else {
        setMensaje({ tipo: 'error', texto: 'No se pudo conectar con el servidor.' })
      }
    } finally {
      setEnviando(false)
    }
  }

  return { formulario, errores, mensaje, enviando, cambiarCampo, enviar }
}
