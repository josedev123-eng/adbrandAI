// HU 26: con llaves de Mercado Pago se usa su formulario; sin ellas, el formulario simulado.
// El SDK de Mercado Pago se reemplaza por uno falso: estas pruebas no salen a internet.
import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { cargarSdkMercadoPago } from '../models/mercadoPagoSdk'
import { obtenerConfiguracionPago, pagarConMercadoPago } from '../models/pagosApi'
import PagoConTarjetaView from './PagoConTarjetaView'

vi.mock('../models/pagosApi', () => ({
  obtenerConfiguracionPago: vi.fn(),
  pagarConMercadoPago: vi.fn(),
  pagarConTarjeta: vi.fn(),
}))

vi.mock('../models/mercadoPagoSdk', () => ({
  cargarSdkMercadoPago: vi.fn(),
}))

const PLAN = { id: 2, nombre: 'Pro', precio: 59.9, limiteTokens: 150000 }

const DATOS_TARJETA = {
  token: 'tok_123',
  payment_method_id: 'visa',
  issuer_id: 310,
  installments: 1,
  payer: { email: 'ana@correo.pe' },
}

// SDK falso: guarda lo que se le pasa al crear el formulario para poder simular el envío.
function sdkFalso() {
  const brick = { opciones: null, publicKey: null, desmontado: false }
  class MercadoPagoFalso {
    constructor(publicKey) {
      brick.publicKey = publicKey
    }

    bricks() {
      return {
        create: async (tipo, contenedor, opciones) => {
          brick.tipo = tipo
          brick.contenedor = contenedor
          brick.opciones = opciones
          opciones.callbacks.onReady()
          return { unmount: () => (brick.desmontado = true) }
        },
      }
    }
  }
  cargarSdkMercadoPago.mockResolvedValue(MercadoPagoFalso)
  return brick
}

