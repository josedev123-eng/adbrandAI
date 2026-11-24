// Aviso de conexión y pantalla de respaldo cuando una pantalla se rompe.
import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import Aviso, { SEGUNDOS_AVISO_EXITO } from './Aviso'
import AvisoConexion from './AvisoConexion'
import LimiteDeErrores from './LimiteDeErrores'

function cambiarConexion(enLinea) {
  vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(enLinea)
  act(() => {
    window.dispatchEvent(new Event(enLinea ? 'online' : 'offline'))
  })
}

describe('AvisoConexion', () => {
  afterEach(() => {
    vi.restoreAllMocks()
    vi.useRealTimers()
  })

  it('con internet no muestra nada', () => {
    const { container } = render(<AvisoConexion />)
    expect(container).toBeEmptyDOMElement()
  })

  it('avisa cuando se cae el internet y confirma cuando vuelve', async () => {
    vi.useFakeTimers()
    render(<AvisoConexion />)

    cambiarConexion(false)
    expect(screen.getByRole('alert')).toHaveTextContent('Sin conexión a internet.')

    cambiarConexion(true)
    await act(() => vi.advanceTimersByTimeAsync(10))
    expect(screen.getByRole('status')).toHaveTextContent('Conexión restablecida.')

    await act(() => vi.advanceTimersByTimeAsync(5000))
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
  })
})

function PantallaRota() {
  throw new Error('falla de prueba')
}

describe('LimiteDeErrores', () => {
  afterEach(() => vi.restoreAllMocks())

  it('si una pantalla se rompe muestra el aviso en vez de quedar en blanco', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {})
    const alVolverAlInicio = vi.fn()

    render(
      <LimiteDeErrores alVolverAlInicio={alVolverAlInicio}>
        <PantallaRota />
      </LimiteDeErrores>,
    )

    expect(screen.getByRole('heading', { name: 'Algo salió mal en esta pantalla' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Recargar' })).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Volver al inicio' }))
    expect(alVolverAlInicio).toHaveBeenCalled()
  })

  it('si no hay error muestra la pantalla normal', () => {
    render(
      <LimiteDeErrores>
        <p>Todo bien</p>
      </LimiteDeErrores>,
    )
    expect(screen.getByText('Todo bien')).toBeInTheDocument()
  })
})

describe('Aviso', () => {
  afterEach(() => vi.useRealTimers())

  it('el aviso de éxito se desvanece solo después de unos segundos', async () => {
    vi.useFakeTimers()
    render(<Aviso mensaje={{ tipo: 'exito', texto: 'Datos guardados' }} />)
    expect(screen.getByRole('status')).toHaveTextContent('Datos guardados')

    await act(() => vi.advanceTimersByTimeAsync(SEGUNDOS_AVISO_EXITO * 1000))
    expect(screen.getByRole('status')).toHaveClass('aviso-saliendo')

    await act(() => vi.advanceTimersByTimeAsync(600))
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
  })

  it('el aviso de error se queda para que se pueda leer', async () => {
    vi.useFakeTimers()
    render(<Aviso mensaje={{ tipo: 'error', texto: 'No se pudo guardar.' }} />)

    await act(() => vi.advanceTimersByTimeAsync(10_000))
    expect(screen.getByRole('alert')).toHaveTextContent('No se pudo guardar.')
  })

  it('un mensaje nuevo vuelve a aparecer aunque el anterior ya se haya ido', async () => {
    vi.useFakeTimers()
    const { rerender } = render(<Aviso mensaje={{ tipo: 'exito', texto: 'Datos guardados' }} />)
    await act(() => vi.advanceTimersByTimeAsync(SEGUNDOS_AVISO_EXITO * 1000 + 600))
    expect(screen.queryByRole('status')).not.toBeInTheDocument()

    rerender(<Aviso mensaje={{ tipo: 'exito', texto: 'Datos guardados' }} />)
    expect(screen.getByRole('status')).toHaveTextContent('Datos guardados')
  })
})
