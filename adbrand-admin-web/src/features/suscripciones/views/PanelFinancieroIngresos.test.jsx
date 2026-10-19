// HU-06: prueba los ingresos por mes del panel financiero, sin necesitar Django encendido.
import { render, screen, within } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { obtenerIngresosMensuales, obtenerSuscripcionesActivas } from '../models/suscripcionesApi'
import PanelFinancieroView from './PanelFinancieroView'

vi.mock('../models/suscripcionesApi', async (importOriginal) => ({
  ...(await importOriginal()),
  obtenerSuscripcionesActivas: vi.fn(),
  obtenerIngresosMensuales: vi.fn(),
}))

const INGRESOS = [
  { mes: '2026-07', total: '100.00' },
  { mes: '2026-08', total: '0.00' },
  { mes: '2026-09', total: '200.00' },
]

// Busca la fila de un mes por su nombre y la devuelve para mirar dentro.
function filaDelMes(nombre) {
  return screen.getByText(nombre).closest('li')
}

describe('Ingresos por mes en el panel financiero (HU-06)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    obtenerSuscripcionesActivas.mockResolvedValue({ total: 4 })
    obtenerIngresosMensuales.mockResolvedValue(INGRESOS)
  })

  it('muestra cada mes con lo cobrado', async () => {
    render(<PanelFinancieroView />)

    expect(await screen.findByText('julio de 2026')).toBeInTheDocument()
    expect(within(filaDelMes('julio de 2026')).getByText('S/ 100.00')).toBeInTheDocument()
    expect(within(filaDelMes('setiembre de 2026')).getByText('S/ 200.00')).toBeInTheDocument()
  })

  it('un mes sin pagos aparece con S/ 0.00', async () => {
    render(<PanelFinancieroView />)

    await screen.findByText('agosto de 2026')

    expect(within(filaDelMes('agosto de 2026')).getByText('S/ 0.00')).toBeInTheDocument()
  })

  it('respeta el orden de los meses que manda la API', async () => {
    render(<PanelFinancieroView />)

    await screen.findByText('julio de 2026')

    const meses = screen.getAllByRole('listitem').map((fila) => fila.firstChild.textContent)
    expect(meses).toEqual(['julio de 2026', 'agosto de 2026', 'setiembre de 2026'])
  })

  it('muestra el total del periodo', async () => {
    render(<PanelFinancieroView />)

    expect(await screen.findByText('S/ 300.00')).toBeInTheDocument()
    expect(screen.getByText('Ingresos del periodo')).toBeInTheDocument()
  })

  it('la barra del mejor mes ocupa todo el ancho y las demás son proporcionales', async () => {
    render(<PanelFinancieroView />)

    await screen.findByText('julio de 2026')

    const ancho = (nombre) => filaDelMes(nombre).querySelector('.panel-ingreso-relleno').style.width
    expect(ancho('setiembre de 2026')).toBe('100%')
    expect(ancho('julio de 2026')).toBe('50%')
    expect(ancho('agosto de 2026')).toBe('0%')
  })

  it('si Django no responde no muestra barras', async () => {
    obtenerIngresosMensuales.mockRejectedValue(new Error('sin conexión'))

    render(<PanelFinancieroView />)

    expect(await screen.findByText(/No se pudo cargar el panel/)).toBeInTheDocument()
    expect(screen.queryAllByRole('listitem')).toHaveLength(0)
  })
})
