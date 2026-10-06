import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook, waitFor, act } from '@testing-library/react'
import { useDetalleContenido } from '../viewmodels/useDetalleContenido'
import { obtenerContenido, moderarContenido } from '../models/moderacionApi'

vi.mock('../models/moderacionApi', () => ({
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
  texto: 'Texto generado por la IA.',
  motivo_revision: 'Motivo de prueba.',
  fecha_creacion: '2024-01-15T10:30:00Z',
  negocio: { nombre: 'Negocio Test', rubro: 'Rubro Test' },
}

describe('useDetalleContenido (HU-15)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.confirm = vi.fn(() => true)
    obtenerContenido.mockResolvedValue(MOCK_CONTENIDO)
  })

  it('carga el contenido y expone aprobar y rechazar', async () => {
    const { result } = renderHook(() => useDetalleContenido(123))

    await waitFor(() => expect(result.current.contenido).toEqual(MOCK_CONTENIDO))

    expect(typeof result.current.aprobar).toBe('function')
    expect(typeof result.current.rechazar).toBe('function')
    expect(result.current.moderando).toBe(false)
  })

  it('aprobar llama a moderarContenido con APROBAR y actualiza el contenido', async () => {
    const aprobado = { ...MOCK_CONTENIDO, estado: 'APROBADO', moderador_id: 42 }
    moderarContenido.mockResolvedValue(aprobado)

    const { result } = renderHook(() => useDetalleContenido(123))

    await waitFor(() => expect(result.current.contenido).toEqual(MOCK_CONTENIDO))

    await act(async () => {
      await result.current.aprobar(42)
    })

    expect(moderarContenido).toHaveBeenCalledWith(123, 'APROBAR', 42)
    expect(result.current.contenido).toEqual(aprobado)
    expect(result.current.moderando).toBe(false)
  })

  it('rechazar llama a moderarContenido con RECHAZAR, el moderador y el motivo', async () => {
    const rechazado = { ...MOCK_CONTENIDO, estado: 'RECHAZADO', moderador_id: 99 }
    moderarContenido.mockResolvedValue(rechazado)

    const { result } = renderHook(() => useDetalleContenido(123))

    await waitFor(() => expect(result.current.contenido).toEqual(MOCK_CONTENIDO))

    await act(async () => {
      await result.current.rechazar(99, 'No cumple las reglas de la IA')
    })

    expect(moderarContenido).toHaveBeenCalledWith(123, 'RECHAZAR', 99, 'No cumple las reglas de la IA')
    expect(result.current.contenido).toEqual(rechazado)
    expect(result.current.moderando).toBe(false)
  })

  it('rechazar sin motivo muestra el error y no llama a la API', async () => {
    const { result } = renderHook(() => useDetalleContenido(123))

    await waitFor(() => expect(result.current.contenido).toEqual(MOCK_CONTENIDO))

    await act(async () => {
      await result.current.rechazar(99)
    })

    expect(result.current.error).toBe('El motivo de rechazo es obligatorio.')
    expect(moderarContenido).not.toHaveBeenCalled()
  })

  it('pone moderando en true mientras aprueba', async () => {
    let resolveModerar
    moderarContenido.mockImplementation(() => new Promise((r) => { resolveModerar = r }))

    const { result } = renderHook(() => useDetalleContenido(123))

    await waitFor(() => expect(result.current.contenido).toEqual(MOCK_CONTENIDO))

    let pendiente
    await act(async () => {
      pendiente = result.current.aprobar(42)
      await Promise.resolve()
    })

    expect(result.current.moderando).toBe(true)

    await act(async () => {
      resolveModerar({ ...MOCK_CONTENIDO, estado: 'APROBADO', moderador_id: 42 })
      await pendiente
    })

    expect(result.current.moderando).toBe(false)
  })

  it('guarda error si la API falla al aprobar', async () => {
    const errorApi = { status: 400, detalles: { detail: 'Solo DUDOSO' } }
    moderarContenido.mockRejectedValue(errorApi)

    const { result } = renderHook(() => useDetalleContenido(123))

    await waitFor(() => expect(result.current.contenido).toEqual(MOCK_CONTENIDO))

    let fallo = null
    await act(async () => {
      try {
        await result.current.aprobar(42)
      } catch (e) {
        fallo = e
      }
    })

    expect(fallo).not.toBeNull()
    expect(result.current.error).toBe('Solo DUDOSO')
  })

  it('maneja error 404 al cargar contenido', async () => {
    obtenerContenido.mockRejectedValue({ status: 404 })

    const { result } = renderHook(() => useDetalleContenido(999))

    await waitFor(() => expect(result.current.error).toBe('Este contenido ya no existe.'))
    expect(result.current.contenido).toBeNull()
  })
})
