// Colores del avatar (círculo con las iniciales). Colores planos, todos con buen contraste con el texto blanco.
// Si el usuario no eligió uno, se asigna uno según el nombre (siempre el mismo para la misma empresa).
export const COLORES_AVATAR = [
  { id: 'INDIGO', nombre: 'Índigo', color: '#4F46E5' },
  { id: 'CORAL', nombre: 'Coral', color: '#C2410C' },
  { id: 'VERDE', nombre: 'Verde', color: '#15803D' },
  { id: 'AZUL', nombre: 'Azul', color: '#0369A1' },
  { id: 'VIOLETA', nombre: 'Violeta', color: '#7E22CE' },
  { id: 'ROSA', nombre: 'Rosa', color: '#BE185D' },
]

// Suma simple de los códigos de las letras: el mismo nombre siempre da el mismo color.
function indicePorNombre(nombre) {
  const texto = (nombre ?? '').trim().toUpperCase()
  let suma = 0
  for (const letra of texto) suma += letra.codePointAt(0)
  return suma % COLORES_AVATAR.length
}

export function colorDeAvatar(nombre, elegido) {
  const color = COLORES_AVATAR.find((opcion) => opcion.id === elegido)
  return (color ?? COLORES_AVATAR[indicePorNombre(nombre)]).color
}
