// ViewModel para el menú: el estado de la creación de opciones en curso ('creando', 'lista', 'error' o null).
// Así el menú avisa en "Crear anuncio" cuando las opciones quedaron listas mientras el dueño estaba en otra pantalla.
import { useEffect, useState } from 'react'
import { generacionActual, suscribirGeneracion } from '../models/generacionEnCurso'

export function useGeneracionEnCurso() {
  const [estado, setEstado] = useState(() => generacionActual()?.estado ?? null)
  useEffect(() => suscribirGeneracion((actual) => setEstado(actual?.estado ?? null)), [])
  return estado
}