describe('PagoConTarjetaView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('sin llaves de Mercado Pago muestra el formulario simulado', async () => {
    obtenerConfiguracionPago.mockResolvedValue({ pasarela: 'simulada', publicKey: null })

    render(<PagoConTarjetaView plan={PLAN} alPagar={() => {}} alVolver={() => {}} />)

    expect(await screen.findByLabelText(/Número de la tarjeta/)).toBeInTheDocument()
    expect(cargarSdkMercadoPago).not.toHaveBeenCalled()
  })

  it('si no se puede consultar la configuración usa el formulario simulado', async () => {
    obtenerConfiguracionPago.mockRejectedValue(new Error('sin conexión'))

    render(<PagoConTarjetaView plan={PLAN} alPagar={() => {}} alVolver={() => {}} />)

    expect(await screen.findByLabelText(/Número de la tarjeta/)).toBeInTheDocument()
  })

  it('con Mercado Pago crea su formulario con la llave pública y el monto del plan', async () => {
    obtenerConfiguracionPago.mockResolvedValue({ pasarela: 'mercadopago', publicKey: 'TEST-publica' })
    const brick = sdkFalso()

    render(<PagoConTarjetaView plan={PLAN} alPagar={() => {}} alVolver={() => {}} />)

    await waitFor(() => expect(brick.opciones).not.toBeNull())
    expect(brick.publicKey).toBe('TEST-publica')
    expect(brick.tipo).toBe('cardPayment')
    expect(brick.opciones.initialization.amount).toBe(59.9)
    expect(screen.queryByLabelText(/Número de la tarjeta/)).not.toBeInTheDocument()
  })

  it('manda solo el token al backend y entrega el pago aprobado', async () => {
    obtenerConfiguracionPago.mockResolvedValue({ pasarela: 'mercadopago', publicKey: 'TEST-publica' })
    const brick = sdkFalso()
    const pago = { id: 9, plan: 'Pro', estado: 'APROBADO', referencia: 'MP-999' }
    pagarConMercadoPago.mockResolvedValue(pago)
    const alPagar = vi.fn()

    render(<PagoConTarjetaView plan={PLAN} alPagar={alPagar} alVolver={() => {}} />)
    await waitFor(() => expect(brick.opciones).not.toBeNull())
    let envio
    act(() => {
      envio = brick.opciones.callbacks.onSubmit(DATOS_TARJETA, { cardholderName: 'APRO', lastFourDigits: '0604', bin: '503175' })
    })
    expect(await screen.findByRole('dialog', { name: '¿Confirmas el pago?' })).toHaveTextContent('Tarjeta •••• 0604')
    await userEvent.click(screen.getByRole('button', { name: /Sí, pagar/ }))
    await act(() => envio)

    expect(pagarConMercadoPago).toHaveBeenCalledWith({
      planId: 2,
      token: 'tok_123',
      metodoPago: 'visa',
      emisorId: '310',
      correo: 'ana@correo.pe',
      titular: 'APRO',
      ultimosDigitos: '0604',
      tipoTarjeta: '',
    })
    expect(alPagar).toHaveBeenCalledWith(pago)
  })

  it('si Mercado Pago rechaza la tarjeta muestra el motivo y deja reintentar', async () => {
    obtenerConfiguracionPago.mockResolvedValue({ pasarela: 'mercadopago', publicKey: 'TEST-publica' })
    const brick = sdkFalso()
    pagarConMercadoPago.mockRejectedValue(
      Object.assign(new Error('La tarjeta no tiene fondos suficientes.'), { status: 400, codigo: 'PAGO_RECHAZADO' }),
    )
    const alPagar = vi.fn()

    render(<PagoConTarjetaView plan={PLAN} alPagar={alPagar} alVolver={() => {}} />)
    await waitFor(() => expect(brick.opciones).not.toBeNull())
    let envio
    act(() => {
      // El error se captura enseguida para que no quede como rechazo sin atender.
      envio = brick.opciones.callbacks.onSubmit(DATOS_TARJETA).catch((error) => error)
    })
    await userEvent.click(await screen.findByRole('button', { name: /Sí, pagar/ }))
    await act(async () => {
      expect(await envio).toBeInstanceOf(Error)
    })

    expect(await screen.findByText('La tarjeta no tiene fondos suficientes.')).toBeInTheDocument()
    expect(alPagar).not.toHaveBeenCalled()
  })

  it('si cancela en "Confirma tu pago" no se cobra y el formulario deja reintentar', async () => {
    obtenerConfiguracionPago.mockResolvedValue({ pasarela: 'mercadopago', publicKey: 'TEST-publica' })
    const brick = sdkFalso()

    render(<PagoConTarjetaView plan={PLAN} alPagar={vi.fn()} alVolver={() => {}} />)
    await waitFor(() => expect(brick.opciones).not.toBeNull())
    let envio
    act(() => {
      // El error se captura enseguida para que no quede como rechazo sin atender.
      envio = brick.opciones.callbacks.onSubmit(DATOS_TARJETA).catch((error) => error)
    })
    await userEvent.click(await screen.findByRole('button', { name: 'Cancelar' }))
    await act(async () => {
      expect(await envio).toBeInstanceOf(Error)
    })

    expect(pagarConMercadoPago).not.toHaveBeenCalled()
    expect(screen.getByText('Cancelaste el pago. No se hizo ningún cobro.')).toBeInTheDocument()
  })

  it('si el SDK no carga avisa al usuario', async () => {
    obtenerConfiguracionPago.mockResolvedValue({ pasarela: 'mercadopago', publicKey: 'TEST-publica' })
    cargarSdkMercadoPago.mockRejectedValue(new Error('sin internet'))

    render(<PagoConTarjetaView plan={PLAN} alPagar={() => {}} alVolver={() => {}} />)

    expect(await screen.findByText(/No se pudo cargar el formulario de Mercado Pago/)).toBeInTheDocument()
  })
})
