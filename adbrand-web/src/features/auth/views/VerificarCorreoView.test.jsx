// Confirmar el correo con el código de 6 dígitos después de registrarse.
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { reenviarCodigoVerificacion, verificarCorreo } from '../models/authApi'
import VerificarCorreoView from './VerificarCorreoView'

const entrar = vi.fn()
vi.mock('../../../app/context/useSesion', () => ({ useSesion: () => ({ entrar }) }))
vi.mock('../models/authApi', async (importOriginal) => ({
  ...(await importOriginal()),
  verificarCorreo: vi.fn(),
  reenviarCodigoVerificacion: vi.fn(),
}))

describe('VerificarCorreoView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    reenviarCodigoVerificacion.mockResolvedValue({ mensaje: 'ok' })
  })

  it('con el código correcto entra directo', async () => {
    verificarCorreo.mockResolvedValue({ token: 't-1', usuario: { id: 1 } })
    render(<VerificarCorreoView correo="rosa@panaderia.pe" irALogin={vi.fn()} />)

    expect(screen.getByText('rosa@panaderia.pe')).toBeInTheDocument()
    await userEvent.type(screen.getByLabelText(/^Código/), '123456')
    await userEvent.click(screen.getByRole('button', { name: 'Confirmar y entrar' }))

    expect(verificarCorreo).toHaveBeenCalledWith('rosa@panaderia.pe', '123456')
    await waitFor(() => expect(entrar).toHaveBeenCalledWith('t-1', { id: 1 }))
  })

  it('pide los 6 dígitos antes de llamar a la API', async () => {
    render(<VerificarCorreoView correo="rosa@panaderia.pe" irALogin={vi.fn()} />)

    await userEvent.type(screen.getByLabelText(/^Código/), '12a3')
    await userEvent.click(screen.getByRole('button', { name: 'Confirmar y entrar' }))

    expect(screen.getByText('El código tiene 6 dígitos.')).toBeInTheDocument()
    expect(verificarCorreo).not.toHaveBeenCalled()
  })

  it('muestra el error si el código está mal', async () => {
    verificarCorreo.mockRejectedValue(
      Object.assign(new Error('El código no es correcto o ya venció. Pide uno nuevo.'), { status: 400, codigo: 'CODIGO_INVALIDO' }),
    )
    render(<VerificarCorreoView correo="rosa@panaderia.pe" irALogin={vi.fn()} />)

    await userEvent.type(screen.getByLabelText(/^Código/), '000000')
    await userEvent.click(screen.getByRole('button', { name: 'Confirmar y entrar' }))

    expect(await screen.findByText(/no es correcto o ya venció/)).toBeInTheDocument()
    expect(entrar).not.toHaveBeenCalled()
  })

  it('recién registrado, el botón de reenviar espera un minuto', () => {
    render(<VerificarCorreoView correo="rosa@panaderia.pe" irALogin={vi.fn()} />)

    expect(screen.getByRole('button', { name: /Reenviar código en \d+ s/ })).toBeDisabled()
    expect(reenviarCodigoVerificacion).not.toHaveBeenCalled()
  })

  it('si llega desde el inicio de sesión, pide un código nuevo al abrir', async () => {
    render(<VerificarCorreoView correo="rosa@panaderia.pe" desdeLogin irALogin={vi.fn()} />)

    await waitFor(() => expect(reenviarCodigoVerificacion).toHaveBeenCalledWith('rosa@panaderia.pe'))
    expect(screen.getByText(/Antes de entrar falta confirmar tu correo/)).toBeInTheDocument()
  })
})
