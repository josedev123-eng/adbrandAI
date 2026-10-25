// HU-18: panel del estado del servidor, sin necesitar Django encendido.
import { act, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { formatearHora, verificarEstado } from '../models/servidorApi'
import { CADA_CUANTO_MS } from '../viewmodels/useEstadoServidor'
import EstadoServidorView from './EstadoServidorView'

vi.mock('../models/servidorApi', async (importOriginal) => ({
  ...(await importOriginal()),
  verificarEstado: vi.fn(),
}))

const HORA = '2026-10-27T09:05:03-05:00'

function servicio(servicio, nombre, estado, extra = {}) {
  return { servicio, nombre, estado, tiempo_ms: estado === 'ACTIVO' ? 12 : null, detalle: null, ...extra }
}

const TODO_ACTIVO = {
  verificado_en: HORA,
  servicios: [
    servicio('BACKEND', 'Backend', 'ACTIVO'),
    servicio('BASE_DATOS', 'Base de datos', 'ACTIVO'),
    servicio('IA', 'Servicio de IA', 'ACTIVO', { detalle: 'Modo simulado' }),
  ],
}

const BACKEND_CAIDO = {
  verificado_en: HORA,
  servicios: [
    servicio('BACKEND', 'Backend', 'CAIDO', { detalle: 'El backend no responde' }),
    servicio('BASE_DATOS', 'Base de datos', 'ACTIVO'),
    servicio('IA', 'Servicio de IA', 'CAIDO', { detalle: 'No se pudo verificar porque el backend no responde' }),
  ],
}

function tarjeta(nombre) {
  return screen.getByRole('heading', { name: nombre }).closest('article')
}

describe('EstadoServidorView (HU-18)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('muestra cada servicio activo y la hora de la última verificación (criterio 1)', async () => {
    verificarEstado.mockResolvedValue(TODO_ACTIVO)
    render(<EstadoServidorView />)

    expect(await screen.findByText(`Última verificación: ${formatearHora(HORA)}`)).toBeInTheDocument()
    for (const nombre of ['Backend', 'Base de datos', 'Servicio de IA']) {
      expect(within(tarjeta(nombre)).getByText('Activo')).toBeInTheDocument()
    }
    expect(within(tarjeta('Backend')).getByText('12 ms')).toBeInTheDocument()
    expect(screen.queryByRole('alert')).toBeNull()
  })

  it('marca como caído el servicio que no responde, con el motivo (criterio 2)', async () => {
    verificarEstado.mockResolvedValue(BACKEND_CAIDO)
    render(<EstadoServidorView />)

    expect(await screen.findByRole('alert')).toHaveTextContent('Hay servicios caídos: Backend, Servicio de IA.')
    expect(tarjeta('Backend')).toHaveClass('servicio-caido')
    expect(within(tarjeta('Backend')).getByText('Caído')).toBeInTheDocument()
    expect(within(tarjeta('Backend')).getByText('El backend no responde')).toBeInTheDocument()
    expect(within(tarjeta('Base de datos')).getByText('Activo')).toBeInTheDocument()
  })

  it('muestra un error si no se puede consultar a Django', async () => {
    verificarEstado.mockRejectedValue({ status: 500 })
    render(<EstadoServidorView />)

    expect(await screen.findByText(/No se pudo consultar el estado/)).toBeInTheDocument()
  })

  it('el botón Verificar ahora vuelve a consultar', async () => {
    verificarEstado.mockResolvedValueOnce(TODO_ACTIVO).mockResolvedValueOnce(BACKEND_CAIDO)
    render(<EstadoServidorView />)
    await screen.findByText(/Última verificación/)

    await userEvent.click(screen.getByRole('button', { name: 'Verificar ahora' }))

    expect(await screen.findByRole('alert')).toBeInTheDocument()
    expect(verificarEstado).toHaveBeenCalledTimes(2)
  })

  it('se vuelve a verificar solo cada 15 segundos y deja de hacerlo al salir', async () => {
    vi.useFakeTimers()
    verificarEstado.mockResolvedValue(TODO_ACTIVO)
    const { unmount } = render(<EstadoServidorView />)
    await act(async () => {})
    expect(verificarEstado).toHaveBeenCalledTimes(1)

    await act(async () => {
      await vi.advanceTimersByTimeAsync(CADA_CUANTO_MS)
    })
    expect(verificarEstado).toHaveBeenCalledTimes(2)

    unmount()
    await act(async () => {
      await vi.advanceTimersByTimeAsync(CADA_CUANTO_MS * 3)
    })
    expect(verificarEstado).toHaveBeenCalledTimes(2)
  })
})
