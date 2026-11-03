// Reglas que comparten el registro, el inicio de sesión y la recuperación de contraseña (HU-21).
// Son las mismas que valida Spring en RegistroRequest y ReglasContrasena.

const CORREO_VALIDO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export function esCorreoValido(correo) {
  return CORREO_VALIDO.test(correo.trim())
}

// Mínimo 8 caracteres, con una mayúscula, una minúscula y un número, y sin espacios (regla de la HU 1).
export function problemaDeContrasena(contrasena) {
  if (!contrasena) return 'Ingresa una contraseña.'
  if (/\s/.test(contrasena)) return 'La contraseña no puede tener espacios.'
  if (contrasena.length < 8) return 'Debe tener al menos 8 caracteres.'
  if (contrasena.length > 72) return 'Debe tener como máximo 72 caracteres.'
  if (!/[A-Z]/.test(contrasena)) return 'Debe tener al menos una letra mayúscula.'
  if (!/[a-z]/.test(contrasena)) return 'Debe tener al menos una letra minúscula.'
  if (!/[0-9]/.test(contrasena)) return 'Debe tener al menos un número.'
  return null
}
