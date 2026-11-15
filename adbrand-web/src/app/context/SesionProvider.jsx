// Guarda quién inició sesión y lo comparte con toda la web (HU-22).
import { useCallback, useEffect, useMemo, useState } from 'react'
import { obtenerSesion } from '../../features/auth/models/authApi'
import { EVENTO_SESION_VENCIDA, MENSAJES } from '../../shared/services/apiClient'
import { borrarToken, guardarToken, leerToken } from '../../shared/services/sesionToken'
import { SesionContexto } from './useSesion'

export default function SesionProvider({ children }) {
  const [usuario, setUsuario] = useState(null)
  const [avisoSalida, setAvisoSalida] = useState(null)
  // Si hay un token guardado, primero se revisa con Spring antes de mostrar nada.
  const [cargando, setCargando] = useState(() => Boolean(leerToken()))

  // Al abrir la página: si hay token, se pide el usuario. Si ya no sirve, se borra.
  useEffect(() => {
    if (!leerToken()) return
    let vigente = true

    obtenerSesion()
      .then((datos) => {
        if (vigente) setUsuario(datos)
      })
      .catch(() => {
        borrarToken()
        if (vigente) setUsuario(null)
      })
      .finally(() => {
        if (vigente) setCargando(false)
      })

    return () => {
      vigente = false
    }
  }, [])

  // Si Spring responde 401 en cualquier pantalla, apiClient avisa y se vuelve al inicio de sesión.
  useEffect(() => {
    const alVencer = (evento) => {
      setUsuario(null)
      // El login muestra por qué se cerró la sesión: el motivo de la suspensión o que la sesión venció.
      setAvisoSalida(evento?.detail?.motivo ?? MENSAJES.SESION_VENCIDA)
    }
    window.addEventListener(EVENTO_SESION_VENCIDA, alVencer)
    return () => window.removeEventListener(EVENTO_SESION_VENCIDA, alVencer)
  }, [])

  const entrar = useCallback((token, datosUsuario) => {
    setAvisoSalida(null)
    guardarToken(token)
    setUsuario(datosUsuario)
  }, [])

  const salir = useCallback(() => {
    borrarToken()
    setUsuario(null)
  }, [])

  // Cambia algún dato del usuario en pantalla (por ejemplo el color del avatar) sin volver a pedir la sesión.
  const actualizarUsuario = useCallback((cambios) => {
    setUsuario((actual) => (actual ? { ...actual, ...cambios } : actual))
  }, [])

  const valor = useMemo(
    () => ({ usuario, cargando, entrar, salir, actualizarUsuario, avisoSalida }),
    [usuario, cargando, entrar, salir, actualizarUsuario, avisoSalida],
  )

  return <SesionContexto.Provider value={valor}>{children}</SesionContexto.Provider>
}
