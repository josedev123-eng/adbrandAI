import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import DetalleContenidoView from './DetalleContenidoView'
import { obtenerContenido, moderarContenido } from '../models/moderacionApi'

vi.mock('../models/moderacionApi', async (importOriginal) => ({
  ...(await importOriginal()),
  obtenerContenido: vi.fn(),
  moderarContenido: vi.fn(),
}))

const MOCK_CONTENIDO = {
  id: 123,
  tipo: 'ANUNCIO',
  estado: 'DUDOSO',
  red_social: 'INSTAGRAM',
  tono: 'CERCANO',
  oferta: 'Promo de prueba',
  texto: 'Texto generado por la IA para probar.',
  motivo_revision: '"palabra": Motivo de prueba.',
  fecha_creacion: '2024-01-15T10:30:00Z',
  negocio: { nombre: 'Negocio Test', rubro: 'Rubro Test' },
}

describe('DetalleContenidoView (HU-15)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.confirm = vi.fn(() => true)
    obtenerContenido.mockResolvedValue(MOCK_CONTENIDO)
    moderarContenido.mockResolvedValue({ ...MOCK_CONTENIDO, estado: 'APROBADO', moderador_id: 42 })
  })

  it('muestra el formulario de moderación cuando el estado es DUDOSO', async () => {
    render(<DetalleContenidoView id={123} alVolver={vi.fn()} />)

    expect(await screen.findByRole('button', { name: '✓ Aprobar' })).toBeInTheDocument()
    expect(screen.queryByText('Tu ID de administrador:')).toBeNull()
    expect(screen.getByRole('button', { name: '✗ Rechazar' })).toBeInTheDocument()
  })

  it('no muestra formulario si el contenido ya está APROBADO', async () => {
    obtenerContenido.mockResolvedValue({ ...MOCK_CONTENIDO, estado: 'APROBADO' })

    const { rerender } = render(<DetalleContenidoView id={123} alVolver={vi.fn()} />)

    await waitFor(() => expect(screen.getByText('Aprobado')).toBeInTheDocument())

    rerender(<DetalleContenidoView id={123} alVolver={vi.fn()} />)

    expect(screen.queryByRole('button', { name: '✓ Aprobar' })).not.toBeInTheDocument()
  })

  it('no muestra formulario si el contenido ya está RECHAZADO', async () => {
    obtenerContenido.mockResolvedValue({ ...MOCK_CONTENIDO, estado: 'RECHAZADO' })

    const { rerender } = render(<DetalleContenidoView id={123} alVolver={vi.fn()} />)

    await waitFor(() => expect(screen.getByText('Rechazado')).toBeInTheDocument())

    rerender(<DetalleContenidoView id={123} alVolver={vi.fn()} />)

    expect(screen.queryByRole('button', { name: '✓ Aprobar' })).not.toBeInTheDocument()
  })

  it('llama a moderarContenido con APROBAR (el moderador sale de la sesión)', async () => {
    render(<DetalleContenidoView id={123} alVolver={vi.fn()} />)

    fireEvent.click(await screen.findByRole('button', { name: '✓ Aprobar' }))

    await waitFor(() => {
      expect(moderarContenido).toHaveBeenCalledWith(123, 'APROBAR')
    })
  })

  it('llama a moderarContenido con RECHAZAR y el motivo al confirmar el rechazo', async () => {
    render(<DetalleContenidoView id={123} alVolver={vi.fn()} />)

    fireEvent.click(await screen.findByRole('button', { name: '✗ Rechazar' }))
    fireEvent.change(screen.getByPlaceholderText('Explica por qué se rechaza el contenido...'), {
      target: { value: 'Publicidad engañosa' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar rechazo' }))

    await waitFor(() => {
      expect(moderarContenido).toHaveBeenCalledWith(123, 'RECHAZAR', 'Publicidad engañosa')
    })
  })

  it('ya no pide el ID del moderador y los botones están habilitados', async () => {
    render(<DetalleContenidoView id={123} alVolver={vi.fn()} />)

    await screen.findByRole('button', { name: '✓ Aprobar' })

    expect(screen.queryByText('Tu ID de administrador:')).toBeNull()
    expect(screen.getByRole('button', { name: '✓ Aprobar' })).not.toBeDisabled()
    expect(screen.getByRole('button', { name: '✗ Rechazar' })).not.toBeDisabled()
  })

  it('deshabilita botones y muestra estado de carga mientras modera', async () => {
    let resolveModerar
    moderarContenido.mockImplementation(() => new Promise((r) => { resolveModerar = r }))

    render(<DetalleContenidoView id={123} alVolver={vi.fn()} />)

    fireEvent.click(await screen.findByRole('button', { name: '✓ Aprobar' }))

    expect(screen.getByRole('button', { name: 'Aprobando...' })).toBeDisabled()
    expect(screen.getByRole('button', { name: '✗ Rechazar' })).toBeDisabled()

    resolveModerar({ ...MOCK_CONTENIDO, estado: 'APROBADO', moderador_id: 42 })
    await waitFor(() => expect(screen.getByText('Aprobado')).toBeInTheDocument())
  })

  it('muestra error si la API falla al aprobar', async () => {
    moderarContenido.mockRejectedValue({
      status: 400,
      detalles: { detail: 'Solo se pueden moderar contenidos en estado DUDOSO' },
    })

    render(<DetalleContenidoView id={123} alVolver={vi.fn()} />)

    fireEvent.click(await screen.findByRole('button', { name: '✓ Aprobar' }))

    await waitFor(() => {
      expect(screen.getByText('Solo se pueden moderar contenidos en estado DUDOSO')).toBeInTheDocument()
    })
  })
})
