// HU 35: campañas con su color en el calendario, filtro por campaña y formulario para crear campañas.
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { crearPublicacion, obtenerCalendario } from '../models/calendarioApi'
import { crearCampana, listarCampanas } from '../models/campanasApi'
import { fechaDeDia, mesActual, validarCampana } from '../viewmodels/useCalendario'
import CalendarioView from './CalendarioView'

vi.mock('../models/calendarioApi', () => ({
  obtenerCalendario: vi.fn(),
  crearPublicacion: vi.fn(),
}))

vi.mock('../models/campanasApi', () => ({
  listarCampanas: vi.fn(),
  crearCampana: vi.fn(),
}))

const MES = mesActual()
const HOY = fechaDeDia(MES, new Date().getDate())

const NAVIDAD = {
  id: 7,
  nombre: 'Navidad 2026',
  color: '#C62828',
  tipo: 'CAMPANA',
  fechaInicio: '2026-12-01',
  fechaFin: '2026-12-25',
}

const DE_CAMPANA = {
  id: 1,
  titulo: 'Oferta navideña',
  redSocial: 'INSTAGRAM',
  fecha: HOY,
  hora: '18:00:00',
  estado: 'PROGRAMADA',
  campanaId: 7,
  campanaNombre: 'Navidad 2026',
  campanaColor: '#C62828',
}

const SIN_CAMPANA = {
  id: 2,
  titulo: 'Post normal',
  redSocial: 'FACEBOOK',
  fecha: HOY,
  hora: '10:00:00',
  estado: 'PROGRAMADA',
  campanaId: null,
  campanaNombre: null,
  campanaColor: null,
}

describe('Campañas en el calendario', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    listarCampanas.mockResolvedValue([NAVIDAD])
  })

  it('pinta la publicación con el color de su campaña y muestra la leyenda', async () => {
    obtenerCalendario.mockResolvedValue([DE_CAMPANA, SIN_CAMPANA])

    render(<CalendarioView />)

    const conCampana = (await screen.findByText(/Oferta navideña/)).closest('.calendario-chip')
    expect(conCampana).toHaveClass('calendario-chip-campana')
    expect(conCampana.style.getPropertyValue('--color-campana')).toBe('#C62828')

    const sinCampana = screen.getByText(/Post normal/).closest('.calendario-chip')
    expect(sinCampana).toHaveClass('calendario-chip-facebook')

    expect(screen.getByRole('list', { name: 'Campañas' })).toHaveTextContent('Navidad 2026')
  })

  it('al elegir una campaña pide el calendario solo de esa campaña', async () => {
    obtenerCalendario.mockResolvedValue([DE_CAMPANA, SIN_CAMPANA])

    render(<CalendarioView />)
    await screen.findByText(/Post normal/)

    obtenerCalendario.mockResolvedValue([DE_CAMPANA])
    const usuario = userEvent.setup()
    await usuario.selectOptions(screen.getByLabelText('Ver campaña'), '7')

    await waitFor(() => expect(obtenerCalendario).toHaveBeenLastCalledWith(MES, '7'))
    await waitFor(() => expect(screen.queryByText(/Post normal/)).not.toBeInTheDocument())
    expect(screen.getByText(/Oferta navideña/)).toBeInTheDocument()
  })

  it('programa una publicación dentro de una campaña', async () => {
    obtenerCalendario.mockResolvedValue([])
    crearPublicacion.mockResolvedValue(DE_CAMPANA)

    render(<CalendarioView />)
    // Espera a que la campaña aparezca en la lista del formulario.
    await screen.findByLabelText('Ver campaña')

    const usuario = userEvent.setup()
    await usuario.type(screen.getByLabelText(/Título/), 'Oferta navideña')
    await usuario.selectOptions(screen.getByLabelText('Campaña (opcional)'), '7')
    await usuario.click(screen.getByRole('button', { name: /Programar publicación/ }))

    await waitFor(() =>
      expect(crearPublicacion).toHaveBeenCalledWith(expect.objectContaining({ titulo: 'Oferta navideña', campanaId: 7 })),
    )
  })

  it('crea una campaña y la agrega a la leyenda', async () => {
    obtenerCalendario.mockResolvedValue([])
    listarCampanas.mockResolvedValue([])
    crearCampana.mockResolvedValue(NAVIDAD)

    render(<CalendarioView />)
    await screen.findByRole('grid')

    const usuario = userEvent.setup()
    await usuario.type(screen.getByLabelText('Nombre'), 'Navidad 2026')
    await usuario.click(screen.getByRole('button', { name: 'Crear campaña' }))

    await waitFor(() =>
      expect(crearCampana).toHaveBeenCalledWith(expect.objectContaining({ nombre: 'Navidad 2026', tipo: 'CAMPANA' })),
    )
    expect(await screen.findByText('Campaña creada.')).toBeInTheDocument()
    expect(screen.getByRole('list', { name: 'Campañas' })).toHaveTextContent('Navidad 2026')
  })

  it('no envía una campaña sin nombre y muestra el aviso en el campo', async () => {
    obtenerCalendario.mockResolvedValue([])

    render(<CalendarioView />)
    await screen.findByRole('grid')

    const usuario = userEvent.setup()
    await usuario.click(screen.getByRole('button', { name: 'Crear campaña' }))

    expect(await screen.findByText('Escribe el nombre de la campaña.')).toBeInTheDocument()
    expect(crearCampana).not.toHaveBeenCalled()
  })

  it('validarCampana revisa el color y el orden de las fechas', () => {
    const base = { nombre: 'Navidad', color: '#C62828', fechaInicio: '2026-12-01', fechaFin: '2026-12-25' }

    expect(validarCampana(base)).toEqual({})
    expect(validarCampana({ ...base, color: 'rojo' })).toEqual({ color: 'Escribe el color como #RRGGBB.' })
    expect(validarCampana({ ...base, fechaFin: '2026-11-30' })).toEqual({
      fechaFin: 'La fecha de fin no puede ser anterior a la fecha de inicio.',
    })
  })
})
