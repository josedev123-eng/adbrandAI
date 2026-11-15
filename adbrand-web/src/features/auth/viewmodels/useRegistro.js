// ViewModel: formulario de registro de la empresa, sus validaciones y el envío (HU-21).
import { useState } from 'react'
import { registrar } from '../models/authApi'
import { esCorreoValido, normalizarCorreo, problemaDeContrasena, problemaDeEmpresa, problemaDeNombre } from './validaciones'

const FORMULARIO_VACIO = { nombre: '', nombreEmpresa: '', correo: '', contrasena: '', confirmarContrasena: '', aceptaTerminos: false }

function validar(formulario) {
  const errores = {}
  const problemaNombre = problemaDeNombre(formulario.nombre)
  if (problemaNombre) errores.nombre = problemaNombre
  const problemaEmpresa = problemaDeEmpresa(formulario.nombreEmpresa)
  if (problemaEmpresa) errores.nombreEmpresa = problemaEmpresa
  if (!formulario.correo.trim()) errores.correo = 'Ingresa tu correo.'
  else if (!esCorreoValido(formulario.correo)) errores.correo = 'Ingresa un correo válido.'

  const problema = problemaDeContrasena(formulario.contrasena)
  if (problema) errores.contrasena = problema
  if (!formulario.confirmarContrasena) errores.confirmarContrasena = 'Repite la contraseña.'
  else if (formulario.confirmarContrasena !== formulario.contrasena) {
    errores.confirmarContrasena = 'Las contraseñas no coinciden.'
  }
  // Ley 29733: el consentimiento para tratar los datos debe ser expreso, por eso la casilla es obligatoria.
  if (!formulario.aceptaTerminos) errores.aceptaTerminos = 'Para crear la cuenta debes aceptar los Términos y la Política de privacidad.'
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
        correo: normalizarCorreo(formulario.correo),
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
        setMensaje({ tipo: 'error', texto: error?.message || 'No se pudo conectar con el servidor.' })
      }
    } finally {
      setEnviando(false)
    }
  }

  return { formulario, errores, mensaje, enviando, creada, cambiarCampo, enviar }
}
