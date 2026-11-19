// Ayuda / Preguntas frecuentes: temas, buscador y contacto.
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import AyudaView from './AyudaView'

describe('AyudaView', () => {
  it('muestra los temas con sus preguntas y el correo de contacto', () => {
    render(<AyudaView />)

    for (const tema of ['Primeros pasos', 'Anuncios', 'Planes y pagos', 'Cuenta y seguridad']) {
      expect(screen.getByRole('heading', { name: tema })).toBeInTheDocument()
    }
    expect(screen.getByText('¿Qué son los tokens de IA?')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /@/ })).toHaveAttribute('href', expect.stringMatching(/^mailto:/))
  })

  it('el buscador filtra sin importar tildes ni mayúsculas', async () => {
    render(<AyudaView />)

    await userEvent.type(screen.getByLabelText('Buscar'), 'CONTRASEÑA')

    expect(screen.getByText('Olvidé mi contraseña, ¿qué hago?')).toBeInTheDocument()
    expect(screen.queryByText('¿Qué son los tokens de IA?')).toBeNull()
  })

  it('si no hay resultados lo dice', async () => {
    render(<AyudaView />)

    await userEvent.type(screen.getByLabelText('Buscar'), 'zzzz')

    expect(screen.getByText(/No encontramos nada con "zzzz"/)).toBeInTheDocument()
  })
})
