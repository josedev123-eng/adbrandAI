// ViewModel: estado del formulario, validaciones y envío (HU-01).
import { useEffect, useState } from 'react'
import { crearAdministrador, listarRoles } from '../models/administradoresApi'

const FORMULARIO_VACIO = { nombres: '', apellidos: '', correo: '', contrasena: '', rol_id: '' }
const CORREO_VALIDO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
// Las mismas reglas que valida Django en CrearAdministradorSerializer.
const NOMBRE_VALIDO = /^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+([ '-][A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+)*$/

function validarNombre(valor, campo) {
  const texto = valor.trim()
  if (!texto) return `Ingresa los ${campo}.`
  if (texto.length < 2) return 'Debe tener al menos 2 letras.'
  if (!NOMBRE_VALIDO.test(texto)) return 'Usa solo letras y espacios, sin números ni símbolos.'
  return undefined
}

// Devuelve el primer requisito que falta, o undefined si la contraseña es fuerte.
export function validarContrasena(contrasena) {
  if (contrasena.length < 8) return 'Mínimo 8 caracteres.'
  if (/\s/.test(contrasena)) return 'La contraseña no puede tener espacios.'
  if (!/[A-Z]/.test(contrasena)) return 'Debe tener al menos una letra mayúscula.'
  if (!/[a-z]/.test(contrasena)) return 'Debe tener al menos una letra minúscula.'
  if (!/[0-9]/.test(contrasena)) return 'Debe tener al menos un número.'
  return undefined
}

export function validar(formulario) {
  const errores = {}
  const errorNombres = validarNombre(formulario.nombres, 'nombres')
  const errorApellidos = validarNombre(formulario.apellidos, 'apellidos')
  const errorContrasena = validarContrasena(formulario.contrasena)
  if (errorNombres) errores.nombres = errorNombres
  if (errorApellidos) errores.apellidos = errorApellidos
  if (!CORREO_VALIDO.test(formulario.correo)) errores.correo = 'Ingresa un correo válido.'
  if (errorContrasena) errores.contrasena = errorContrasena
  if (!formulario.rol_id) errores.rol_id = 'Selecciona un rol.'
  return errores
}

export function useCrearAdministrador() {
  const [formulario, setFormulario] = useState(FORMULARIO_VACIO)
  const [roles, setRoles] = useState([])
  const [errores, setErrores] = useState({})
  const [mensaje, setMensaje] = useState(null)
  const [enviando, setEnviando] = useState(false)

  useEffect(() => {
    listarRoles()
      .then(setRoles)
      .catch(() => setMensaje({ tipo: 'error', texto: 'No se pudieron cargar los roles.' }))
  }, [])

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
      const creado = await crearAdministrador({
        ...formulario,
        nombres: formulario.nombres.trim(),
        apellidos: formulario.apellidos.trim(),
        rol_id: Number(formulario.rol_id),
      })
      setMensaje({ tipo: 'exito', texto: `Cuenta creada para ${creado.correo} con rol ${creado.rol.nombre}.` })
      setFormulario(FORMULARIO_VACIO)
    } catch (error) {
      if (error.status === 400 && error.detalles) {
        // Errores que devuelve Django, por ejemplo "Ya existe un administrador con este correo."
        const delServidor = {}
        for (const [campo, mensajes] of Object.entries(error.detalles)) {
          delServidor[campo] = Array.isArray(mensajes) ? mensajes[0] : String(mensajes)
        }
        setErrores(delServidor)
      } else {
        setMensaje({ tipo: 'error', texto: 'No se pudo conectar con el servidor.' })
      }
    } finally {
      setEnviando(false)
    }
  }

  return { formulario, roles, errores, mensaje, enviando, cambiarCampo, enviar }
}