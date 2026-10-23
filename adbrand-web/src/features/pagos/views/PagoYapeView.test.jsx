// HU 27: la pantalla genera el QR de Yape y confirma la operación cuando el usuario ya pagó.
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { confirmarYape, registrarYape } from '../models/pagosApi'
import PagoYapeView from './PagoYapeView'

vi.mock('../models/pagosApi', () => ({
  registrarYape: vi.fn(),
  confirmarYape: vi.fn(),
}))

vi.mock('qrcode', () => ({
  default: {
    toDataURL: vi.fn(() => Promise.resolve('data:image/png;base64,qr-simulado')),
  },
}))

const PLAN = { id: 2, nombre: 'Pro', precio: 59.9, limiteTokens: 150000 }

const OPERACION = {
  id: 7,
  codigo: 'YPE-ABC12345',
  plan: 'Pro',
  monto: 59.9,
  estado: 'PENDIENTE',
  celular: null,
  contenidoQr: 'yape://pagar?codigo=YPE-ABC12345&monto=59.90&moneda=PEN',
}

describe('PagoYapeView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('genera el QR, muestra el código y confirma el pago', async () => {
    const alConfirmar = vi.fn()
    registrarYape.mockResolvedValue(OPERACION)
    confirmarYape.mockResolvedValue({
      id: 5,
      plan: 'Pro',
      monto: 59.9,
      metodo: 'YAPE',
      estado: 'APROBADO',
      referencia: 'YPE-ABC12345',
    })

    render(<PagoYapeView plan={PLAN} alConfirmar={alConfirmar} alVolver={() => {}} />)
    const usuario = userEvent.setup()

    await usuario.click(screen.getByRole('button', { name: /Generar QR de Yape/ }))

    expect(await screen.findByText('YPE-ABC12345')).toBeInTheDocument()
    expect(screen.getByAltText(/Código QR de Yape/)).toBeInTheDocument()
    expect(registrarYape).toHaveBeenCalledWith({ planId: 2, celular: null })

    await usuario.click(screen.getByRole('button', { name: /Ya pagué, confirmar/ }))

    await waitFor(() => expect(alConfirmar).toHaveBeenCalledTimes(1))
    expect(confirmarYape).toHaveBeenCalledWith(7)
  })

  it('no pide el QR cuando el celular no empieza con 9', async () => {
    render(<PagoYapeView plan={PLAN} alConfirmar={vi.fn()} alVolver={() => {}} />)
    const usuario = userEvent.setup()

    await usuario.type(screen.getByLabelText(/Celular de Yape/), '123456789')
    await usuario.click(screen.getByRole('button', { name: /Generar QR de Yape/ }))

    expect(await screen.findByText(/debe tener 9 dígitos/)).toBeInTheDocument()
    expect(registrarYape).not.toHaveBeenCalled()
  })

  it('muestra el mensaje del servidor si la operación ya fue confirmada', async () => {
    registrarYape.mockResolvedValue(OPERACION)
    confirmarYape.mockRejectedValue(new Error('Esta operación de Yape ya fue confirmada.'))

    render(<PagoYapeView plan={PLAN} alConfirmar={vi.fn()} alVolver={() => {}} />)
    const usuario = userEvent.setup()

    await usuario.click(screen.getByRole('button', { name: /Generar QR de Yape/ }))
    await screen.findByText('YPE-ABC12345')
    await usuario.click(screen.getByRole('button', { name: /Ya pagué, confirmar/ }))

    expect(await screen.findByText(/ya fue confirmada/)).toBeInTheDocument()
  })
})
