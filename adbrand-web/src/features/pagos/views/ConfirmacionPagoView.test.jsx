// HU 28: la confirmación muestra el comprobante generado y avisa si el correo salió.
import { render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { obtenerComprobante } from '../models/pagosApi'
import ConfirmacionPagoView from './ConfirmacionPagoView'

vi.mock('../models/pagosApi', () => ({
  obtenerComprobante: vi.fn(),
}))

const COMPROBANTE = {
  numeroComprobante: 'ADB-000005',
  pagoId: 5,
  cliente: 'Dulce Perú',
  correo: 'ana@dulceperu.pe',
  plan: 'Pro',
  monto: 59.9,
  metodo: 'TARJETA',
  referencia: 'OP-ABC123',
  fechaPago: '2026-10-25',
  correoEnviado: true,
}

describe('ConfirmacionPagoView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('muestra el número, el cliente y el total del comprobante', async () => {
    obtenerComprobante.mockResolvedValue(COMPROBANTE)

    render(<ConfirmacionPagoView pago={{ id: 5 }} alVolver={() => {}} />)

    expect(await screen.findByText('ADB-000005')).toBeInTheDocument()
    expect(screen.getByText('Dulce Perú')).toBeInTheDocument()
    expect(screen.getByText('Pro')).toBeInTheDocument()
    expect(screen.getByText(/59[.,]90/)).toBeInTheDocument()
    expect(screen.getByText(/Enviamos tu comprobante a ana@dulceperu\.pe/)).toBeInTheDocument()
    expect(obtenerComprobante).toHaveBeenCalledWith(5)
  })

  it('avisa cuando no se pudo cargar el comprobante', async () => {
    obtenerComprobante.mockRejectedValue(new Error('sin conexión'))

    render(<ConfirmacionPagoView pago={{ id: 5 }} alVolver={() => {}} />)

    expect(await screen.findByText(/No se pudo cargar el comprobante/)).toBeInTheDocument()
  })
})
