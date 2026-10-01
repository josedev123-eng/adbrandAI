// ViewModel: estado del formulario, validaciones y envío (HU-01).
import { useEffect, useState } from 'react'
import { crearAdministrador, listarRoles } from '../models/administradoresApi'

const FORMULARIO_VACIO = { nombres: '', apellidos: '', correo: '', contrasena: '', rol_id: '' }
const CORREO_VALIDO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function validar(formulario) {
  const errores = {}
  if (!formulario.nombres.trim()) errores.nombres = 'Ingresa los nombres.'
  if (!formulario.apellidos.trim()) errores.apellidos = 'Ingresa los apellidos.'
  if (!CORREO_VALIDO.test(formulario.correo)) errores.correo = 'Ingresa un correo válido.'
  if (formulario.contrasena.length < 8) errores.contrasena = 'Mínimo 8 caracteres.'
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
      const creado = await crearAdministrador({ ...formulario, rol_id: Number(formulario.rol_id) })
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