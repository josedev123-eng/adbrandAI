// "Mis anuncios": el dueño ve el estado de cada anuncio y se entera de lo que decidió el moderador.
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { cancelarPublicacion, eliminarAnuncio, listarMisAnuncios, marcarAvisosVistos, reactivarAnuncio } from '../models/misAnunciosApi'
import MisAnunciosView from './MisAnunciosView'

vi.mock('../models/misAnunciosApi', async (importOriginal) => ({
  ...(await importOriginal()),
  listarMisAnuncios: vi.fn(),
  marcarAvisosVistos: vi.fn(),
  cancelarPublicacion: vi.fn(),
  reactivarAnuncio: vi.fn(),
  eliminarAnuncio: vi.fn(),
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

  it('"Cancelar en AdBrand" pide confirmar, lo pasa a "Cancelados" y desde ahí se puede volver a activar', async () => {
    listarMisAnuncios.mockResolvedValue([ANUNCIOS[2]])
    cancelarPublicacion.mockResolvedValue({ programacionesCanceladas: 2 })
    reactivarAnuncio.mockResolvedValue(null)
    render(<MisAnunciosView />)

    expect(await screen.findByText('Bórralo si ya lo subiste.')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Cancelar en AdBrand' }))
    expect(cancelarPublicacion).not.toHaveBeenCalled()
    await userEvent.click(screen.getByRole('button', { name: 'Sí, cancelar' }))

    expect(cancelarPublicacion).toHaveBeenCalledWith(1)
    // El aviso de abajo solo dice lo que le queda por hacer.
    expect(await screen.findByRole('status')).toHaveTextContent('Bórralo si ya lo subiste.')
    // "Todos" ya no lo muestra; está en "Cancelados".
    expect(screen.queryByRole('article', { name: /Anuncio:/ })).toBeNull()
    await userEvent.click(screen.getByRole('button', { name: 'Cancelados (1)' }))
    expect(screen.getByText('Cancelado')).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Volver a activar' }))
    expect(reactivarAnuncio).toHaveBeenCalledWith(1)
    await userEvent.click(screen.getByRole('button', { name: 'Todos (1)' }))
    expect(await screen.findByRole('button', { name: '📣 Listo para publicar' })).toBeInTheDocument()
  })

  it('"Eliminar" solo aparece en los cancelados, pide confirmar y lo quita de la lista', async () => {
    listarMisAnuncios.mockResolvedValue([{ ...ANUNCIOS[2], cancelado: true }])
    eliminarAnuncio.mockResolvedValue(null)
    render(<MisAnunciosView />)

    await userEvent.click(await screen.findByRole('button', { name: 'Cancelados (1)' }))
    await userEvent.click(screen.getByRole('button', { name: 'Eliminar' }))
    await userEvent.click(screen.getByRole('button', { name: 'Sí, eliminar' }))

    expect(eliminarAnuncio).toHaveBeenCalledWith(1)
    expect(await screen.findByText('Anuncio eliminado de tu lista.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Cancelados (0)' })).toBeInTheDocument()
  })

  it('muestra de a 12 con "Ver más" y busca por texto o por fecha', async () => {
    listarMisAnuncios.mockResolvedValue(
      Array.from({ length: 14 }, (_, i) => ({
        ...ANUNCIOS[1],
        id: 100 + i,
        oferta: i === 0 ? 'Panetón navideño' : `Promo ${i + 1}`,
        titular: i === 0 ? 'Panetón navideño' : `Promo ${i + 1}`,
        fechaCreacion: i === 1 ? '2026-12-01T09:00:00' : '2026-11-20T09:00:00',
      })),
    )
    render(<MisAnunciosView />)

    expect(await screen.findAllByRole('article')).toHaveLength(12)
    await userEvent.click(screen.getByRole('button', { name: 'Ver más (2)' }))
    expect(screen.getAllByRole('article')).toHaveLength(14)

    await userEvent.type(screen.getByLabelText('Buscar'), 'paneton')
    expect(screen.getAllByRole('article')).toHaveLength(1)

    await userEvent.click(screen.getByRole('button', { name: 'Limpiar' }))
    fireEvent.change(screen.getByLabelText('Fecha'), { target: { value: '2026-12-01' } })
    expect(screen.getAllByRole('article')).toHaveLength(1)
  })
})
