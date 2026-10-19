// ViewModel: lista administradores y gestiona desactivación (HU-03).
// Crear cuentas (HU-01) está en useCrearAdministrador.
import { useEffect, useState } from 'react'
import { listarRoles, listarAdministradores, desactivarAdministrador } from '../models/administradoresApi'

export function useAdministradores() {
  const [roles, setRoles] = useState([])
  const [administradores, setAdministradores] = useState([])
  const [mensaje, setMensaje] = useState(null)
  const [desactivandoId, setDesactivandoId] = useState(null)
  const [mostrarConfirmacion, setMostrarConfirmacion] = useState(false)
  const [adminADesactivar, setAdminADesactivar] = useState(null)

  // Cargar roles (para editar permisos, HU-02) y administradores al montar
  useEffect(() => {
    Promise.all([listarRoles(), listarAdministradores()])
      .then(([rolesData, adminsData]) => {
        setRoles(rolesData)
        setAdministradores(adminsData)
      })
      .catch(() => setMensaje({ tipo: 'error', texto: 'No se pudieron cargar los datos.' }))
  }, [])

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

  // HU-02: vuelve a pedir la lista después de cambiar el rol de un administrador.
  async function recargarAdministradores() {
    const adminsActualizados = await listarAdministradores()
    setAdministradores(adminsActualizados)
  }

  return {
    roles,
    administradores,
    mensaje,
    desactivandoId,
    mostrarConfirmacion,
    adminADesactivar,
    abrirConfirmacionDesactivar,
    cerrarConfirmacionDesactivar,
    confirmarDesactivar,
    recargarAdministradores,
  }
}