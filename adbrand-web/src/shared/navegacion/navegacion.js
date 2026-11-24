// Navegación con direcciones reales (/calendario, /mi-cuenta…) sin librerías, con la History API del navegador:
// - cada pantalla tiene su URL: se puede recargar (F5), guardar en favoritos y compartir el enlace;
// - los botones Atrás y Adelante del navegador recorren las pantallas de la app.
// Importante al publicar: el servidor debe responder index.html para cualquier ruta (ver la guía de despliegue).
import { useEffect, useState } from 'react'

const EVENTO_NAVEGAR = 'adbrand:navegar'

export const MENSAJE_CAMBIOS_SIN_GUARDAR = 'Tienes cambios sin guardar. Si sales ahora, se perderán. ¿Quieres salir igual?'

// Pantallas con un formulario a medio editar (ver useCambiosSinGuardar). Mientras haya alguna, salir pide confirmar.
const pendientes = new Set()
let ultimaRuta = typeof window === 'undefined' ? '/' : null

export function marcarCambiosPendientes(clave, hay) {
  if (hay) pendientes.add(clave)
  else pendientes.delete(clave)
}

export function hayCambiosPendientes() {
  return pendientes.size > 0
}

// true si se puede salir: no hay cambios pendientes o el usuario confirmó que quiere salir igual.
function puedeSalir() {
  if (!hayCambiosPendientes()) return true
  const confirmado = window.confirm(MENSAJE_CAMBIOS_SIN_GUARDAR)
  if (confirmado) pendientes.clear()
  return confirmado
}

export function rutaActual() {
  // Sin la barra final: "/calendario/" y "/calendario" son la misma pantalla.
  const ruta = window.location.pathname.replace(/\/+$/, '')
  return ruta === '' ? '/' : ruta
}

// Cambia la URL y avisa a la app. "reemplazar" no deja la ruta anterior en el historial (para redirecciones).
// Si hay cambios sin guardar, primero pregunta; si el usuario se queda, no navega y devuelve false.
export function navegar(ruta, { reemplazar = false, sinPreguntar = false } = {}) {
  if (ruta === rutaActual()) return true
  if (!sinPreguntar && !reemplazar && !puedeSalir()) return false
  if (reemplazar) window.history.replaceState({}, '', ruta)
  else {
    window.history.pushState({}, '', ruta)
    // Al abrir otra pantalla se empieza desde arriba, como en cualquier página web.
    window.scrollTo?.(0, 0)
  }
  ultimaRuta = ruta
  window.dispatchEvent(new Event(EVENTO_NAVEGAR))
  return true
}

// La ruta actual; se actualiza al navegar dentro de la app y con Atrás/Adelante del navegador.
export function useRuta() {
  const [ruta, setRuta] = useState(rutaActual)
  useEffect(() => {
    ultimaRuta = rutaActual()
    const actualizar = () => {
      ultimaRuta = rutaActual()
      setRuta(ultimaRuta)
    }
    // Atrás/Adelante con cambios sin guardar: si el usuario se queda, se vuelve a la dirección donde estaba.
    const alVolver = () => {
      if (!puedeSalir()) {
        window.history.pushState({}, '', ultimaRuta)
        return
      }
      actualizar()
    }
    window.addEventListener('popstate', alVolver)
    window.addEventListener(EVENTO_NAVEGAR, actualizar)
    return () => {
      window.removeEventListener('popstate', alVolver)
      window.removeEventListener(EVENTO_NAVEGAR, actualizar)
    }
  }, [])
  return ruta
}

// Busca el id de pantalla de una ruta en un mapa { id: '/ruta' }. null si la ruta no existe.
export function pantallaDeRuta(mapa, ruta) {
  return Object.keys(mapa).find((id) => mapa[id] === ruta) ?? null
}
