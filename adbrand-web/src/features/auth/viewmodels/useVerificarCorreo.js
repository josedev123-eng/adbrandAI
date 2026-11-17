// ViewModel: confirmar el correo con el código de 6 dígitos después de registrarse en la web.
// Con el código correcto la persona entra directo, sin volver a escribir la contraseña.
import { useEffect, useState } from 'react'
import { useSesion } from '../../../app/context/useSesion'
import { reenviarCodigoVerificacion, verificarCorreo } from '../models/authApi'

const CODIGO_VALIDO = /^\d{6}$/
// Spring no manda otro código si el anterior tiene menos de un minuto; el botón espera lo mismo.
export const SEGUNDOS_PARA_REENVIAR = 60

// "pedirAlEntrar": si llega desde el inicio de sesión (cuenta sin verificar) se pide un código nuevo al abrir.
export function useVerificarCorreo(correo, { pedirAlEntrar = false } = {}) {
  const { entrar } = useSesion()
  const [codigo, setCodigo] = useState('')
  const [error, setError] = useState(null)
  const [mensaje, setMensaje] = useState(null)
  const [enviando, setEnviando] = useState(false)
  const [espera, setEspera] = useState(pedirAlEntrar ? 0 : SEGUNDOS_PARA_REENVIAR)

  useEffect(() => {
    if (espera <= 0) return undefined
    const reloj = setTimeout(() => setEspera((s) => s - 1), 1000)
    return () => clearTimeout(reloj)
  }, [espera])

  useEffect(() => {
    if (pedirAlEntrar) reenviar()
    // Solo al abrir la pantalla.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  function cambiarCodigo(valor) {
    setCodigo(valor.replace(/\D/g, '').slice(0, 6))
    setError(null)
  }

  async function confirmar() {
    setMensaje(null)
    if (!CODIGO_VALIDO.test(codigo)) {
      setError('El código tiene 6 dígitos.')
      return
    }
    setEnviando(true)
    try {
      const { token, usuario } = await verificarCorreo(correo, codigo)
      entrar(token, usuario)
    } catch (e) {
      if (e.codigo === 'CODIGO_INVALIDO' || e.status === 400) setError(e.message || 'El código no es correcto.')
      else setMensaje({ tipo: 'error', texto: e?.message || 'No se pudo conectar con el servidor.' })
    } finally {
      setEnviando(false)
    }
  }

  async function reenviar() {
    if (espera > 0) return
    setError(null)
    try {
      await reenviarCodigoVerificacion(correo)
      setMensaje({ tipo: 'exito', texto: `Te enviamos un código nuevo a ${correo}. Revisa también la carpeta de spam.` })
      setEspera(SEGUNDOS_PARA_REENVIAR)
    } catch (e) {
      setMensaje({ tipo: 'error', texto: e?.message || 'No se pudo enviar el código.' })
    }
  }

  return { codigo, error, mensaje, enviando, espera, cambiarCodigo, confirmar, reenviar }
}
