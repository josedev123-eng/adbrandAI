// ViewModel: lista administradores y gestiona desactivación (HU-01, HU-03).
import { useEffect, useState } from 'react'
import { listarRoles, crearAdministrador, listarAdministradores, desactivarAdministrador } from '../models/administradoresApi'

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

export function useAdministradores() {
  const [formulario, setFormulario] = useState(FORMULARIO_VACIO)
  const [roles, setRoles] = useState([])
  const [administradores, setAdministradores] = useState([])
  const [errores, setErrores] = useState({})
  const [mensaje, setMensaje] = useState(null)
  const [enviando, setEnviando] = useState(false)
  const [desactivandoId, setDesactivandoId] = useState(null)
  const [mostrarConfirmacion, setMostrarConfirmacion] = useState(false)
  const [adminADesactivar, setAdminADesactivar] = useState(null)

  // Cargar roles y administradores al montar
  useEffect(() => {
    Promise.all([listarRoles(), listarAdministradores()])
      .then(([rolesData, adminsData]) => {
        setRoles(rolesData)
        setAdministradores(adminsData)
      })
      .catch(() => setMensaje({ tipo: 'error', texto: 'No se pudieron cargar los datos.' }))
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
      // Recargar lista
      const adminsActualizados = await listarAdministradores()
      setAdministradores(adminsActualizados)
    } catch (error) {
      if (error.status === 400 && error.detalles) {
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

  // HU-03: Desactivar administrador
  function abrirConfirmacionDesactivar(admin) {
    setAdminADesactivar(admin)
    setMostrarConfirmacion(true)
  }

  function cerrarConfirmacionDesactivar() {
    setMostrarConfirmacion(false)
    setAdminADesactivar(null)
  }

  async function confirmarDesactivar() {
    if (!adminADesactivar) return

    setDesactivandoId(adminADesactivar.id)
    setMensaje(null)

    try {
      await desactivarAdministrador(adminADesactivar.id)
      setMensaje({ tipo: 'exito', texto: `Cuenta de ${adminADesactivar.correo} desactivada correctamente.` })
      // Recargar lista
      const adminsActualizados = await listarAdministradores()
      setAdministradores(adminsActualizados)
    } catch (error) {
      if (error.status === 400 && error.detalles) {
        setMensaje({ tipo: 'error', texto: error.detalles.detail || 'No se pudo desactivar la cuenta.' })
      } else if (error.status === 403) {
        setMensaje({ tipo: 'error', texto: 'No tienes permisos o la cuenta ya está desactivada.' })
      } else {
        setMensaje({ tipo: 'error', texto: 'No se pudo desactivar la cuenta.' })
      }
    } finally {
      setDesactivandoId(null)
      cerrarConfirmacionDesactivar()
    }
  }

  return {
    formulario,
    roles,
    administradores,
    errores,
    mensaje,
    enviando,
    desactivandoId,
    mostrarConfirmacion,
    adminADesactivar,
    cambiarCampo,
    enviar,
    abrirConfirmacionDesactivar,
    cerrarConfirmacionDesactivar,
    confirmarDesactivar,
  }
}