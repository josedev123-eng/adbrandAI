// HU-17: panel de consumo de tokens por empresa, sin necesitar Django encendido.
import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { formatearTokens, obtenerConsumoActual } from '../models/tokensApi'
import { CADA_CUANTO_MS } from '../viewmodels/useConsumoTokens'
import ConsumoTokensView from './ConsumoTokensView'

vi.mock('../models/tokensApi', async (importOriginal) => ({
  ...(await importOriginal()),
  obtenerConsumoActual: vi.fn(),
}))

function consumoCon(total, empresas = []) {
  return { mes: '2026-10', total, empresas }
}

const ROSA = {
  usuario_id: 1,
  empresa: 'Panadería Doña Rosa',
  tokens: 1500,
  generaciones: 4,
  ultima_generacion: '2026-10-27T10:30:00-05:00',
}
const CAFE = { usuario_id: 2, empresa: 'Empresa #2', tokens: 300, generaciones: 1, ultima_generacion: '2026-10-26T09:00:00-05:00' }

// Deja que terminen las promesas pendientes (la respuesta simulada de la API).
async function esperarRespuesta() {
  await act(async () => {})
}

describe('ConsumoTokensView (HU-17)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('muestra el total del mes y los tokens de cada empresa', async () => {
    obtenerConsumoActual.mockResolvedValue(consumoCon(1800, [ROSA, CAFE]))
    render(<ConsumoTokensView />)

    expect(await screen.findByText('Panadería Doña Rosa')).toBeInTheDocument()
    expect(screen.getByText('Empresa #2')).toBeInTheDocument()
    expect(screen.getByText(formatearTokens(1800))).toBeInTheDocument()
    expect(screen.getByText(formatearTokens(1500))).toBeInTheDocument()
    expect(screen.getByText(/octubre de 2026/)).toBeInTheDocument()
  })

  it('avisa cuando ninguna empresa usó la IA este mes', async () => {
    obtenerConsumoActual.mockResolvedValue(consumoCon(0))
    render(<ConsumoTokensView />)

    expect(await screen.findByText('Ninguna empresa ha usado la IA este mes.')).toBeInTheDocument()
  })

  it('muestra un error si la API falla', async () => {
    obtenerConsumoActual.mockRejectedValue({ status: 500 })
    render(<ConsumoTokensView />)

    expect(await screen.findByText(/No se pudo cargar el consumo de IA/)).toBeInTheDocument()
  })

  it('el botón Actualizar vuelve a pedir el consumo', async () => {
    obtenerConsumoActual.mockResolvedValueOnce(consumoCon(100, [{ ...ROSA, tokens: 100 }]))
    obtenerConsumoActual.mockResolvedValueOnce(consumoCon(250, [{ ...ROSA, tokens: 250 }]))
    render(<ConsumoTokensView />)
    await screen.findByText('Panadería Doña Rosa')

    await userEvent.click(screen.getByRole('button', { name: 'Actualizar' }))

    expect(await screen.findAllByText(formatearTokens(250))).not.toHaveLength(0)
    expect(obtenerConsumoActual).toHaveBeenCalledTimes(2)
  })

  it('se vuelve a pedir solo cada 30 segundos (criterio 2)', async () => {
    vi.useFakeTimers()
    obtenerConsumoActual.mockResolvedValueOnce(consumoCon(100, [{ ...ROSA, tokens: 100 }]))
    obtenerConsumoActual.mockResolvedValueOnce(consumoCon(700, [{ ...ROSA, tokens: 700 }]))
    render(<ConsumoTokensView />)
    await esperarRespuesta()
    expect(obtenerConsumoActual).toHaveBeenCalledTimes(1)

    await act(async () => {
      await vi.advanceTimersByTimeAsync(CADA_CUANTO_MS)
    })

    expect(obtenerConsumoActual).toHaveBeenCalledTimes(2)
    expect(screen.getAllByText(formatearTokens(700))).not.toHaveLength(0)
  })

  it('al salir de la pantalla deja de pedir el consumo', async () => {
    vi.useFakeTimers()
    obtenerConsumoActual.mockResolvedValue(consumoCon(0))
    const { unmount } = render(<ConsumoTokensView />)
    await esperarRespuesta()

    unmount()
    await act(async () => {
      await vi.advanceTimersByTimeAsync(CADA_CUANTO_MS * 3)
    })

    expect(obtenerConsumoActual).toHaveBeenCalledTimes(1)
  })
})
