// Datos del menú lateral de la web del usuario (las secciones y sus opciones) y ayudas para dibujarlo.
// Opciones agrupadas por sección, en el orden en que se ven.
export const SECCIONES = [
  // Inicio va solo, arriba de todo y sin título de sección.
  { titulo: '', opciones: [{ id: 'inicio', nombre: 'Inicio' }] },
  {
    titulo: 'Crear',
    opciones: [
      { id: 'anuncio', nombre: 'Crear anuncio' },
      { id: 'misAnuncios', nombre: 'Mis anuncios' },
      { id: 'kit', nombre: 'Kit de Marca' },
    ],
  },
  {
    titulo: 'Planificar',
    opciones: [
      { id: 'calendario', nombre: 'Calendario' },
      { id: 'informes', nombre: 'Informes' },
    ],
  },
  {
    titulo: 'Mi empresa',
    opciones: [
      { id: 'perfil', nombre: 'Mi negocio' },
      { id: 'planes', nombre: 'Planes' },
      { id: 'cuenta', nombre: 'Mi cuenta' },
    ],
  },
]

// "Panadería Doña Rosa" → "PD"
export function iniciales(nombre) {
  const palabras = (nombre ?? '').trim().split(/\s+/).filter(Boolean)
  return palabras.slice(0, 2).map((palabra) => palabra[0].toUpperCase()).join('') || 'A'
}
