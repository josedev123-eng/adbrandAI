// ViewModel: avatar con imagen en Mi Cuenta. Usa el logo del kit o una foto (PNG/JPG hasta 3 MB) y avisa a App
// para que el menú muestre el avatar nuevo al instante.
import { useState } from 'react'
import { cambiarAvatar, quitarAvatar } from '../models/cuentaApi'

const TIPOS = ['image/png', 'image/jpeg']
const MAXIMO = 3 * 1024 * 1024

function leerComoDataUrl(archivo) {
  return new Promise((resolver, rechazar) => {
    const lector = new FileReader()
    lector.onload = () => resolver(lector.result)
    lector.onerror = () => rechazar(lector.error)
    lector.readAsDataURL(archivo)
  })
}

export function useAvatar(alCambiar) {
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState(null)

  async function aplicar(peticion) {
    setError(null)
    setGuardando(true)
    try {
      const usuario = await peticion()
      alCambiar?.(usuario?.avatarImagen ?? null)
    } catch (e) {
      setError(e?.message || 'No se pudo cambiar tu avatar.')
    } finally {
      setGuardando(false)
    }
  }

  function usarLogoDelKit() {
    return aplicar(() => cambiarAvatar({ usarLogoDelKit: true }))
  }

  async function subirFoto(archivo) {
    if (!archivo) return
    if (!TIPOS.includes(archivo.type)) {
      setError('Elige una imagen PNG o JPG.')
      return
    }
    if (archivo.size > MAXIMO) {
      setError('La imagen puede pesar hasta 3 MB.')
      return
    }
    await aplicar(async () => cambiarAvatar({ imagen: await leerComoDataUrl(archivo) }))
  }

  function quitar() {
    return aplicar(() => quitarAvatar())
  }

  return { guardando, error, usarLogoDelKit, subirFoto, quitar }
}
