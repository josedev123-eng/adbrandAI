// HU-20: historial de tokens por periodo, sin necesitar Django encendido.
import { fireEvent, render, screen, within } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { formatearTokens, obtenerHistorial } from '../models/tokensApi'
import { AVISO_PERIODO, periodoPorDefecto } from '../viewmodels/useHistorialTokens'
import HistorialTokensView from './HistorialTokensView'

vi.mock('../models/tokensApi', async (importOriginal) => ({
  ...(await importOriginal()),
  obtenerHistorial: vi.fn(),
}))

const TRES_MESES = {
  desde: '2026-08',
  hasta: '2026-10',
  total: 1800,
  meses: [
    { mes: '2026-08', tokens: 1500, generaciones: 5 },
    { mes: '2026-09', tokens: 0, generaciones: 0 },
    { mes: '2026-10', tokens: 300, generaciones: 2 },
  ],
}

function elegirPeriodo(desde, hasta) {
  fireEvent.change(screen.getByLabelText('Desde'), { target: { value: desde } })
  fireEvent.change(screen.getByLabelText('Hasta'), { target: { value: hasta } })
  fireEvent.click(screen.getByRole('button', { name: 'Consultar' }))
}

describe('HistorialTokensView (HU-20)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    obtenerHistorial.mockResolvedValue(TRES_MESES)
  })

  it('al abrir consulta los últimos 6 meses', async () => {
    render(<HistorialTokensView />)
    await screen.findByText('Total del periodo')

    const { desde, hasta } = periodoPorDefecto()
    expect(obtenerHistorial).toHaveBeenCalledWith(desde, hasta)
    expect(screen.getByLabelText('Desde')).toHaveValue(desde)
    expect(screen.getByLabelText('Hasta')).toHaveValue(hasta)
  })

  it('el periodo por defecto son 6 meses contando el actual, también al cruzar de año', () => {
    expect(periodoPorDefecto(new Date(2026, 9, 27))).toEqual({ desde: '2026-05', hasta: '2026-10' })
    expect(periodoPorDefecto(new Date(2026, 1, 10))).toEqual({ desde: '2025-09', hasta: '2026-02' })
  })

  it('un periodo válido muestra cada mes y el total (criterio 1)', async () => {
    render(<HistorialTokensView />)
    await screen.findByText('Total del periodo')

    elegirPeriodo('2026-08', '2026-10')

    expect(obtenerHistorial).toHaveBeenLastCalledWith('2026-08', '2026-10')
    await screen.findByText('agosto 2026')
    // En Perú el navegador puede escribir "setiembre" o "septiembre".
    expect(screen.getByText(/^sep?tiembre 2026$/)).toBeInTheDocument()
    const filaAgosto = screen.getByRole('row', { name: /agosto 2026/ })
    expect(within(filaAgosto).getByText(formatearTokens(1500))).toBeInTheDocument()
    const total = screen.getByRole('row', { name: /Total del periodo/ })
    expect(total).toHaveTextContent(formatearTokens(1800))
  })

  it('si el inicio es posterior al fin muestra el aviso y no llama a la API (criterio 2)', async () => {
    render(<HistorialTokensView />)
    await screen.findByText('Total del periodo')
    expect(obtenerHistorial).toHaveBeenCalledTimes(1)

    elegirPeriodo('2026-10', '2026-08')

    expect(screen.getByRole('alert')).toHaveTextContent(AVISO_PERIODO)
    expect(obtenerHistorial).toHaveBeenCalledTimes(1)
  })

  it('al corregir el periodo el aviso desaparece', async () => {
    render(<HistorialTokensView />)
    await screen.findByText('Total del periodo')
    elegirPeriodo('2026-10', '2026-08')

    fireEvent.change(screen.getByLabelText('Desde'), { target: { value: '2026-07' } })

    expect(screen.queryByRole('alert')).toBeNull()
  })

  it('un periodo de un solo mes funciona', async () => {
    render(<HistorialTokensView />)
    await screen.findByText('Total del periodo')
    obtenerHistorial.mockResolvedValue({
      desde: '2026-10',
      hasta: '2026-10',
      total: 300,
      meses: [{ mes: '2026-10', tokens: 300, generaciones: 2 }],
    })

    elegirPeriodo('2026-10', '2026-10')

    await screen.findByText('octubre 2026 a octubre 2026')
    expect(obtenerHistorial).toHaveBeenLastCalledWith('2026-10', '2026-10')
    expect(screen.getAllByRole('row')).toHaveLength(3) // cabecera, octubre y total
  })

  it('muestra el error si la API falla', async () => {
    obtenerHistorial.mockRejectedValue({ status: 500 })
    render(<HistorialTokensView />)

    expect(await screen.findByText(/No se pudo cargar el historial/)).toBeInTheDocument()
  })

  it('si Django rechaza el periodo muestra su mensaje', async () => {
    render(<HistorialTokensView />)
    await screen.findByText('Total del periodo')
    obtenerHistorial.mockRejectedValue({ status: 400, detalles: { hasta: ['El periodo no puede pasar de 24 meses.'] } })

    elegirPeriodo('2024-01', '2026-01')

    expect(await screen.findByText('El periodo no puede pasar de 24 meses.')).toBeInTheDocument()
  })
})
