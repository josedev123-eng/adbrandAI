// Guarda la sesión del administrador conectado y la comparte con toda la web.
import { useCallback, useEffect, useState } from 'react'
import { obtenerSesion } from '../../features/auth/models/authApi'
import { EVENTO_SESION_VENCIDA } from '../../shared/services/apiClient'
import { borrarToken, guardarToken, leerToken } from '../../shared/services/sesionToken'
import { SesionContext } from './useSesion'

export function SesionProvider({ children }) {
  const [admin, setAdmin] = useState(null)
  // Si hay un token guardado, primero hay que preguntar a Django si sigue vivo.
  const [cargando, setCargando] = useState(() => Boolean(leerToken()))
  const [aviso, setAviso] = useState(null)

  useEffect(() => {
    if (!leerToken()) return
    let activo = true
    obtenerSesion()
      .then((datos) => activo && setAdmin(datos))
      .catch(() => {
        borrarToken()
        if (activo) setAdmin(null)
      })
      .finally(() => activo && setCargando(false))
    return () => {
      activo = false
    }
  }, [])

  // Si cualquier pedido responde 401, se cierra la sesión y se avisa en el login.
  useEffect(() => {
    function alVencer() {
      setAdmin(null)
      setAviso('Tu sesión venció. Vuelve a iniciar sesión.')
    }
    window.addEventListener(EVENTO_SESION_VENCIDA, alVencer)
    return () => window.removeEventListener(EVENTO_SESION_VENCIDA, alVencer)
  }, [])

  const entrar = useCallback((token, datosAdmin) => {
    guardarToken(token)
    setAviso(null)
    setAdmin(datosAdmin)
  }, [])

  const salir = useCallback(() => {
    borrarToken()
    setAviso(null)
    setAdmin(null)
  }, [])

  return (
    <SesionContext.Provider value={{ admin, cargando, aviso, entrar, salir }}>
      {children}
    </SesionContext.Provider>
  )
}
