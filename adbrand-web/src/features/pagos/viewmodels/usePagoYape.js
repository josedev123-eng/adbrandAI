// ViewModel: genera el QR de Yape, lo dibuja y confirma la operación cuando el usuario ya pagó (HU 27).
import { useState } from 'react'
import QRCode from 'qrcode'
import { confirmarYape, registrarYape } from '../models/pagosApi'

export function celularValido(valor) {
  return valor.trim() === '' || /^9\d{8}$/.test(valor.trim())
}

export function usePagoYape(plan, alConfirmar) {
  const [celular, setCelular] = useState('')
  const [operacion, setOperacion] = useState(null)
  const [qr, setQr] = useState(null)
  const [cargando, setCargando] = useState(false)
  const [confirmando, setConfirmando] = useState(false)
  const [error, setError] = useState(null)

  async function generar() {
    setError(null)
    if (!celularValido(celular)) {
      setError('El celular debe tener 9 dígitos y empezar con 9.')
      return
    }

    setCargando(true)
    try {
      const nueva = await registrarYape({
        planId: plan.id,
        celular: celular.trim() === '' ? null : celular.trim(),
      })
      setOperacion(nueva)
      setQr(await QRCode.toDataURL(nueva.contenidoQr, { width: 240, margin: 1 }))
    } catch (fallo) {
      setError(fallo.message)
    } finally {
      setCargando(false)
    }
  }

  async function confirmar() {
    setError(null)
    setConfirmando(true)
    try {
      const pago = await confirmarYape(operacion.id)
      alConfirmar(pago)
    } catch (fallo) {
      setError(fallo.message)
    } finally {
      setConfirmando(false)
    }
  }

  return {
    celular,
    cambiarCelular: setCelular,
    operacion,
    qr,
    cargando,
    confirmando,
    error,
    generar,
    confirmar,
  }
}
