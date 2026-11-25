// Mensajes de contacto: se listan los pendientes y se marcan como atendidos.
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { listarMensajes, marcarAtendido } from '../models/contactoApi'
import MensajesContactoView from './MensajesContactoView'

vi.mock('../models/contactoApi', () => ({ listarMensajes: vi.fn(), marcarAtendido: vi.fn() }))

const MENSAJE = {
  id: 3,
  usuario_id: 12,
  nombre: 'Rosa Quispe',
  correo: 'rosa@panaderia.pe',
  asunto: 'PROBLEMA',
  motivo: 'Algo no funciona',
  mensaje: 'No puedo descargar mi video.',
  atendido: false,
  fecha_creacion: '2026-11-26T15:00:00Z',
}

describe('MensajesContactoView', () => {
  beforeEach(() => vi.clearAllMocks())

  it('muestra los pendientes y al marcar uno como atendido sale de la lista', async () => {
    listarMensajes.mockResolvedValue([MENSAJE])
    marcarAtendido.mockResolvedValue({ ...MENSAJE, atendido: true })
    render(<MensajesContactoView />)

    expect(await screen.findByText('No puedo descargar mi video.')).toBeInTheDocument()
    expect(listarMensajes).toHaveBeenCalledWith({ pendientes: true })
    expect(screen.getByRole('link', { name: 'Responder por correo' })).toHaveAttribute('href', expect.stringMatching(/^mailto:rosa@panaderia\.pe/))

    await userEvent.click(screen.getByRole('button', { name: '✓ Marcar como atendido' }))

    expect(marcarAtendido).toHaveBeenCalledWith(3, true)
    expect(await screen.findByText('No hay mensajes pendientes. ¡Todo al día!')).toBeInTheDocument()
  })
})
