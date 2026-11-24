// "Mis anuncios": el dueño ve el estado de cada anuncio y se entera de lo que decidió el moderador.
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { listarMisAnuncios, marcarAvisosVistos } from '../models/misAnunciosApi'
import MisAnunciosView from './MisAnunciosView'

vi.mock('../models/misAnunciosApi', async (importOriginal) => ({
  ...(await importOriginal()),
  listarMisAnuncios: vi.fn(),
  marcarAvisosVistos: vi.fn(),
}))

const ANUNCIOS = [
  {
    id: 3,
    oferta: 'Cigarrillos importados',
    texto: 'Oferta especial',
    redSocial: 'INSTAGRAM',
    estado: 'RECHAZADO',
    motivo: 'Publicidad de tabaco.',
    moderado: true,
    avisoPendiente: true,
    fechaCreacion: '2026-11-20T10:00:00',
  },
  { id: 2, oferta: 'Pan de yema', texto: 'Recién horneado', redSocial: 'FACEBOOK', estado: 'DUDOSO', moderado: false, avisoPendiente: false },
  { id: 1, oferta: '2x1 en café', texto: 'Ven con un amigo', redSocial: 'INSTAGRAM', estado: 'APROBADO', moderado: false, avisoPendiente: false },
]

describe('MisAnunciosView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    listarMisAnuncios.mockResolvedValue(ANUNCIOS)
    marcarAvisosVistos.mockResolvedValue(null)
  })

  it('muestra cada anuncio con su estado y el motivo del rechazo del moderador', async () => {
    render(<MisAnunciosView />)

    const rechazado = await screen.findByRole('article', { name: 'Anuncio: Cigarrillos importados' })
    expect(within(rechazado).getByText('Rechazado')).toBeInTheDocument()
    expect(within(rechazado).getByText('Nuevo')).toBeInTheDocument()
    expect(within(rechazado).getByText(/El moderador no lo aprobó/)).toBeInTheDocument()
    expect(within(rechazado).getByText(/Motivo: Publicidad de tabaco\./)).toBeInTheDocument()
    expect(screen.getByText(/Un moderador lo está revisando/)).toBeInTheDocument()
  })

  it('al abrir la pantalla marca las decisiones como vistas y avisa al menú', async () => {
    const alVerAvisos = vi.fn()
    render(<MisAnunciosView alVerAvisos={alVerAvisos} />)

    await waitFor(() => expect(marcarAvisosVistos).toHaveBeenCalled())
    await waitFor(() => expect(alVerAvisos).toHaveBeenCalled())
  })

  it('sin decisiones nuevas no llama a marcarlas como vistas', async () => {
    listarMisAnuncios.mockResolvedValue(ANUNCIOS.slice(1))
    render(<MisAnunciosView />)

    await screen.findByRole('article', { name: 'Anuncio: Pan de yema' })
    expect(marcarAvisosVistos).not.toHaveBeenCalled()
  })

  it('filtra por estado', async () => {
    render(<MisAnunciosView />)
    await screen.findByRole('article', { name: 'Anuncio: Cigarrillos importados' })

    await userEvent.click(screen.getByRole('button', { name: 'Aprobados (1)' }))

    expect(screen.getAllByRole('article')).toHaveLength(1)
    expect(screen.getByRole('article', { name: 'Anuncio: 2x1 en café' })).toBeInTheDocument()
  })

  it('sin anuncios invita a crear el primero', async () => {
    listarMisAnuncios.mockResolvedValue([])
    const irACrear = vi.fn()
    render(<MisAnunciosView irACrear={irACrear} />)

    await userEvent.click(await screen.findByRole('button', { name: 'Crear mi primer anuncio' }))
    expect(irACrear).toHaveBeenCalled()
  })
})
