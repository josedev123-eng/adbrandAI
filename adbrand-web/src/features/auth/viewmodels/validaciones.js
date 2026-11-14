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

// Nombre de persona: solo letras (con tildes y ñ), espacios, apóstrofo o guion. Entre 2 y 100 caracteres.
const NOMBRE_PERSONA = /^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+([ '-][A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+)*$/

export function problemaDeNombre(nombre) {
  const texto = nombre.trim()
  if (!texto) return 'Ingresa tu nombre.'
  if (texto.length < 2) return 'Debe tener al menos 2 letras.'
  if (texto.length > 100) return 'Máximo 100 caracteres.'
  if (!NOMBRE_PERSONA.test(texto)) return 'Usa solo letras y espacios, sin números ni símbolos.'
  return null
}

// Nombre de empresa: letras, números y signos comunes (& . , - '). Entre 2 y 150 caracteres.
const NOMBRE_EMPRESA = /^[0-9A-Za-zÁÉÍÓÚÜÑáéíóúüñ&.,'\- ]+$/

export function problemaDeEmpresa(nombre) {
  const texto = nombre.trim()
  if (!texto) return 'Ingresa el nombre de tu empresa.'
  if (texto.length < 2) return 'Debe tener al menos 2 caracteres.'
  if (texto.length > 150) return 'Máximo 150 caracteres.'
  if (!NOMBRE_EMPRESA.test(texto)) return 'Usa letras, números y signos simples (& . , -).'
  return null
}

// El correo se guarda sin espacios y en minúsculas, para que "Rosa@Mail.com" y "rosa@mail.com" sean el mismo.
export function normalizarCorreo(correo) {
  return correo.trim().toLowerCase()
}
