// Términos y condiciones / Política de privacidad.
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import DocumentoLegalView from './DocumentoLegalView'

describe('DocumentoLegalView', () => {
  it('la política de privacidad explica los datos, la Ley 29733 y cómo pedir que se borren', () => {
    render(<DocumentoLegalView tipo="privacidad" />)

    expect(screen.getByRole('heading', { level: 1, name: 'Política de privacidad' })).toBeInTheDocument()
    expect(screen.getByText(/Ley N\.º 29733/)).toBeInTheDocument()
    expect(screen.getByText(/Mercado Pago; nosotros no los vemos ni los guardamos/)).toBeInTheDocument()
    expect(screen.getByText(/ver, corregir o borrar tus datos/)).toBeInTheDocument()
  })

  it('los términos explican la moderación y la suspensión, y el botón vuelve atrás', async () => {
    const alVolver = vi.fn()
    render(<DocumentoLegalView tipo="terminos" alVolver={alVolver} textoVolver="Volver al registro" />)

    expect(screen.getByRole('heading', { level: 1, name: 'Términos y condiciones' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: '5. Suspensión de la cuenta' })).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: /Volver al registro/ }))
    expect(alVolver).toHaveBeenCalled()
  })
})
