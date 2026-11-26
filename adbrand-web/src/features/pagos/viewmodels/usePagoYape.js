// ViewModel: genera el QR de Yape, lo dibuja y confirma la operación cuando el usuario ya pagó (HU 27).
import { useState } from 'react'
import QRCode from 'qrcode'
import { confirmarYape, registrarYape } from '../models/pagosApi'

// "confirmar": modal "Confirma tu pago" antes de avisar que ya se pagó (true si confirma).
export function usePagoYape(plan, alConfirmar, confirmar = async () => true) {
  const [operacion, setOperacion] = useState(null)
  const [qr, setQr] = useState(null)
  const [cargando, setCargando] = useState(false)
  const [confirmando, setConfirmando] = useState(false)
  const [error, setError] = useState(null)

  async function generar() {
    setError(null)
    setCargando(true)
    try {
      const nueva = await registrarYape({ planId: plan.id })
      setOperacion(nueva)
      setQr(await QRCode.toDataURL(nueva.contenidoQr, { width: 240, margin: 1 }))
    } catch (fallo) {
      setError(fallo.message)
    } finally {
      setCargando(false)
    }
  }

  async function confirmarPago() {
    setError(null)
    if (!(await confirmar({ metodo: 'Yape (QR de AdBrand)' }))) return
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
    operacion,
    qr,
    cargando,
    confirmando,
    error,
    generar,
    confirmar: confirmarPago,
  }
}
