// Navegación con direcciones reales (/calendario, /mi-cuenta…) sin librerías, con la History API del navegador:
// - cada pantalla tiene su URL: se puede recargar (F5), guardar en favoritos y compartir el enlace;
// - los botones Atrás y Adelante del navegador recorren las pantallas de la app.
// Importante al publicar: el servidor debe responder index.html para cualquier ruta (ver la guía de despliegue).
import { useEffect, useState } from 'react'

const EVENTO_NAVEGAR = 'adbrand-admin:navegar'

export function rutaActual() {
  // Sin la barra final: "/calendario/" y "/calendario" son la misma pantalla.
  const ruta = window.location.pathname.replace(/\/+$/, '')
  return ruta === '' ? '/' : ruta
}

// Cambia la URL y avisa a la app. "reemplazar" no deja la ruta anterior en el historial (para redirecciones).
export function navegar(ruta, { reemplazar = false } = {}) {
  if (ruta === rutaActual()) return
  if (reemplazar) window.history.replaceState({}, '', ruta)
  else {
    window.history.pushState({}, '', ruta)
    // Al abrir otra pantalla se empieza desde arriba, como en cualquier página web.
    window.scrollTo?.(0, 0)
  }
  window.dispatchEvent(new Event(EVENTO_NAVEGAR))
}

// La ruta actual; se actualiza al navegar dentro de la app y con Atrás/Adelante del navegador.
export function useRuta() {
  const [ruta, setRuta] = useState(rutaActual)
  useEffect(() => {
    const actualizar = () => setRuta(rutaActual())
    window.addEventListener('popstate', actualizar)
    window.addEventListener(EVENTO_NAVEGAR, actualizar)
    return () => {
      window.removeEventListener('popstate', actualizar)
      window.removeEventListener(EVENTO_NAVEGAR, actualizar)
    }
  }, [])
  return ruta
}

// Busca el id de pantalla de una ruta en un mapa { id: '/ruta' }. null si la ruta no existe.
export function pantallaDeRuta(mapa, ruta) {
  return Object.keys(mapa).find((id) => mapa[id] === ruta) ?? null
}
