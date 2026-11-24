// "¿Olvidaste tu contraseña?" del panel: pide el código al correo y con él pone una contraseña nueva.
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { cambiarConCodigo, solicitarCodigo } from '../models/authApi'
import RecuperarContrasenaView from './RecuperarContrasenaView'

vi.mock('../models/authApi', () => ({ solicitarCodigo: vi.fn(), cambiarConCodigo: vi.fn() }))

describe('RecuperarContrasenaView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    solicitarCodigo.mockResolvedValue({ detail: 'Si el correo es de un administrador activo, te enviamos un código.' })
  })

  it('pide el código y luego cambia la contraseña', async () => {
    cambiarConCodigo.mockResolvedValue({ detail: 'Listo, tu contraseña se cambió.' })
    const alTerminar = vi.fn()
    render(<RecuperarContrasenaView irALogin={vi.fn()} alTerminar={alTerminar} />)

    await userEvent.type(screen.getByLabelText('Correo'), 'ana@adbrand.pe')
    await userEvent.click(screen.getByRole('button', { name: 'Enviarme el código' }))
    expect(solicitarCodigo).toHaveBeenCalledWith('ana@adbrand.pe')
    expect(await screen.findByText(/te enviamos un código/)).toBeInTheDocument()

    await userEvent.type(screen.getByLabelText('Código de 6 números'), '12a3456')
    await userEvent.type(screen.getByLabelText(/^Contraseña nueva/), 'Nueva2026')
    await userEvent.type(screen.getByLabelText('Confirmar contraseña nueva'), 'Nueva2026')
    await userEvent.click(screen.getByRole('button', { name: 'Cambiar contraseña' }))

    // Las letras del código se ignoran: solo números.
    expect(cambiarConCodigo).toHaveBeenCalledWith('ana@adbrand.pe', '123456', 'Nueva2026')
    expect(alTerminar).toHaveBeenCalledWith('Listo, tu contraseña se cambió.')
  })

  it('si el código está mal o venció, lo dice en el campo del código', async () => {
    cambiarConCodigo.mockRejectedValue(
      Object.assign(new Error('x'), { status: 400, detalles: { codigo: ['El código no es correcto o ya venció. Pide uno nuevo.'] } }),
    )
    render(<RecuperarContrasenaView irALogin={vi.fn()} alTerminar={vi.fn()} />)
    await userEvent.type(screen.getByLabelText('Correo'), 'ana@adbrand.pe')
    await userEvent.click(screen.getByRole('button', { name: 'Enviarme el código' }))

    await userEvent.type(await screen.findByLabelText('Código de 6 números'), '000000')
    await userEvent.type(screen.getByLabelText(/^Contraseña nueva/), 'Nueva2026')
    await userEvent.type(screen.getByLabelText('Confirmar contraseña nueva'), 'Nueva2026')
    await userEvent.click(screen.getByRole('button', { name: 'Cambiar contraseña' }))

    expect(await screen.findByText(/ya venció/)).toBeInTheDocument()
  })

  it('valida el correo antes de pedir el código', async () => {
    render(<RecuperarContrasenaView irALogin={vi.fn()} alTerminar={vi.fn()} />)

    await userEvent.click(screen.getByRole('button', { name: 'Enviarme el código' }))

    expect(screen.getByText('Escribe tu correo.')).toBeInTheDocument()
    expect(solicitarCodigo).not.toHaveBeenCalled()
  })
})
