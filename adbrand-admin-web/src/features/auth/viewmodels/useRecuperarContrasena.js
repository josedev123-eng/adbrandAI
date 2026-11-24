// ViewModel: "¿Olvidaste tu contraseña?" del panel. Paso 1: correo → código. Paso 2: código + contraseña nueva.
import { useState } from 'react'
import { cambiarConCodigo, solicitarCodigo } from '../models/authApi'
import { erroresDeDjango, problemaDeContrasena } from './reglasContrasena'

const CORREO_VALIDO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export function useRecuperarContrasena(alTerminar) {
  const [paso, setPaso] = useState('correo')
  const [datos, setDatos] = useState({ correo: '', codigo: '', nueva: '', confirmar: '' })
  const [errores, setErrores] = useState({})
  const [mensaje, setMensaje] = useState(null)
  const [enviando, setEnviando] = useState(false)

  function cambiar(campo, valor) {
    setDatos((actuales) => ({ ...actuales, [campo]: campo === 'codigo' ? valor.replace(/\D/g, '').slice(0, 6) : valor }))
    setErrores((actuales) => ({ ...actuales, [campo]: undefined }))
  }

  async function pedirCodigo(evento) {
    evento?.preventDefault()
    setMensaje(null)
    const correo = datos.correo.trim()
    if (!CORREO_VALIDO.test(correo)) {
      setErrores({ correo: correo ? 'Escribe un correo válido.' : 'Escribe tu correo.' })
      return
    }
    setEnviando(true)
    try {
      const respuesta = await solicitarCodigo(correo)
      setPaso('codigo')
      setMensaje({ tipo: 'info', texto: respuesta?.detail ?? 'Te enviamos un código a tu correo.' })
    } catch (error) {
      setMensaje({ tipo: 'error', texto: error.message })
    } finally {
      setEnviando(false)
    }
  }

  async function cambiarContrasena(evento) {
    evento?.preventDefault()
    setMensaje(null)
    const nuevos = {}
    if (datos.codigo.length !== 6) nuevos.codigo = 'El código tiene 6 números.'
    const problema = problemaDeContrasena(datos.nueva)
    if (problema) nuevos.nueva = problema
    if (!nuevos.nueva && datos.confirmar !== datos.nueva) nuevos.confirmar = 'Las contraseñas no coinciden.'
    setErrores(nuevos)
    if (Object.keys(nuevos).length > 0) return

    setEnviando(true)
    try {
      const respuesta = await cambiarConCodigo(datos.correo.trim(), datos.codigo, datos.nueva)
      alTerminar?.(respuesta?.detail ?? 'Listo, tu contraseña se cambió. Ya puedes iniciar sesión.')
    } catch (error) {
      if (error.status === 400) setErrores(erroresDeDjango(error.detalles))
      else setMensaje({ tipo: 'error', texto: error.message })
    } finally {
      setEnviando(false)
    }
  }

  function volverAPedir() {
    setPaso('correo')
    setDatos((actuales) => ({ ...actuales, codigo: '', nueva: '', confirmar: '' }))
    setErrores({})
    setMensaje(null)
  }

  return { paso, datos, errores, mensaje, enviando, cambiar, pedirCodigo, cambiarContrasena, volverAPedir }
}
