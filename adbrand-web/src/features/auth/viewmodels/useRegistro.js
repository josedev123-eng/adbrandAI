// ViewModel: formulario de registro de la empresa, sus validaciones y el envío (HU-21).
import { useState } from 'react'
import { registrar } from '../models/authApi'
import { esCorreoValido, problemaDeContrasena } from './validaciones'

const FORMULARIO_VACIO = { nombre: '', nombreEmpresa: '', correo: '', contrasena: '', confirmarContrasena: '' }

function validar(formulario) {
  const errores = {}
  if (!formulario.nombre.trim()) errores.nombre = 'Ingresa tu nombre.'
  if (!formulario.nombreEmpresa.trim()) errores.nombreEmpresa = 'Ingresa el nombre de tu empresa.'
  if (!formulario.correo.trim()) errores.correo = 'Ingresa tu correo.'
  else if (!esCorreoValido(formulario.correo)) errores.correo = 'Ingresa un correo válido.'

  const problema = problemaDeContrasena(formulario.contrasena)
  if (problema) errores.contrasena = problema
  if (!formulario.confirmarContrasena) errores.confirmarContrasena = 'Repite la contraseña.'
  else if (formulario.confirmarContrasena !== formulario.contrasena) {
    errores.confirmarContrasena = 'Las contraseñas no coinciden.'
  }
  return errores
}

export function useRegistro() {
  const [formulario, setFormulario] = useState(FORMULARIO_VACIO)
  const [errores, setErrores] = useState({})
  const [mensaje, setMensaje] = useState(null)
  const [enviando, setEnviando] = useState(false)
  const [creada, setCreada] = useState(false)

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
      await registrar({
        nombre: formulario.nombre.trim(),
        nombreEmpresa: formulario.nombreEmpresa.trim(),
        correo: formulario.correo.trim(),
        contrasena: formulario.contrasena,
      })
      setFormulario(FORMULARIO_VACIO)
      setCreada(true)
      setMensaje({ tipo: 'exito', texto: 'Cuenta creada, ahora inicia sesión.' })
    } catch (error) {
      if (error.codigo === 'CORREO_REPETIDO') {
        setErrores({ correo: error.message })
      } else if (error.status === 400) {
        // Los errores de cada campo que manda Spring en "campos".
        setErrores(error.campos)
        setMensaje({ tipo: 'error', texto: error.message })
      } else {
        setMensaje({ tipo: 'error', texto: 'No se pudo conectar con el servidor.' })
      }
    } finally {
      setEnviando(false)
    }
  }

  return { formulario, errores, mensaje, enviando, creada, cambiarCampo, enviar }
}
