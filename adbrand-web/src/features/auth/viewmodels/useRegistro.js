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

// Validación mientras se escribe: las reglas se muestran desde el inicio como ayuda; el error de un campo
// aparece cuando el usuario sale de él (o al intentar crear la cuenta) y desaparece apenas lo corrige.
// "alCrear" recibe el correo de la cuenta nueva para abrir "Confirma tu correo".
export function useRegistro(alCrear = null) {
  const [formulario, setFormulario] = useState(FORMULARIO_VACIO)
  // Errores que devolvió Spring (por ejemplo, correo repetido). Los locales se calculan en cada tecla.
  const [errores, setErrores] = useState({})
  const [tocados, setTocados] = useState({})
  const [intentado, setIntentado] = useState(false)
  const vivos = validar(formulario)
  const erroresVisibles = {}
  for (const campo of Object.keys(FORMULARIO_VACIO)) {
    // La confirmación avisa en cuanto deja de coincidir, sin esperar a salir del campo.
    const mostrarLocal = intentado || tocados[campo] || (campo === 'confirmarContrasena' && formulario.confirmarContrasena)
    erroresVisibles[campo] = errores[campo] ?? (mostrarLocal ? vivos[campo] : undefined)
  }
  // Campos que ya cumplen sus reglas (para marcarlos con ✓).
  const validos = Object.fromEntries(
    Object.keys(FORMULARIO_VACIO).map((campo) => [campo, Boolean(formulario[campo]) && !vivos[campo] && !errores[campo]]),
  )

  function tocar(campo) {
    if (formulario[campo]) setTocados((actuales) => ({ ...actuales, [campo]: true }))
  }
  const [mensaje, setMensaje] = useState(null)
  const [enviando, setEnviando] = useState(false)
  const [creada, setCreada] = useState(false)

  function cambiarCampo(campo, valor) {
    setFormulario((actual) => ({ ...actual, [campo]: valor }))
    setErrores((actuales) => ({ ...actuales, [campo]: undefined }))
  }

  async function enviar() {
    setMensaje(null)
    setIntentado(true)
    setErrores({})
    if (Object.keys(vivos).length > 0) return

    setEnviando(true)
    try {
      const correo = normalizarCorreo(formulario.correo)
      await registrar({
        nombre: formulario.nombre.trim(),
        nombreEmpresa: formulario.nombreEmpresa.trim(),
        correo,
        contrasena: formulario.contrasena,
      })
      setFormulario(FORMULARIO_VACIO)
      setTocados({})
      setIntentado(false)
      setCreada(true)
      setMensaje({ tipo: 'exito', texto: 'Cuenta creada. Te enviamos un código a tu correo para confirmarlo.' })
      alCrear?.(correo)
    } catch (error) {
      if (error.codigo === 'CORREO_REPETIDO') {
        setErrores({ correo: error.message })
      } else if (error.status === 400) {
        // Los errores de cada campo que manda Spring en "campos".
        setErrores(error.campos ?? {})
        setMensaje({ tipo: 'error', texto: error.message })
      } else {
        setMensaje({ tipo: 'error', texto: error?.message || 'No se pudo conectar con el servidor.' })
      }
    } finally {
      setEnviando(false)
    }
  }

  return { formulario, errores: erroresVisibles, validos, mensaje, enviando, creada, cambiarCampo, tocar, enviar }
}
