// Sesión del usuario PYME (HU-22): usuario, cargando, entrar y salir, en cualquier pantalla.
// El contexto vive aquí y el componente en SesionProvider.jsx: el lint de React no deja exportar
// un componente y un hook desde el mismo archivo.
import { createContext, useContext } from 'react'

export const SesionContexto = createContext(null)

export function useSesion() {
  const sesion = useContext(SesionContexto)
  if (!sesion) throw new Error('useSesion se usa dentro de <SesionProvider>.')
  return sesion
}
