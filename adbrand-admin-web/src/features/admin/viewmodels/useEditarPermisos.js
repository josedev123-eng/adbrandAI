// ViewModel: edita los permisos (el rol) de un administrador existente (HU-02).
import { useState } from 'react'
import { cambiarRolAdministrador } from '../models/administradoresApi'

// adminActualId: id del administrador conectado (viene de la sesión).
export function useEditarPermisos(alGuardar, adminActualId) {
  const [adminAEditar, setAdminAEditar] = useState(null)
  const [rolId, setRolId] = useState('')
  const [errorPermisos, setErrorPermisos] = useState(null)
  const [mensajePermisos, setMensajePermisos] = useState(null)
  const [guardando, setGuardando] = useState(false)

  // Nadie modifica sus propios permisos: el servidor también lo rechaza con 403.
  function puedeEditar(admin) {
    return admin.id !== adminActualId
  }

  function abrirEdicion(admin) {
    setAdminAEditar(admin)
    setRolId(admin.rol ? String(admin.rol.id) : '')
    setErrorPermisos(null)
    setMensajePermisos(null)
  }

  function cerrarEdicion() {
    setAdminAEditar(null)
    setRolId('')
    setErrorPermisos(null)
  }

  function cambiarRol(valor) {
    setRolId(valor)
    setErrorPermisos(null)
  }

  async function guardarPermisos() {
    if (!adminAEditar) return

    // Validaciones antes de llamar a la API.
    if (!rolId) {
      setErrorPermisos('Selecciona un rol.')
      return
    }
    if (Number(rolId) === adminAEditar.rol?.id) {
      setErrorPermisos('Selecciona un rol distinto al actual.')
      return
    }

    setGuardando(true)
    setErrorPermisos(null)
    try {
      const actualizado = await cambiarRolAdministrador(adminAEditar.id, Number(rolId))
      setMensajePermisos({
        tipo: 'exito',
        texto: `Permisos de ${actualizado.correo} actualizados: ahora tiene el rol ${actualizado.rol.nombre}.`,
      })
      cerrarEdicion()
      await alGuardar?.()
    } catch (error) {
      if (error.status === 400 && error.detalles?.rol_id) {
        setErrorPermisos(error.detalles.rol_id[0])
      } else if (error.status === 403) {
        setErrorPermisos(error.detalles?.detail || 'No tienes permisos para modificar esta cuenta.')
      } else if (error.status === 404) {
        setErrorPermisos('Este administrador ya no existe.')
      } else {
        setErrorPermisos('No se pudieron guardar los permisos.')
      }
    } finally {
      setGuardando(false)
    }
  }

  return {
    adminAEditar,
    rolId,
    errorPermisos,
    mensajePermisos,
    guardando,
    puedeEditar,
    abrirEdicion,
    cerrarEdicion,
    cambiarRol,
    guardarPermisos,
  }
}
