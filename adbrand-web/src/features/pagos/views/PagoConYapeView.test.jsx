// HU 27: con llaves de Mercado Pago, Yape con celular y código de aprobación; sin ellas, el QR simulado.
// El SDK de Mercado Pago se reemplaza por uno falso: estas pruebas no salen a internet.
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { cargarSdkMercadoPago } from '../models/mercadoPagoSdk'
import { obtenerConfiguracionPago, pagarYapeConMercadoPago } from '../models/pagosApi'
import PagoConYapeView from './PagoConYapeView'

vi.mock('../models/pagosApi', () => ({
  obtenerConfiguracionPago: vi.fn(),
  pagarYapeConMercadoPago: vi.fn(),
  registrarYape: vi.fn(),
  confirmarYape: vi.fn(),
  obtenerYapePendientes: vi.fn().mockResolvedValue([]),
}))

vi.mock('../models/mercadoPagoSdk', () => ({
  cargarSdkMercadoPago: vi.fn(),
}))

const PLAN = { id: 1, nombre: 'Básico', precio: 29.9, limiteTokens: 50000 }

// SDK falso: guarda lo que se le pasa a mp.yape() y devuelve el token que se le indique.
function sdkFalso(resultado = { id: 'yape_tok' }) {
  const llamadas = { yape: null }
  class MercadoPagoFalso {
    yape(datos) {
      llamadas.yape = datos
      return { create: async () => resultado }
    }
  }
  cargarSdkMercadoPago.mockResolvedValue(MercadoPagoFalso)
  return llamadas
}

async function llenarYPagar({ celular = '111111111', codigo = '123456', correo = 'comprador@testuser.com' } = {}) {
  const usuario = userEvent.setup()
  if (celular) await usuario.type(await screen.findByLabelText(/Celular de Yape/), celular)
  if (codigo) await usuario.type(screen.getByLabelText(/Código de aprobación/), codigo)
  if (correo) await usuario.type(screen.getByLabelText(/Correo/), correo)
  await usuario.click(screen.getByRole('button', { name: /con Yape/ }))
  // Con datos válidos sale "Confirma tu pago": se confirma.
  const confirmar = screen.queryByRole('button', { name: /Sí, pagar/ })
  if (confirmar) await usuario.click(confirmar)
}

describe('PagoConYapeView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    obtenerConfiguracionPago.mockResolvedValue({ pasarela: 'mercadopago', publicKey: 'TEST-publica' })
  })

  it('sin llaves de Mercado Pago muestra el QR simulado', async () => {
    obtenerConfiguracionPago.mockResolvedValue({ pasarela: 'simulada', publicKey: null })

    render(<PagoConYapeView plan={PLAN} alConfirmar={() => {}} alVolver={() => {}} />)

    expect(await screen.findByRole('button', { name: 'Generar QR de pago' })).toBeInTheDocument()
  })

  it('crea el token con el celular y el código y paga sin mandar el código al backend', async () => {
    const llamadas = sdkFalso()
    const pago = { id: 7, plan: 'Básico', estado: 'APROBADO', referencia: 'MP-ORDYAPE' }
    pagarYapeConMercadoPago.mockResolvedValue(pago)
    const alConfirmar = vi.fn()

    render(<PagoConYapeView plan={PLAN} alConfirmar={alConfirmar} alVolver={() => {}} />)
    await llenarYPagar()

    expect(llamadas.yape).toEqual({ otp: '123456', phoneNumber: '111111111' })
    expect(pagarYapeConMercadoPago).toHaveBeenCalledWith({
      planId: 1,
      token: 'yape_tok',
      celular: '111111111',
      correo: 'comprador@testuser.com',
    })
    expect(alConfirmar).toHaveBeenCalledWith(pago)
  })

  it('valida el celular y el código antes de llamar a Mercado Pago', async () => {
    sdkFalso()
    render(<PagoConYapeView plan={PLAN} alConfirmar={() => {}} alVolver={() => {}} />)

    await llenarYPagar({ celular: '12345', codigo: '12' })

    expect(screen.getByText('El celular tiene 9 dígitos.')).toBeInTheDocument()
    expect(screen.getByText('El código de aprobación tiene 6 dígitos.')).toBeInTheDocument()
    expect(cargarSdkMercadoPago).not.toHaveBeenCalled()
  })

  it('si Mercado Pago rechaza el pago muestra el motivo y limpia el código', async () => {
    sdkFalso()
    pagarYapeConMercadoPago.mockRejectedValue(
      Object.assign(new Error('La tarjeta no tiene fondos suficientes.'), { status: 400, codigo: 'PAGO_RECHAZADO' }),
    )
    const alConfirmar = vi.fn()

    render(<PagoConYapeView plan={PLAN} alConfirmar={alConfirmar} alVolver={() => {}} />)
    await llenarYPagar({ celular: '111111113' })

    expect(await screen.findByText('La tarjeta no tiene fondos suficientes.')).toBeInTheDocument()
    expect(screen.getByLabelText(/Código de aprobación/)).toHaveValue('')
    expect(alConfirmar).not.toHaveBeenCalled()
  })

  it('si Yape no entrega el token avisa y no llama al backend', async () => {
    sdkFalso(null)
    render(<PagoConYapeView plan={PLAN} alConfirmar={() => {}} alVolver={() => {}} />)

    await llenarYPagar()

    expect(await screen.findByText(/Yape no aceptó el celular o el código/)).toBeInTheDocument()
    expect(pagarYapeConMercadoPago).not.toHaveBeenCalled()
  })
})
