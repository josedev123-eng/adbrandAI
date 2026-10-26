// HU 33: el calendario muestra el mes, sus publicaciones y permite programar una nueva.
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { crearPublicacion, obtenerCalendario } from '../models/calendarioApi'
import { etiquetaMes, fechaDeDia, mesActual } from '../viewmodels/useCalendario'
import CalendarioView from './CalendarioView'

vi.mock('../models/calendarioApi', () => ({
  obtenerCalendario: vi.fn(),
  crearPublicacion: vi.fn(),
}))

const MES = mesActual()
const HOY = fechaDeDia(MES, new Date().getDate())

const PUBLICACION = {
  id: 3,
  titulo: 'Promoción de fin de semana',
  redSocial: 'INSTAGRAM',
  fecha: HOY,
  hora: '18:30:00',
  estado: 'PROGRAMADA',
}

describe('CalendarioView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('muestra el mes actual y las publicaciones del día', async () => {
    obtenerCalendario.mockResolvedValue([PUBLICACION])

    render(<CalendarioView />)

    expect(await screen.findByText(/Promoción de fin de semana/)).toBeInTheDocument()
    expect(screen.getByText(etiquetaMes(MES))).toBeInTheDocument()
    expect(screen.getByText(/18:30/)).toBeInTheDocument()
    expect(obtenerCalendario).toHaveBeenCalledWith(MES)
  })

  it('programa una publicación y recarga el calendario', async () => {
    obtenerCalendario.mockResolvedValue([])
    crearPublicacion.mockResolvedValue({ ...PUBLICACION, id: 4, titulo: 'Nuevo anuncio' })

    render(<CalendarioView />)
    await screen.findByText(etiquetaMes(MES))

    const usuario = userEvent.setup()
    await usuario.type(screen.getByLabelText(/Título/), 'Nuevo anuncio')
    await usuario.click(screen.getByRole('button', { name: /Programar publicación/ }))

    await waitFor(() =>
      expect(crearPublicacion).toHaveBeenCalledWith(
        expect.objectContaining({
          titulo: 'Nuevo anuncio',
          redSocial: 'INSTAGRAM',
          fecha: HOY,
          hora: '10:00:00',
        }),
      ),
    )
    expect(await screen.findByText('Publicación programada.')).toBeInTheDocument()
    expect(obtenerCalendario).toHaveBeenCalledTimes(2)
  })

  it('avisa cuando no se pudo cargar el calendario', async () => {
    obtenerCalendario.mockRejectedValue(new Error('sin conexión'))

    render(<CalendarioView />)

    expect(await screen.findByText(/No se pudo cargar el calendario/)).toBeInTheDocument()
  })
})
