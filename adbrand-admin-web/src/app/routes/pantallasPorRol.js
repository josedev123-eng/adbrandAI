// Qué pantallas ve cada rol en el menú (HU 02, criterio 2: módulos según el rol).
// Django también revisa el rol en cada endpoint (paso 3), esto solo ordena el menú.
export const PANTALLAS = [
  { id: 'revision', nombre: 'Revisión de contenido', roles: ['SUPERADMIN', 'MODERADOR'] },
  { id: 'administradores', nombre: 'Administradores', roles: ['SUPERADMIN'] },
  { id: 'lista-administradores', nombre: 'Lista de administradores', roles: ['SUPERADMIN'] },
  { id: 'auditoria', nombre: 'Auditoría', roles: ['SUPERADMIN'] },
  { id: 'panel-financiero', nombre: 'Panel financiero', roles: ['SUPERADMIN', 'FINANZAS'] },
]

export const NOMBRES_ROL = {
  SUPERADMIN: 'Superadministrador',
  MODERADOR: 'Moderador',
  FINANZAS: 'Finanzas',
  TECNICO: 'Técnico',
}

export function pantallasDelRol(rol) {
  return PANTALLAS.filter((pantalla) => pantalla.roles.includes(rol))
}
