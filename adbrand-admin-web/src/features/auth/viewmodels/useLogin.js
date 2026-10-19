// ViewModel: formulario de inicio de sesión de administradores.
import { useState } from 'react'
import { useSesion } from '../../../app/context/useSesion'
import { iniciarSesion } from '../models/authApi'

const CORREO_VALIDO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export function useLogin() {
  const { entrar } = useSesion()
  const [correo, setCorreo] = useState('')
  const [contrasena, setContrasena] = useState('')
  const [errores, setErrores] = useState({})
  const [error, setError] = useState(null)
  const [enviando, setEnviando] = useState(false)

  function cambiarCorreo(valor) {
    setCorreo(valor)
    setErrores((actuales) => ({ ...actuales, correo: undefined }))
  }

  function cambiarContrasena(valor) {
    setContrasena(valor)
    setErrores((actuales) => ({ ...actuales, contrasena: undefined }))
  }

  function validar() {
    const nuevos = {}
    if (!correo.trim()) nuevos.correo = 'Escribe tu correo.'
    else if (!CORREO_VALIDO.test(correo.trim())) nuevos.correo = 'Escribe un correo válido.'
    if (!contrasena) nuevos.contrasena = 'Escribe tu contraseña.'
    setErrores(nuevos)
    return Object.keys(nuevos).length === 0
  }

  async function enviar(evento) {
    evento?.preventDefault()
    setError(null)
    if (!validar()) return

    setEnviando(true)
    try {
      const { token, admin } = await iniciarSesion(correo.trim(), contrasena)
      entrar(token, admin)
    } catch (e) {
      if (e.status === 401 || e.status === 403) {
        // 401: correo o contraseña incorrectos. 403: cuenta desactivada (HU 03).
        setError(e.detalles?.detail || 'No se pudo iniciar sesión.')
      } else if (e.status === 400) {
        setError('Revisa el correo y la contraseña.')
      } else {
        setError('No se pudo conectar con el servidor. Revisa que Django esté encendido.')
      }
      setContrasena('')
    } finally {
      setEnviando(false)
    }
  }

  return { correo, contrasena, errores, error, enviando, cambiarCorreo, cambiarContrasena, enviar }
}
