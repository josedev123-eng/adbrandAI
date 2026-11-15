// Reglas de contraseña del panel: las mismas que valida Django (usuarios/serializers.py).
export function problemaDeContrasena(valor) {
  if (!valor) return 'Escribe la contraseña nueva.'
  if (valor.length < 8) return 'Usa al menos 8 caracteres.'
  if (/\s/.test(valor)) return 'La contraseña no puede tener espacios.'
  if (!/[A-Z]/.test(valor)) return 'Agrega al menos una letra mayúscula.'
  if (!/[a-z]/.test(valor)) return 'Agrega al menos una letra minúscula.'
  if (!/[0-9]/.test(valor)) return 'Agrega al menos un número.'
  return null
}

// Django devuelve los errores por campo como listas: { nueva: ['…'] }. Se toma el primero de cada uno.
export function erroresDeDjango(detalles = {}) {
  const errores = {}
  for (const [campo, mensajes] of Object.entries(detalles ?? {})) {
    if (campo !== 'detail') errores[campo] = Array.isArray(mensajes) ? mensajes[0] : String(mensajes)
  }
  return errores
}
