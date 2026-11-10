// HU 34: programar una publicación eligiendo un anuncio aprobado, sin días pasados.
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { listarAnuncios } from '../models/anunciosApi'
import { crearPublicacion, obtenerCalendario } from '../models/calendarioApi'
import { listarCampanas } from '../models/campanasApi'
import { esDiaPasado, hoyIso } from '../viewmodels/useCalendario'
import CalendarioView from './CalendarioView'

vi.mock('../models/calendarioApi', () => ({
  obtenerCalendario: vi.fn(),
  crearPublicacion: vi.fn(),
}))

vi.mock('../models/campanasApi', () => ({
  listarCampanas: vi.fn(),
  crearCampana: vi.fn(),
}))

vi.mock('../models/anunciosApi', () => ({
  listarAnuncios: vi.fn(),
}))

const ANUNCIOS = [
  { id: 11, fecha: '2026-11-02T10:00:00', redSocial: 'FACEBOOK', oferta: '2x1 en café los viernes', estado: 'APROBADO' },
  { id: 12, fecha: '2026-11-01T10:00:00', redSocial: 'INSTAGRAM', oferta: 'Anuncio rechazado', estado: 'RECHAZADO' },
  { id: 13, fecha: '2026-10-30T10:00:00', redSocial: 'INSTAGRAM', oferta: 'Anuncio en revisión', estado: 'DUDOSO' },
]

function ayerIso() {
  const ayer = new Date()
  ayer.setDate(ayer.getDate() - 1)
  return `${ayer.getFullYear()}-${String(ayer.getMonth() + 1).padStart(2, '0')}-${String(ayer.getDate()).padStart(2, '0')}`
}

describe('Programar una publicación', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    obtenerCalendario.mockResolvedValue([])
    listarCampanas.mockResolvedValue([])
    listarAnuncios.mockResolvedValue(ANUNCIOS)
  })

  it('la lista solo muestra los anuncios aprobados', async () => {
    render(<CalendarioView />)

    const lista = screen.getByLabelText('Anuncio aprobado')
    await waitFor(() => expect(lista).toHaveTextContent('2x1 en café los viernes'))
    expect(lista).not.toHaveTextContent('Anuncio rechazado')
    expect(lista).not.toHaveTextContent('Anuncio en revisión')
  })

  it('al elegir un anuncio llena el título y la red social, y lo envía con su id', async () => {
    crearPublicacion.mockResolvedValue({ id: 5 })

    render(<CalendarioView />)
    const lista = screen.getByLabelText('Anuncio aprobado')
    await waitFor(() => expect(lista).toHaveTextContent('2x1 en café los viernes'))

    const usuario = userEvent.setup()
    await usuario.selectOptions(lista, '11')

    expect(screen.getByLabelText(/Título/)).toHaveValue('2x1 en café los viernes')
    expect(screen.getByLabelText('Red social')).toHaveValue('FACEBOOK')

    await usuario.click(screen.getByRole('button', { name: /Programar publicación/ }))

    await waitFor(() =>
      expect(crearPublicacion).toHaveBeenCalledWith(
        expect.objectContaining({ contenidoId: 11, titulo: '2x1 en café los viernes', redSocial: 'FACEBOOK' }),
      ),
    )
    expect(await screen.findByText('Publicación programada.')).toBeInTheDocument()
  })

  it('no envía una publicación en un día que ya pasó', async () => {
    render(<CalendarioView />)
    await screen.findByRole('grid')

    const usuario = userEvent.setup()
    await usuario.type(screen.getByLabelText(/Título/), 'Oferta atrasada')
    const fecha = screen.getByLabelText('Fecha')
    await usuario.clear(fecha)
    await usuario.type(fecha, ayerIso())
    await usuario.click(screen.getByRole('button', { name: /Programar publicación/ }))

    expect(await screen.findByText('No puedes programar en una fecha u hora que ya pasó.')).toBeInTheDocument()
    expect(crearPublicacion).not.toHaveBeenCalled()
  })

  it('muestra el mensaje de Spring cuando el anuncio fue rechazado', async () => {
    crearPublicacion.mockRejectedValue(new Error('Este anuncio fue rechazado y no se puede programar.'))

    render(<CalendarioView />)
    await screen.findByRole('grid')

    const usuario = userEvent.setup()
    await usuario.type(screen.getByLabelText(/Título/), 'Oferta')
    await usuario.click(screen.getByRole('button', { name: /Programar publicación/ }))

    expect(await screen.findByText('Este anuncio fue rechazado y no se puede programar.')).toBeInTheDocument()
    expect(screen.queryByText('Publicación programada.')).not.toBeInTheDocument()
  })

  it('esDiaPasado solo marca los días anteriores a hoy', () => {
    expect(esDiaPasado(ayerIso())).toBe(true)
    expect(esDiaPasado(hoyIso())).toBe(false)
    expect(esDiaPasado('2999-01-01')).toBe(false)
  })
})
