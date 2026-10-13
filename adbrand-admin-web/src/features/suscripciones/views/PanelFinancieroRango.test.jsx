// HU-07: prueba el filtro por rango de meses del panel financiero, sin necesitar Django encendido.
import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { obtenerIngresosMensuales, obtenerSuscripcionesActivas } from '../models/suscripcionesApi'
import PanelFinancieroView from './PanelFinancieroView'

vi.mock('../models/suscripcionesApi', async (importOriginal) => ({
  ...(await importOriginal()),
  obtenerSuscripcionesActivas: vi.fn(),
  obtenerIngresosMensuales: vi.fn(),
}))

const ULTIMOS_MESES = [
  { mes: '2026-09', total: '100.00' },
  { mes: '2026-10', total: '200.00' },
]

const PRIMER_TRIMESTRE = [
  { mes: '2025-01', total: '10.00' },
  { mes: '2025-02', total: '20.00' },
  { mes: '2025-03', total: '30.00' },
]

// Dibuja el panel, espera la primera carga y deja listo al usuario.
async function abrirPanel() {
  const usuario = userEvent.setup()
  render(<PanelFinancieroView />)
  await screen.findByText('octubre de 2026')
  return usuario
}

function escribirRango(desde, hasta) {
  fireEvent.change(screen.getByLabelText('Desde'), { target: { value: desde } })
  fireEvent.change(screen.getByLabelText('Hasta'), { target: { value: hasta } })
}

describe('Filtro por rango de meses en el panel financiero (HU-07)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    obtenerSuscripcionesActivas.mockResolvedValue({ total: 4 })
    obtenerIngresosMensuales.mockResolvedValue(ULTIMOS_MESES)
  })

  it('un rango válido actualiza los ingresos por mes y el total del rango', async () => {
    const usuario = await abrirPanel()
    obtenerIngresosMensuales.mockResolvedValue(PRIMER_TRIMESTRE)

    escribirRango('2025-01', '2025-03')
    await usuario.click(screen.getByRole('button', { name: 'Aplicar rango' }))

    expect(await screen.findByText('enero de 2025')).toBeInTheDocument()
    expect(obtenerIngresosMensuales).toHaveBeenLastCalledWith({ desde: '2025-01', hasta: '2025-03' })
    expect(screen.getAllByRole('listitem')).toHaveLength(3)
    expect(screen.getByText('S/ 60.00')).toBeInTheDocument()
    expect(screen.queryByText('octubre de 2026')).not.toBeInTheDocument()
    expect(screen.getByText('Pagos recibidos en el rango elegido.')).toBeInTheDocument()
  })

  it('si el mes inicial es mayor que el final muestra el error y no llama a la API', async () => {
    const usuario = await abrirPanel()

    escribirRango('2025-06', '2025-01')
    await usuario.click(screen.getByRole('button', { name: 'Aplicar rango' }))

    expect(screen.getByText('El mes inicial no puede ser mayor que el final.')).toBeInTheDocument()
    expect(obtenerIngresosMensuales).toHaveBeenCalledTimes(1)
    expect(screen.getByText('octubre de 2026')).toBeInTheDocument()
  })

  it('si falta un mes muestra el error y no llama a la API', async () => {
    const usuario = await abrirPanel()

    fireEvent.change(screen.getByLabelText('Desde'), { target: { value: '2025-01' } })
    await usuario.click(screen.getByRole('button', { name: 'Aplicar rango' }))

    expect(screen.getByText('Elige el mes inicial y el mes final.')).toBeInTheDocument()
    expect(obtenerIngresosMensuales).toHaveBeenCalledTimes(1)
  })

  it('si el rango pasa de 24 meses muestra el error y no llama a la API', async () => {
    const usuario = await abrirPanel()

    escribirRango('2024-01', '2026-01')
    await usuario.click(screen.getByRole('button', { name: 'Aplicar rango' }))

    expect(screen.getByText('El rango no puede pasar de 24 meses.')).toBeInTheDocument()
    expect(obtenerIngresosMensuales).toHaveBeenCalledTimes(1)
  })

  it('al corregir un mes el error desaparece', async () => {
    const usuario = await abrirPanel()

    escribirRango('2025-06', '2025-01')
    await usuario.click(screen.getByRole('button', { name: 'Aplicar rango' }))
    fireEvent.change(screen.getByLabelText('Hasta'), { target: { value: '2025-08' } })

    expect(screen.queryByText('El mes inicial no puede ser mayor que el final.')).not.toBeInTheDocument()
  })

  it('el botón Últimos 6 meses quita el rango y vuelve a pedir el periodo normal', async () => {
    const usuario = await abrirPanel()
    obtenerIngresosMensuales.mockResolvedValueOnce(PRIMER_TRIMESTRE)

    escribirRango('2025-01', '2025-03')
    await usuario.click(screen.getByRole('button', { name: 'Aplicar rango' }))
    await screen.findByText('enero de 2025')

    await usuario.click(screen.getByRole('button', { name: 'Últimos 6 meses' }))

    expect(await screen.findByText('octubre de 2026')).toBeInTheDocument()
    expect(obtenerIngresosMensuales).toHaveBeenLastCalledWith({ desde: '', hasta: '' })
    expect(screen.getByLabelText('Desde')).toHaveValue('')
    expect(screen.getByText('Pagos recibidos en los últimos 6 meses.')).toBeInTheDocument()
  })

  it('si Django rechaza el rango muestra su mensaje', async () => {
    const usuario = await abrirPanel()
    obtenerIngresosMensuales.mockRejectedValue({
      status: 400,
      detalles: { hasta: ['El rango no puede pasar de 24 meses.'] },
    })

    escribirRango('2025-01', '2025-03')
    await usuario.click(screen.getByRole('button', { name: 'Aplicar rango' }))

    expect(await screen.findByText('El rango no puede pasar de 24 meses.')).toBeInTheDocument()
    expect(screen.queryByText(/No se pudo cargar el panel/)).not.toBeInTheDocument()
  })
})
