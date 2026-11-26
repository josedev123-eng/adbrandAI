// ViewModel: formulario de tarjeta de Mercado Pago (Card Payment Brick) para la HU 26.
// El Brick pide la tarjeta y el correo, crea el token y nos lo entrega en onSubmit;
// aquí solo mandamos ese token al backend, que hace el cobro.
import { useEffect, useRef, useState } from 'react'
import { cargarSdkMercadoPago } from '../models/mercadoPagoSdk'
import { pagarConMercadoPago } from '../models/pagosApi'

export const ID_CONTENEDOR = 'formulario-mercadopago'

const TIPOS_TARJETA = ['credit_card', 'debit_card', 'prepaid_card']

// Traduce lo que entrega el Brick a lo que espera POST /api/pagos/mercadopago.
// datosExtra (segundo argumento de onSubmit) trae el titular y los últimos 4 dígitos para el comprobante.
export function datosDelPago(plan, datosTarjeta, datosExtra = {}) {
  const tipo = datosTarjeta.payment_type_id ?? datosExtra?.paymentTypeId
  const digitos = datosExtra?.lastFourDigits
  return {
    planId: plan.id,
    token: datosTarjeta.token,
    metodoPago: datosTarjeta.payment_method_id,
    emisorId: datosTarjeta.issuer_id ? String(datosTarjeta.issuer_id) : '',
    correo: datosTarjeta.payer?.email ?? '',
    titular: datosExtra?.cardholderName ?? '',
    ultimosDigitos: /^\d{4}$/.test(digitos ?? '') ? digitos : '',
    tipoTarjeta: TIPOS_TARJETA.includes(tipo) ? tipo : '',
  }
}

// "confirmar": modal "Confirma tu pago" antes de cobrar (true si confirma).
export function usePagoMercadoPago(plan, publicKey, alPagar, confirmar = async () => true) {
  const [listo, setListo] = useState(false)
  const [mensaje, setMensaje] = useState(null)
  const [enviando, setEnviando] = useState(false)
  // alPagar puede cambiar en cada render; el Brick se crea una sola vez y lee siempre el último.
  const alPagarActual = useRef(alPagar)
  const confirmarActual = useRef(confirmar)
  useEffect(() => {
    alPagarActual.current = alPagar
    confirmarActual.current = confirmar
  }, [alPagar, confirmar])

  useEffect(() => {
    let controlador = null
    let cancelado = false

    async function crearFormulario() {
      try {
        const MercadoPago = await cargarSdkMercadoPago()
        if (cancelado) return
        const mp = new MercadoPago(publicKey, { locale: 'es-PE' })
        controlador = await mp.bricks().create('cardPayment', ID_CONTENEDOR, {
          initialization: { amount: Number(plan.precio) },
          customization: { paymentMethods: { maxInstallments: 1 } },
          callbacks: {
            onReady: () => {
              if (!cancelado) setListo(true)
            },
            onSubmit: (datosTarjeta, datosExtra) => enviar(datosTarjeta, datosExtra),
            onError: () => {
              if (!cancelado) setMensaje({ tipo: 'error', texto: 'Revisa los datos de la tarjeta e intenta de nuevo.' })
            },
          },
        })
        if (cancelado) controlador?.unmount()
      } catch {
        if (!cancelado) {
          setMensaje({
            tipo: 'error',
            texto: 'No se pudo cargar el formulario de Mercado Pago. Revisa tu conexión y vuelve a intentarlo.',
          })
        }
      }
    }

    // El Brick espera una promesa: si se rechaza, deja corregir la tarjeta y reintentar.
    async function enviar(datosTarjeta, datosExtra) {
      setMensaje(null)
      const digitos = /^\d{4}$/.test(datosExtra?.lastFourDigits ?? '') ? ` •••• ${datosExtra.lastFourDigits}` : ''
      if (!(await confirmarActual.current({ metodo: `Tarjeta${digitos}` }))) {
        setMensaje({ tipo: 'info', texto: 'Cancelaste el pago. No se hizo ningún cobro.' })
        // El Brick espera que se rechace para dejar volver a intentarlo.
        throw new Error('Pago cancelado por el usuario.')
      }
      setEnviando(true)
      try {
        const pago = await pagarConMercadoPago(datosDelPago(plan, datosTarjeta, datosExtra))
        alPagarActual.current(pago)
      } catch (error) {
        setMensaje({ tipo: 'error', texto: error.message || 'No se pudo completar el pago.' })
        throw error
      } finally {
        setEnviando(false)
      }
    }

    crearFormulario()
    return () => {
      cancelado = true
      controlador?.unmount()
    }
  }, [plan, publicKey])

  return { listo, mensaje, enviando }
}
