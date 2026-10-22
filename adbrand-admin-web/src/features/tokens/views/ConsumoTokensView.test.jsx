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

function consumoCon(total, empresas = [], alertas = []) {
  return { mes: '2026-10', total, empresas, alertas }
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

  // HU-19
  const EN_ALERTA = { ...ROSA, tokens: 85000, limite: 100000, porcentaje_aviso: 80, porcentaje: 85, alerta: true }
  const SIN_ALERTA = { ...CAFE, tokens: 79000, limite: 100000, porcentaje_aviso: 80, porcentaje: 79, alerta: false }
  const ALERTA_ROSA = { usuario_id: 1, empresa: 'Panadería Doña Rosa', tokens: 85000, limite: 100000, porcentaje: 85 }

  it('muestra la alerta con la empresa y el porcentaje consumido (HU-19, criterio 1)', async () => {
    obtenerConsumoActual.mockResolvedValue(consumoCon(164000, [EN_ALERTA, SIN_ALERTA], [ALERTA_ROSA]))
    render(<ConsumoTokensView />)

    const alerta = await screen.findByRole('alert')
    expect(alerta).toHaveTextContent('Panadería Doña Rosa consumió el 85% de su límite mensual')
    expect(alerta).toHaveTextContent(`${formatearTokens(85000)} de ${formatearTokens(100000)} tokens`)
    expect(alerta).not.toHaveTextContent('Empresa #2')
    expect(alerta).not.toHaveClass('consumo-alertas-excedida')
  })

  it('sin empresas en alerta no muestra ningún aviso (HU-19, criterio 2)', async () => {
    obtenerConsumoActual.mockResolvedValue(consumoCon(79000, [SIN_ALERTA], []))
    render(<ConsumoTokensView />)

    await screen.findByText('Empresa #2')
    expect(screen.queryByRole('alert')).toBeNull()
  })

  it('si una empresa pasó el 100 % la alerta se pinta como error', async () => {
    const pasada = { ...ALERTA_ROSA, tokens: 120000, porcentaje: 120 }
    obtenerConsumoActual.mockResolvedValue(consumoCon(120000, [{ ...EN_ALERTA, tokens: 120000, porcentaje: 120 }], [pasada]))
    render(<ConsumoTokensView />)

    expect(await screen.findByRole('alert')).toHaveClass('consumo-alertas-excedida')
  })

  it('la tabla muestra el % del límite y resalta la fila en alerta', async () => {
    const sinLimite = { ...CAFE, usuario_id: 3, empresa: 'Empresa #3', limite: null, porcentaje: null, alerta: false }
    obtenerConsumoActual.mockResolvedValue(consumoCon(164300, [EN_ALERTA, SIN_ALERTA, sinLimite], [ALERTA_ROSA]))
    render(<ConsumoTokensView />)

    await screen.findByRole('columnheader', { name: '% del límite' })
    const filaRosa = screen.getByRole('row', { name: /Panadería Doña Rosa/ })
    expect(filaRosa).toHaveTextContent('85%')
    expect(filaRosa).toHaveClass('consumo-fila-alerta')
    expect(screen.getByRole('row', { name: /Empresa #2/ })).toHaveTextContent('79%')
    expect(screen.getByRole('row', { name: /Empresa #2/ })).not.toHaveClass('consumo-fila-alerta')
    expect(screen.getByRole('row', { name: /Empresa #3/ })).toHaveTextContent('—')
  })
})
