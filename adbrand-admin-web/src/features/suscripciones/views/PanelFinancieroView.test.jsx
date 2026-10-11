// HU-05: prueba el panel financiero como lo vería el dueño, sin necesitar Django encendido.
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { obtenerSuscripcionesActivas } from '../models/suscripcionesApi'
import PanelFinancieroView from './PanelFinancieroView'

vi.mock('../models/suscripcionesApi', async (importOriginal) => ({
  ...(await importOriginal()),
  obtenerSuscripcionesActivas: vi.fn(),
}))

describe('Panel financiero (HU-05)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('muestra el número de suscripciones activas', async () => {
    obtenerSuscripcionesActivas.mockResolvedValue({ total: 12 })

    render(<PanelFinancieroView />)

    expect(screen.getByText('Suscripciones activas')).toBeInTheDocument()
    expect(await screen.findByText('12')).toBeInTheDocument()
  })

  it('si no hay ninguna activa muestra cero', async () => {
    obtenerSuscripcionesActivas.mockResolvedValue({ total: 0 })

    render(<PanelFinancieroView />)

    expect(await screen.findByText('0')).toBeInTheDocument()
  })

  it('si Django no responde muestra un mensaje claro y no inventa un número', async () => {
    obtenerSuscripcionesActivas.mockRejectedValue(new Error('sin conexión'))

    render(<PanelFinancieroView />)

    expect(await screen.findByText(/No se pudo cargar el panel/)).toBeInTheDocument()
    expect(screen.getByText('—')).toBeInTheDocument()
  })

  it('el botón Actualizar vuelve a pedir el total', async () => {
    obtenerSuscripcionesActivas.mockResolvedValueOnce({ total: 3 }).mockResolvedValueOnce({ total: 4 })
    const usuario = userEvent.setup()

    render(<PanelFinancieroView />)
    expect(await screen.findByText('3')).toBeInTheDocument()

    await usuario.click(screen.getByRole('button', { name: 'Actualizar' }))

    expect(await screen.findByText('4')).toBeInTheDocument()
    expect(obtenerSuscripcionesActivas).toHaveBeenCalledTimes(2)
  })
})
