// Registro: la casilla de Términos y Política de privacidad es obligatoria (consentimiento, Ley 29733).
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { registrar } from '../models/authApi'
import RegistroView from './RegistroView'

vi.mock('../models/authApi', async (importOriginal) => ({ ...(await importOriginal()), registrar: vi.fn() }))

async function llenarFormulario() {
  await userEvent.type(screen.getByLabelText('Tu nombre'), 'Rosa Quispe')
  await userEvent.type(screen.getByLabelText('Nombre de la empresa'), 'Panadería Doña Rosa')
  await userEvent.type(screen.getByLabelText('Correo'), 'rosa@panaderia.pe')
  await userEvent.type(screen.getByLabelText(/^Contraseña/), 'Clave1234')
  await userEvent.type(screen.getByLabelText(/^Confirmar contraseña/), 'Clave1234')
}

describe('RegistroView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    registrar.mockResolvedValue({ id: 1 })
  })

  it('sin aceptar los Términos no crea la cuenta y explica por qué', async () => {
    render(<RegistroView irALogin={vi.fn()} abrir={vi.fn()} />)
    await llenarFormulario()

    await userEvent.click(screen.getByRole('button', { name: 'Crear cuenta' }))

    expect(screen.getByText(/debes aceptar los Términos y la Política de privacidad/)).toBeInTheDocument()
    expect(registrar).not.toHaveBeenCalled()
  })

  it('al aceptar los Términos crea la cuenta', async () => {
    render(<RegistroView irALogin={vi.fn()} abrir={vi.fn()} />)
    await llenarFormulario()

    await userEvent.click(screen.getByRole('checkbox'))
    await userEvent.click(screen.getByRole('button', { name: 'Crear cuenta' }))

    expect(registrar).toHaveBeenCalledWith(expect.objectContaining({ correo: 'rosa@panaderia.pe' }))
    expect(await screen.findByText(/Te enviamos un código a tu correo/)).toBeInTheDocument()
  })

  it('al crear la cuenta abre Confirma tu correo con el correo escrito', async () => {
    const irAVerificar = vi.fn()
    render(<RegistroView irALogin={vi.fn()} abrir={vi.fn()} irAVerificar={irAVerificar} />)
    await llenarFormulario()

    await userEvent.click(screen.getByRole('checkbox'))
    await userEvent.click(screen.getByRole('button', { name: 'Crear cuenta' }))

    await vi.waitFor(() => expect(irAVerificar).toHaveBeenCalledWith('rosa@panaderia.pe'))
  })

  it('los enlaces de la casilla abren los Términos y la Política de privacidad', async () => {
    const abrir = vi.fn()
    render(<RegistroView irALogin={vi.fn()} abrir={abrir} />)

    await userEvent.click(screen.getByRole('button', { name: 'Términos y condiciones' }))
    await userEvent.click(screen.getByRole('button', { name: 'Política de privacidad' }))

    expect(abrir).toHaveBeenNthCalledWith(1, 'terminos')
    expect(abrir).toHaveBeenNthCalledWith(2, 'privacidad')
  })
})
