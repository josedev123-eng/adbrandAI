// Página de presentación: explica AdBrand.AI, muestra los planes y lleva a crear la cuenta o a iniciar sesión.
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { obtenerPlanes } from '../../pagos/models/pagosApi'
import LandingView from './LandingView'

vi.mock('../../pagos/models/pagosApi', () => ({ obtenerPlanes: vi.fn() }))

function mostrar() {
  const acciones = { irARegistro: vi.fn(), irALogin: vi.fn(), abrir: vi.fn() }
  render(<LandingView {...acciones} />)
  return acciones
}

describe('LandingView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    obtenerPlanes.mockResolvedValue([{ id: 2, nombre: 'Pro', precio: 59.9, limiteTokens: 150000, descripcion: 'Para crecer.' }])
  })

  it('explica qué es y cómo funciona en tres pasos', async () => {
    mostrar()

    expect(screen.getByRole('heading', { level: 1, name: /Anuncios que se ven profesionales/ })).toBeInTheDocument()
    expect(screen.getByText('Cuéntanos de tu negocio')).toBeInTheDocument()
    expect(screen.getByText('La IA crea tu anuncio')).toBeInTheDocument()
    expect(screen.getByText('Revisa y programa')).toBeInTheDocument()
    await screen.findByText('Pro')
  })

  it('muestra el plan gratuito y los planes de pago con su precio en soles', async () => {
    mostrar()

    expect(await screen.findByText('Pro')).toBeInTheDocument()
    expect(screen.getByText('Gratuito')).toBeInTheDocument()
    expect(screen.getByText(/S\/ 59\.90/)).toBeInTheDocument()
    expect(screen.getByText('Para crecer.')).toBeInTheDocument()
  })

  it('si el servidor no responde, la página se sigue viendo con el plan gratuito', async () => {
    obtenerPlanes.mockRejectedValue(new Error('sin conexión'))
    mostrar()

    expect(await screen.findByText('Los planes de pago se ven al crear tu cuenta.')).toBeInTheDocument()
    expect(screen.getByText('Gratuito')).toBeInTheDocument()
  })

  it('los botones llevan al registro, al inicio de sesión y a los textos legales', async () => {
    const { irARegistro, irALogin, abrir } = mostrar()
    await screen.findByText('Pro')

    await userEvent.click(screen.getAllByRole('button', { name: 'Crear cuenta gratis' })[0])
    await userEvent.click(screen.getByRole('button', { name: 'Iniciar sesión' }))
    await userEvent.click(screen.getByRole('button', { name: 'Política de privacidad' }))

    expect(irARegistro).toHaveBeenCalled()
    expect(irALogin).toHaveBeenCalled()
    expect(abrir).toHaveBeenCalledWith('privacidad')
  })
})
