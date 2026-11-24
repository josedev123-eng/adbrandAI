// Pone el nombre de la pantalla en la pestaña del navegador: "Mi cuenta · AdBrand.AI".
// Ayuda a ubicarse con varias pestañas abiertas, en el historial y a los lectores de pantalla.
import { useEffect } from 'react'

export const NOMBRE_APP = 'AdBrand.AI'

export function tituloDePagina(nombre) {
  return nombre ? `${nombre} · ${NOMBRE_APP}` : `${NOMBRE_APP} · Anuncios con IA para tu negocio`
}

export function useTituloPagina(nombre) {
  useEffect(() => {
    document.title = tituloDePagina(nombre)
  }, [nombre])
}
