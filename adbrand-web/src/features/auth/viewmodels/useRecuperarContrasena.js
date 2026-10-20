// ViewModel: recuperar la contraseña en dos pasos (HU-24).
// Paso 1: pedir el código con el correo. Paso 2: escribir el código y la contraseña nueva.
import { useState } from 'react'
import { cambiarContrasena, solicitarCodigo } from '../models/authApi'
import { esCorreoValido, problemaDeContrasena } from './validaciones'

const CODIGO_VALIDO = /^\d{6}$/

// "alTerminar" recibe el mensaje para mostrar en el inicio de sesión.
export function useRecuperarContrasena(alTerminar) {
  const [paso, setPaso] = useState(1)
  const [correo, setCorreo] = useState('')
  const [formulario, setFormulario] = useState({ codigo: '', contrasenaNueva: '', confirmarContrasena: '' })
  const [errores, setErrores] = useState({})
  const [mensaje, setMensaje] = useState(null)
  const [enviando, setEnviando] = useState(false)

  function cambiarCorreo(valor) {
    setCorreo(valor)
    setErrores((actuales) => ({ ...actuales, correo: undefined }))
  }

  function cambiarCampo(campo, valor) {
    setFormulario((actual) => ({ ...actual, [campo]: valor }))
    setErrores((actuales) => ({ ...actuales, [campo]: undefined }))
  }

  async function pedirCodigo() {
    setMensaje(null)
    if (!correo.trim()) return setErrores({ correo: 'Ingresa tu correo.' })
    if (!esCorreoValido(correo)) return setErrores({ correo: 'Ingresa un correo válido.' })

    setEnviando(true)
    try {
      const respuesta = await solicitarCodigo(correo.trim())
      setErrores({})
      setPaso(2)
      setMensaje({ tipo: 'exito', texto: respuesta.mensaje })
    } catch (error) {
      if (error.status === 400) setErrores(error.campos)
      else setMensaje({ tipo: 'error', texto: 'No se pudo conectar con el servidor.' })
    } finally {
      setEnviando(false)
    }
  }

  function validarPaso2() {
    const nuevos = {}
    if (!CODIGO_VALIDO.test(formulario.codigo.trim())) nuevos.codigo = 'El código tiene 6 dígitos.'
    const problema = problemaDeContrasena(formulario.contrasenaNueva)
    if (problema) nuevos.contrasenaNueva = problema
    if (!formulario.confirmarContrasena) nuevos.confirmarContrasena = 'Repite la contraseña nueva.'
    else if (formulario.confirmarContrasena !== formulario.contrasenaNueva) {
      nuevos.confirmarContrasena = 'Las contraseñas no coinciden.'
    }
    return nuevos
  }

  async function cambiar() {
    setMensaje(null)
    const erroresLocales = validarPaso2()
    setErrores(erroresLocales)
    if (Object.keys(erroresLocales).length > 0) return

    setEnviando(true)
    try {
      const respuesta = await cambiarContrasena({
        correo: correo.trim(),
        codigo: formulario.codigo.trim(),
        contrasenaNueva: formulario.contrasenaNueva,
      })
      alTerminar?.({ tipo: 'exito', texto: respuesta.mensaje })
    } catch (error) {
      if (error.codigo === 'CODIGO_INVALIDO') {
        setErrores({ codigo: error.message })
      } else if (error.status === 400) {
        setErrores(error.campos)
      } else {
        setMensaje({ tipo: 'error', texto: 'No se pudo conectar con el servidor.' })
      }
    } finally {
      setEnviando(false)
    }
  }

  // Para pedir otro código, por ejemplo si el primero venció.
  function volverAlPaso1() {
    setPaso(1)
    setFormulario({ codigo: '', contrasenaNueva: '', confirmarContrasena: '' })
    setErrores({})
    setMensaje(null)
  }

  return {
    paso,
    correo,
    formulario,
    errores,
    mensaje,
    enviando,
    cambiarCorreo,
    cambiarCampo,
    pedirCodigo,
    cambiar,
    volverAlPaso1,
  }
}
