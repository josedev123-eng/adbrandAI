// Login de administradores: prueba la pantalla y su viewmodel sin Django encendido.
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { SesionProvider } from '../../../app/context/SesionProvider'
import { leerToken } from '../../../shared/services/sesionToken'
import { iniciarSesion } from '../models/authApi'
import LoginView from './LoginView'

vi.mock('../models/authApi', () => ({
  iniciarSesion: vi.fn(),
  obtenerSesion: vi.fn(),
}))

function errorHttp(status, detail) {
  return Object.assign(new Error('falló'), { status, detalles: detail ? { detail } : null })
}

function mostrar() {
  render(
    <SesionProvider>
      <LoginView />
    </SesionProvider>,
  )
}

describe('LoginView (login de administradores)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  it('pide correo y contraseña antes de llamar a la API', async () => {
    mostrar()
    await userEvent.click(screen.getByRole('button', { name: 'Ingresar' }))

    expect(screen.getByText('Escribe tu correo.')).toBeInTheDocument()
    expect(screen.getByText('Escribe tu contraseña.')).toBeInTheDocument()
    expect(iniciarSesion).not.toHaveBeenCalled()
  })

  it('rechaza un correo mal escrito', async () => {
    mostrar()
    await userEvent.type(screen.getByLabelText('Correo'), 'jose-adbrand.pe')
    await userEvent.type(screen.getByLabelText('Contraseña'), 'Secreta123')
    await userEvent.click(screen.getByRole('button', { name: 'Ingresar' }))

    expect(screen.getByText('Escribe un correo válido.')).toBeInTheDocument()
    expect(iniciarSesion).not.toHaveBeenCalled()
  })

  it('con datos correctos guarda el token', async () => {
    iniciarSesion.mockResolvedValue({ token: 'token-de-prueba', admin: { id: 1, nombres: 'Jose', rol: { nombre: 'SUPERADMIN' } } })
    mostrar()
    await userEvent.type(screen.getByLabelText('Correo'), ' Jose@AdBrand.pe ')
    await userEvent.type(screen.getByLabelText('Contraseña'), 'Secreta123')
    await userEvent.click(screen.getByRole('button', { name: 'Ingresar' }))

    await waitFor(() => expect(leerToken()).toBe('token-de-prueba'))
    expect(iniciarSesion).toHaveBeenCalledWith('Jose@AdBrand.pe', 'Secreta123')
  })

  it('muestra el mensaje de Django si la contraseña es incorrecta y limpia la contraseña', async () => {
    iniciarSesion.mockRejectedValue(errorHttp(401, 'Correo o contraseña incorrectos.'))
    mostrar()
    await userEvent.type(screen.getByLabelText('Correo'), 'jose@adbrand.pe')
    await userEvent.type(screen.getByLabelText('Contraseña'), 'mala')
    await userEvent.click(screen.getByRole('button', { name: 'Ingresar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Correo o contraseña incorrectos.')
    expect(screen.getByLabelText('Contraseña')).toHaveValue('')
    expect(leerToken()).toBeNull()
  })

  it('avisa si la cuenta está desactivada (HU 03)', async () => {
    iniciarSesion.mockRejectedValue(errorHttp(403, 'Tu cuenta está desactivada. Contacta al superadministrador.'))
    mostrar()
    await userEvent.type(screen.getByLabelText('Correo'), 'ana@adbrand.pe')
    await userEvent.type(screen.getByLabelText('Contraseña'), 'Secreta123')
    await userEvent.click(screen.getByRole('button', { name: 'Ingresar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Tu cuenta está desactivada')
  })

  it('avisa si Django está apagado', async () => {
    iniciarSesion.mockRejectedValue(new TypeError('Failed to fetch'))
    mostrar()
    await userEvent.type(screen.getByLabelText('Correo'), 'jose@adbrand.pe')
    await userEvent.type(screen.getByLabelText('Contraseña'), 'Secreta123')
    await userEvent.click(screen.getByRole('button', { name: 'Ingresar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo conectar con el servidor')
  })
})
