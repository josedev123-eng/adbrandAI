// Pone el nombre de la pantalla en la pestaña: "Auditoría · Admin AdBrand.AI".
// Ayuda a ubicarse con varias pestañas abiertas, en el historial y a los lectores de pantalla.
import { useEffect } from 'react'

export const NOMBRE_APP = 'AdBrand.AI'

export function tituloDePagina(nombre) {
  return nombre ? `${nombre} · Admin ${NOMBRE_APP}` : `Panel de administración · ${NOMBRE_APP}`
}

export function useTituloPagina(nombre) {
  useEffect(() => {
    document.title = tituloDePagina(nombre)
  }, [nombre])
}
