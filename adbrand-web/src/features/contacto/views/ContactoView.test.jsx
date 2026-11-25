// Contacto: formulario corto que se envía con o sin sesión.
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { enviarMensajeContacto } from '../models/contactoApi'
import ContactoView from './ContactoView'

vi.mock('../models/contactoApi', async (importOriginal) => ({
  ...(await importOriginal()),
  enviarMensajeContacto: vi.fn(),
}))

describe('ContactoView', () => {
  beforeEach(() => vi.clearAllMocks())

  it('con sesión trae el nombre y el correo, y al enviar agradece', async () => {
    enviarMensajeContacto.mockResolvedValue({ id: 1 })
    render(<ContactoView nombre="Rosa Quispe" correo="rosa@panaderia.pe" />)

    expect(screen.getByLabelText('Nombre')).toHaveValue('Rosa Quispe')
    await userEvent.selectOptions(screen.getByLabelText('Motivo'), 'PROBLEMA')
    await userEvent.type(screen.getByLabelText(/^Mensaje/), 'No puedo descargar mi video.')
    await userEvent.click(screen.getByRole('button', { name: 'Enviar' }))

    expect(enviarMensajeContacto).toHaveBeenCalledWith({
      nombre: 'Rosa Quispe',
      correo: 'rosa@panaderia.pe',
      asunto: 'PROBLEMA',
      mensaje: 'No puedo descargar mi video.',
    })
    expect(await screen.findByText('✓ ¡Mensaje enviado!')).toBeInTheDocument()
  })

  it('no envía si faltan datos y dice qué corregir', async () => {
    render(<ContactoView />)

    await userEvent.type(screen.getByLabelText('Correo'), 'rosa@')
    await userEvent.click(screen.getByRole('button', { name: 'Enviar' }))

    expect(screen.getByText('Escribe tu nombre.')).toBeInTheDocument()
    expect(screen.getByText('Escribe un correo válido.')).toBeInTheDocument()
    expect(screen.getByText('Elige el motivo.')).toBeInTheDocument()
    expect(enviarMensajeContacto).not.toHaveBeenCalled()
  })

  it('si el servidor falla muestra el error y deja intentar de nuevo', async () => {
    enviarMensajeContacto.mockRejectedValue(new Error('Ya nos escribiste varias veces en la última hora.'))
    render(<ContactoView nombre="Rosa" correo="rosa@panaderia.pe" />)

    await userEvent.selectOptions(screen.getByLabelText('Motivo'), 'DUDA')
    await userEvent.type(screen.getByLabelText(/^Mensaje/), '¿Cómo cambio mi plan?')
    await userEvent.click(screen.getByRole('button', { name: 'Enviar' }))

    expect(await screen.findByText(/varias veces/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Enviar' })).toBeEnabled()
  })
})
