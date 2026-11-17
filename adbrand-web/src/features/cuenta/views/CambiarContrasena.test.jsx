// Mi cuenta: cambiar la contraseña escribiendo la actual.
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { cambiarMiContrasena } from '../models/cuentaApi'
import CambiarContrasena from './CambiarContrasena'

vi.mock('../models/cuentaApi', () => ({ cambiarMiContrasena: vi.fn() }))

async function llenar(actual, nueva, confirmar = nueva) {
  await userEvent.type(screen.getByLabelText('Contraseña actual'), actual)
  await userEvent.type(screen.getByLabelText(/^Contraseña nueva/), nueva)
  await userEvent.type(screen.getByLabelText('Confirmar contraseña nueva'), confirmar)
  await userEvent.click(screen.getByRole('button', { name: 'Cambiar contraseña' }))
}

describe('CambiarContrasena', () => {
  beforeEach(() => vi.clearAllMocks())

  it('no envía una contraseña débil ni una confirmación distinta', async () => {
    render(<CambiarContrasena />)
    await llenar('Actual123', 'debil')
    expect(cambiarMiContrasena).not.toHaveBeenCalled()
  })

  it('con datos válidos la cambia y avisa', async () => {
    cambiarMiContrasena.mockResolvedValue({ mensaje: 'Tu contraseña se cambió.' })
    render(<CambiarContrasena />)

    await llenar('Actual123', 'Nueva2026x')

    expect(cambiarMiContrasena).toHaveBeenCalledWith('Actual123', 'Nueva2026x')
    expect(await screen.findByText('Tu contraseña se cambió.')).toBeInTheDocument()
  })

  it('si la actual está mal, lo dice en ese campo', async () => {
    cambiarMiContrasena.mockRejectedValue(
      Object.assign(new Error('La contraseña actual no es correcta.'), { status: 400, codigo: 'CONTRASENA_ACTUAL_INCORRECTA' }),
    )
    render(<CambiarContrasena />)

    await llenar('Otra12345', 'Nueva2026x')

    expect(await screen.findByText('La contraseña actual no es correcta.')).toBeInTheDocument()
  })
})
