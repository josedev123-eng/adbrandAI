// Mi cuenta del administrador: ver sus datos y cambiar su contraseña escribiendo la actual.
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { cambiarMiContrasena } from '../../auth/models/authApi'
import MiCuentaAdminView from './MiCuentaAdminView'

vi.mock('../../auth/models/authApi', () => ({ cambiarMiContrasena: vi.fn() }))
vi.mock('../../../app/context/useSesion', () => ({
  useSesion: () => ({
    admin: { id: 7, nombres: 'Ana', apellidos: 'Quispe', correo: 'ana@adbrand.pe', rol: { id: 2, nombre: 'MODERADOR' } },
  }),
}))

async function llenar(actual, nueva, confirmar = nueva) {
  await userEvent.type(screen.getByLabelText('Contraseña actual'), actual)
  await userEvent.type(screen.getByLabelText(/^Contraseña nueva/), nueva)
  await userEvent.type(screen.getByLabelText('Confirmar contraseña nueva'), confirmar)
  await userEvent.click(screen.getByRole('button', { name: 'Cambiar contraseña' }))
}

describe('MiCuentaAdminView', () => {
  beforeEach(() => vi.clearAllMocks())

  it('muestra los datos del administrador y su rol', () => {
    render(<MiCuentaAdminView />)

    expect(screen.getByText('Ana Quispe')).toBeInTheDocument()
    expect(screen.getByText('ana@adbrand.pe')).toBeInTheDocument()
    expect(screen.getByText('Moderador')).toBeInTheDocument()
  })

  it('no envía una contraseña débil ni una confirmación distinta', async () => {
    render(<MiCuentaAdminView />)

    await llenar('Actual123', 'debil')
    expect(screen.getByText('Usa al menos 8 caracteres.')).toBeInTheDocument()
    expect(cambiarMiContrasena).not.toHaveBeenCalled()
  })

  it('con datos válidos cambia la contraseña y avisa', async () => {
    cambiarMiContrasena.mockResolvedValue({ detail: 'Tu contraseña se cambió.' })
    render(<MiCuentaAdminView />)

    await llenar('Actual123', 'Nueva2026')

    expect(cambiarMiContrasena).toHaveBeenCalledWith('Actual123', 'Nueva2026')
    expect(await screen.findByText('Tu contraseña se cambió.')).toBeInTheDocument()
  })

  it('si la actual está mal, lo dice en ese campo', async () => {
    cambiarMiContrasena.mockRejectedValue(
      Object.assign(new Error('x'), { status: 400, detalles: { actual: ['La contraseña actual no es correcta.'] } }),
    )
    render(<MiCuentaAdminView />)

    await llenar('Otra1234', 'Nueva2026')

    expect(await screen.findByText('La contraseña actual no es correcta.')).toBeInTheDocument()
  })
})
