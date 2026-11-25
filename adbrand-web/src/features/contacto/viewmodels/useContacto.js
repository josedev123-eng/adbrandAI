// ViewModel: formulario de Contacto. Valida antes de enviar y muestra los errores que devuelve Spring.
import { useState } from 'react'
import { enviarMensajeContacto, MAXIMO_MENSAJE } from '../models/contactoApi'

const CORREO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export function validarContacto({ nombre, correo, asunto, mensaje }) {
  const errores = {}
  if (!nombre.trim()) errores.nombre = 'Escribe tu nombre.'
  if (!correo.trim()) errores.correo = 'Escribe tu correo para poder responderte.'
  else if (!CORREO.test(correo.trim())) errores.correo = 'Escribe un correo válido.'
  if (!asunto) errores.asunto = 'Elige el motivo.'
  if (mensaje.trim().length < 10) errores.mensaje = 'Cuéntanos un poco más (mínimo 10 caracteres).'
  else if (mensaje.length > MAXIMO_MENSAJE) errores.mensaje = `Máximo ${MAXIMO_MENSAJE} caracteres.`
  return errores
}

// "inicial": nombre y correo de la sesión, si hay, para no escribirlos de nuevo.
export function useContacto(inicial = {}) {
  const [datos, setDatos] = useState({
    nombre: inicial.nombre ?? '',
    correo: inicial.correo ?? '',
    asunto: '',
    mensaje: '',
  })
  const [errores, setErrores] = useState({})
  const [enviando, setEnviando] = useState(false)
  const [enviado, setEnviado] = useState(false)
  const [error, setError] = useState(null)

  function cambiar(campo, valor) {
    setDatos((actuales) => ({ ...actuales, [campo]: valor }))
    setErrores((actuales) => ({ ...actuales, [campo]: undefined }))
  }

  async function enviar() {
    const locales = validarContacto(datos)
    setErrores(locales)
    if (Object.keys(locales).length > 0) return
    setError(null)
    setEnviando(true)
    try {
      await enviarMensajeContacto({ ...datos, nombre: datos.nombre.trim(), correo: datos.correo.trim() })
      setEnviado(true)
    } catch (e) {
      if (e?.campos && Object.keys(e.campos).length > 0) setErrores(e.campos)
      setError(e?.message || 'No se pudo enviar tu mensaje. Intenta de nuevo.')
    } finally {
      setEnviando(false)
    }
  }

  function escribirOtro() {
    setDatos((actuales) => ({ ...actuales, asunto: '', mensaje: '' }))
    setEnviado(false)
  }

  return { datos, errores, enviando, enviado, error, cambiar, enviar, escribirOtro }
}
