import { describe, it, expect, vi, beforeEach, act } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
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
  })

  it('carga el contenido y expone aprobar y rechazar', async () => {
    obtenerContenido.mockResolvedValue(MOCK_CONTENIDO)
    moderarContenido.mockResolvedValue({ ...MOCK_CONTENIDO, estado: 'APROBADO', moderador_id: 42 })

    const { result } = renderHook(() => useDetalleContenido(123))

    await waitFor(() => expect(result.current.contenido).toEqual(MOCK_CONTENIDO))

    expect(typeof result.current.aprobar).toBe('function')
    expect(typeof result.current.rechazar).toBe('function')
    expect(result.current.moderando).toBe(false)
  })

  it('aprobar llama a moderarContenido con APROBAR y actualiza el contenido', async () => {
    obtenerContenido.mockResolvedValue(MOCK_CONTENIDO)
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

  it('rechazar llama a moderarContenido con RECHAZAR y actualiza el contenido', async () => {
    obtenerContenido.mockResolvedValue(MOCK_CONTENIDO)
    const rechazado = { ...MOCK_CONTENIDO, estado: 'RECHAZADO', moderador_id: 99 }
    moderarContenido.mockResolvedValue(rechazado)

    const { result } = renderHook(() => useDetalleContenido(123))

    await waitFor(() => expect(result.current.contenido).toEqual(MOCK_CONTENIDO))

    await act(async () => {
      await result.current.rechazar(99)
    })

    expect(moderarContenido).toHaveBeenCalledWith(123, 'RECHAZAR', 99)
    expect(result.current.contenido).toEqual(rechazado)
    expect(result.current.moderando).toBe(false)
  })

  it('pone moderando en true mientras aprueba', async () => {
    obtenerContenido.mockResolvedValue(MOCK_CONTENIDO)
    let resolveModerar
    moderarContenido.mockImplementation(() => new Promise((r) => { resolveModerar = r }))

    const { result } = renderHook(() => useDetalleContenido(123))

    await waitFor(() => expect(result.current.contenido).toEqual(MOCK_CONTENIDO))

    const promesa = act(async () => {
      await result.current.aprobar(42)
    })

    await waitFor(() => expect(result.current.moderando).toBe(true))

    resolveModerar({ ...MOCK_CONTENIDO, estado: 'APROBADO', moderador_id: 42 })
    await promesa
    await waitFor(() => expect(result.current.moderando).toBe(false))
  })

  it('guarda error si la API falla al aprobar', async () => {
    obtenerContenido.mockResolvedValue(MOCK_CONTENIDO)
    const errorApi = { status: 400, detalles: { detail: 'Solo DUDOSO' } }
    moderarContenido.mockRejectedValue(errorApi)

    const { result } = renderHook(() => useDetalleContenido(123))

    await waitFor(() => expect(result.current.contenido).toEqual(MOCK_CONTENIDO))

    await act(async () => {
      try {
        await result.current.aprobar(42)
      } catch (_) {}
    })

    expect(result.current.error).toBe('Solo DUDOSO')
  })

  it('maneja error 404 al cargar contenido', async () => {
    obtenerContenido.mockRejectedValue({ status: 404 })

    const { result } = renderHook(() => useDetalleContenido(999))

    await waitFor(() => expect(result.current.error).toBe('Este contenido ya no existe.'))
    expect(result.current.contenido).toBeNull()
  })
})