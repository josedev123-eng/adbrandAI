// Campo de contraseña: no se puede copiar ni pegar, y se puede mostrar/ocultar.
import { createEvent, fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { describe, expect, it } from 'vitest'
import InputContrasena, { AVISO_SIN_PEGAR } from './InputContrasena'

function Formulario({ permitirPegar }) {
  const [valor, setValor] = useState('')
  return (
    <label>
      Contraseña
      <InputContrasena value={valor} onChange={(e) => setValor(e.target.value)} permitirPegar={permitirPegar} />
    </label>
  )
}

function eventoCancelado(elemento, tipo) {
  const evento = createEvent[tipo](elemento)
  fireEvent(elemento, evento)
  return evento.defaultPrevented
}

describe('InputContrasena', () => {
  it('oculta lo escrito y deja mostrarlo con el botón', async () => {
    render(<Formulario />)
    const campo = screen.getByLabelText('Contraseña')
    expect(campo).toHaveAttribute('type', 'password')

    await userEvent.click(screen.getByRole('button', { name: 'Mostrar contraseña' }))
    expect(campo).toHaveAttribute('type', 'text')
    await userEvent.click(screen.getByRole('button', { name: 'Ocultar contraseña' }))
    expect(campo).toHaveAttribute('type', 'password')
  })

  it('no deja copiar, cortar ni pegar, y avisa por qué', () => {
    render(<Formulario />)
    const campo = screen.getByLabelText('Contraseña')

    expect(eventoCancelado(campo, 'copy')).toBe(true)
    expect(eventoCancelado(campo, 'cut')).toBe(true)
    expect(eventoCancelado(campo, 'paste')).toBe(true)
    expect(screen.getByRole('status')).toHaveTextContent(AVISO_SIN_PEGAR)
  })

  it('con permitirPegar sí deja pegar, pero nunca copiar', () => {
    render(<Formulario permitirPegar />)
    const campo = screen.getByLabelText('Contraseña')

    expect(eventoCancelado(campo, 'paste')).toBe(false)
    expect(eventoCancelado(campo, 'copy')).toBe(true)
  })
})
