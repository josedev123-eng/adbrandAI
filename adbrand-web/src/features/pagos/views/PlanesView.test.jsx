// HU 25: la pantalla de planes muestra el nombre, el precio y el límite de tokens de cada plan.
import { render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { obtenerConsumoMes, obtenerPlanes } from '../models/pagosApi'
import { conMiles } from '../models/formatoPlanes'
import PlanesView from './PlanesView'

vi.mock('../models/pagosApi', () => ({
  obtenerPlanes: vi.fn(),
  obtenerConsumoMes: vi.fn(() => Promise.resolve({ origen: 'GRATUITO', limite: 10000 })),
}))

const PLANES = [
  { id: 1, nombre: 'Básico', precio: 29.9, limiteTokens: 50000, descripcion: 'Para empezar.' },
  { id: 2, nombre: 'Pro', precio: 59.9, limiteTokens: 150000, descripcion: 'Para crecer.' },
  { id: 3, nombre: 'Premium', precio: 99.9, limiteTokens: 400000, descripcion: 'Sin límites prácticos.' },
]

describe('PlanesView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('muestra los tres planes con su precio y sus tokens', async () => {
    obtenerPlanes.mockResolvedValue(PLANES)

    render(<PlanesView />)

    expect(await screen.findByText('Básico')).toBeInTheDocument()
    expect(screen.getByText('Pro')).toBeInTheDocument()
    expect(screen.getByText('Premium')).toBeInTheDocument()
    expect(screen.getByText(/29\.90/)).toBeInTheDocument()
    expect(screen.getByText(/99\.90/)).toBeInTheDocument()
    expect(screen.getByText(/400[.,]000 tokens/)).toBeInTheDocument()
  })

  it('avisa cuando no se pudieron cargar los planes', async () => {
    obtenerPlanes.mockRejectedValue(new Error('sin conexión'))

    render(<PlanesView />)

    expect(await screen.findByText(/No se pudieron cargar los planes/)).toBeInTheDocument()
  })

  it('muestra el plan actual y lo marca en su tarjeta', async () => {
    obtenerPlanes.mockResolvedValue(PLANES)
    obtenerConsumoMes.mockResolvedValueOnce({ origen: 'PLAN', plan: 'Pro', limite: 150000 })

    render(<PlanesView />)

    expect(await screen.findByText('Tu plan actual')).toBeInTheDocument()
    expect(screen.getByText(/Tu plan actual:/)).toHaveTextContent('Pro')
  })

  it('con el plan gratuito lo dice arriba', async () => {
    obtenerPlanes.mockResolvedValue(PLANES)

    render(<PlanesView />)

    expect(await screen.findByText(/Tu plan actual:/)).toHaveTextContent('Gratuito')
  })

  it('escribe los miles igual en toda la página', () => {
    expect(conMiles('Para empezar: 50 000 tokens de IA y 150 000 más')).toBe('Para empezar: 50,000 tokens de IA y 150,000 más')
  })
})
