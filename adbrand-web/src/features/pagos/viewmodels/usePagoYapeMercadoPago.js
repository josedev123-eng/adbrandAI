// ViewModel: Yape por Mercado Pago (HU 27). El usuario escribe su celular y el código de aprobación de su app
// Yape; el SDK de Mercado Pago los convierte en un token y el backend cobra con ese token.
// El código de aprobación no se envía a nuestro servidor.
import { useState } from 'react'
import { cargarSdkMercadoPago } from '../models/mercadoPagoSdk'
import { pagarYapeConMercadoPago } from '../models/pagosApi'

const FORMULARIO_VACIO = { celular: '', codigo: '', correo: '' }

export function validarYape(formulario) {
  const errores = {}
  const celular = formulario.celular.replace(/\s/g, '')
  if (!celular) errores.celular = 'Escribe tu número de celular.'
  else if (!/^\d{9}$/.test(celular)) errores.celular = 'El celular tiene 9 dígitos.'
  if (!formulario.codigo.trim()) errores.codigo = 'Escribe el código de aprobación de Yape.'
  else if (!/^\d{6}$/.test(formulario.codigo.trim())) errores.codigo = 'El código de aprobación tiene 6 dígitos.'
  if (!formulario.correo.trim()) errores.correo = 'Escribe tu correo.'
  else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formulario.correo.trim())) errores.correo = 'Escribe un correo válido.'
  return errores
}

// "confirmar": modal "Confirma tu pago" (true si confirma).
export function usePagoYapeMercadoPago(plan, publicKey, alConfirmar, confirmar = async () => true) {
  const [formulario, setFormulario] = useState(FORMULARIO_VACIO)
  const [errores, setErrores] = useState({})
  const [mensaje, setMensaje] = useState(null)
  const [enviando, setEnviando] = useState(false)

  function cambiarCampo(campo, valor) {
    setFormulario((actual) => ({ ...actual, [campo]: valor }))
    setErrores((actuales) => ({ ...actuales, [campo]: undefined }))
  }

  async function pagar() {
    setMensaje(null)
    const locales = validarYape(formulario)
    setErrores(locales)
    if (Object.keys(locales).length > 0) return

    const celular = formulario.celular.replace(/\s/g, '')
    if (!(await confirmar({ metodo: `Yape ${celular}` }))) return
    setEnviando(true)
    try {
      let token
      try {
        const MercadoPago = await cargarSdkMercadoPago()
        const mp = new MercadoPago(publicKey, { locale: 'es-PE' })
        // El token es de un solo uso: se crea uno nuevo en cada intento.
        const yapeToken = await mp.yape({ otp: formulario.codigo.trim(), phoneNumber: celular }).create()
        token = yapeToken?.id
      } catch {
        token = null
      }
      if (!token) {
        setMensaje({ tipo: 'error', texto: 'Yape no aceptó el celular o el código. Revisa los datos y genera un código nuevo.' })
        return
      }

      const pago = await pagarYapeConMercadoPago({ planId: plan.id, token, celular, correo: formulario.correo.trim() })
      alConfirmar(pago)
    } catch (error) {
      if (error.campos && Object.keys(error.campos).length > 0) setErrores(error.campos)
      setMensaje({ tipo: 'error', texto: error.message || 'No se pudo completar el pago con Yape.' })
      // El código ya se usó: hay que pedir otro.
      setFormulario((actual) => ({ ...actual, codigo: '' }))
    } finally {
      setEnviando(false)
    }
  }

  return { formulario, errores, mensaje, enviando, cambiarCampo, pagar }
}
