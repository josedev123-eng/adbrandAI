// HU 26: el formulario valida la tarjeta antes de llamar a la API y avisa cuando el pago se aprueba.
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { pagarConTarjeta } from '../models/pagosApi'
import PagoTarjetaView from './PagoTarjetaView'

vi.mock('../models/pagosApi', () => ({
  pagarConTarjeta: vi.fn(),
}))

const PLAN = { id: 2, nombre: 'Pro', precio: 59.9, limiteTokens: 150000 }

async function llenarTarjeta(numero) {
  const usuario = userEvent.setup()
  await usuario.type(screen.getByLabelText(/Número de la tarjeta/), numero)
  await usuario.type(screen.getByLabelText(/Nombre del titular/), 'Ana Quispe')
  await usuario.type(screen.getByLabelText(/Vencimiento/), '12/30')
  await usuario.type(screen.getByLabelText(/CVV/), '123')
  await usuario.click(screen.getByRole('button', { name: /Pagar/ }))
}

describe('PagoTarjetaView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('envía la tarjeta y entrega el pago aprobado', async () => {
    const alPagar = vi.fn()
    pagarConTarjeta.mockResolvedValue({
      id: 5,
      plan: 'Pro',
      monto: 59.9,
      metodo: 'TARJETA',
      estado: 'APROBADO',
      referencia: 'OP-123',
    })

    render(<PagoTarjetaView plan={PLAN} alPagar={alPagar} alVolver={() => {}} />)
    await llenarTarjeta('4111111111111111')

    await waitFor(() => expect(alPagar).toHaveBeenCalledTimes(1))
    expect(pagarConTarjeta).toHaveBeenCalledWith(
      expect.objectContaining({ planId: 2, numeroTarjeta: '4111111111111111', cvv: '123' }),
    )
  })

  it('no llama a la API cuando el número no pasa el algoritmo de Luhn', async () => {
    render(<PagoTarjetaView plan={PLAN} alPagar={vi.fn()} alVolver={() => {}} />)
    await llenarTarjeta('4111111111111112')

    expect(await screen.findByText('El número de la tarjeta no es válido.')).toBeInTheDocument()
    expect(pagarConTarjeta).not.toHaveBeenCalled()
  })

  it('muestra el mensaje del servidor cuando el banco rechaza la tarjeta', async () => {
    const error = new Error('La tarjeta fue rechazada por el banco. Usa otra tarjeta.')
    error.status = 400
    pagarConTarjeta.mockRejectedValue(error)

    render(<PagoTarjetaView plan={PLAN} alPagar={vi.fn()} alVolver={() => {}} />)
    await llenarTarjeta('4000000000000002')

    expect(await screen.findByText(/rechazada por el banco/)).toBeInTheDocument()
  })
})
