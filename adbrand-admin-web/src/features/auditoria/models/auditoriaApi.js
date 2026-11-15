// Model: llamadas a la API de auditoría (HU-04).
import { apiRequest } from '../../../shared/services/apiClient'

// Los mismos valores que guarda Django en auditoria_admin.accion.
export const NOMBRES_ACCION = {
  CREAR_ADMIN: 'Creó una cuenta',
  DESACTIVAR_ADMIN: 'Desactivó una cuenta',
  CAMBIAR_ROL: 'Cambió un rol',
  APROBAR_CONTENIDO: 'Aprobó un contenido',
  RECHAZAR_CONTENIDO: 'Rechazó un contenido',
  SUSPENDER_EMPRESA: 'Suspendió una empresa',
  REACTIVAR_EMPRESA: 'Reactivó una empresa',
  CAMBIAR_CONTRASENA: 'Cambió su contraseña',
  SOLICITAR_CODIGO: 'Pidió un código de recuperación',
  RECUPERAR_CONTRASENA: 'Recuperó su contraseña',
}

export const NOMBRES_ENTIDAD = {
  usuario_admin: 'Administrador',
  contenido: 'Contenido',
  usuario: 'Empresa',
}

// Los filtros vacíos no se envían: Django responde 400 si llega una fecha vacía.
export function listarAuditoria({ adminId, desde, hasta } = {}) {
  const parametros = new URLSearchParams()
  if (adminId) parametros.set('admin_id', adminId)
  if (desde) parametros.set('desde', desde)
  if (hasta) parametros.set('hasta', hasta)

  const consulta = parametros.toString()
  return apiRequest(consulta ? `/auditoria/?${consulta}` : '/auditoria/')
}

const FORMATO_FECHA = new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium', timeStyle: 'short' })

export function formatearFecha(texto) {
  return FORMATO_FECHA.format(new Date(texto))
}
