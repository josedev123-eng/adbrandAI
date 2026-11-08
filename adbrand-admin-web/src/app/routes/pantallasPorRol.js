// Qué pantallas ve cada rol en el menú (HU 02, criterio 2: módulos según el rol).
// `seccion` agrupa las opciones en el menú lateral.
// Django también revisa el rol en cada endpoint (paso 3), esto solo ordena el menú.
export const PANTALLAS = [
  // Inicio: tarjetas resumen; cada rol ve solo las tarjetas de sus pantallas.
  { id: 'inicio', nombre: 'Inicio', roles: ['SUPERADMIN', 'MODERADOR', 'FINANZAS', 'TECNICO'] },
  { id: 'revision', nombre: 'Revisión de contenido', seccion: 'Contenido', roles: ['SUPERADMIN', 'MODERADOR'] },
  { id: 'historial-contenidos', nombre: 'Historial de contenidos', seccion: 'Contenido', roles: ['SUPERADMIN', 'MODERADOR'] },
  { id: 'administradores', nombre: 'Crear administrador', seccion: 'Administradores', roles: ['SUPERADMIN'] },
  { id: 'lista-administradores', nombre: 'Lista de administradores', seccion: 'Administradores', roles: ['SUPERADMIN'] },
  { id: 'auditoria', nombre: 'Auditoría', seccion: 'Administradores', roles: ['SUPERADMIN'] },
  { id: 'panel-financiero', nombre: 'Panel financiero', seccion: 'Finanzas', roles: ['SUPERADMIN', 'FINANZAS'] },
  { id: 'consumo-tokens', nombre: 'Consumo de IA', seccion: 'IA y sistema', roles: ['SUPERADMIN', 'TECNICO'] },
  { id: 'historial-tokens', nombre: 'Historial de IA', seccion: 'IA y sistema', roles: ['SUPERADMIN', 'TECNICO'] },
  { id: 'estado-servidor', nombre: 'Estado del servidor', seccion: 'IA y sistema', roles: ['SUPERADMIN', 'TECNICO'] },
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
