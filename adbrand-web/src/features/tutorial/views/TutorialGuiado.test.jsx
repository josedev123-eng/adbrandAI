// Tutorial guiado: recorre las opciones del menú, deja continuar o terminar en cualquier momento y avisa al final.
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { PASOS_TUTORIAL } from '../models/tutorialApi'
import TutorialGuiado from './TutorialGuiado'

function mostrar() {
  const acciones = { alTerminar: vi.fn(), alSaltar: vi.fn() }
  render(
    <>
      <button type="button" data-tutorial="perfil">
        Mi negocio
      </button>
      <TutorialGuiado empresa="Panadería Doña Rosa" {...acciones} />
    </>,
  )
  return acciones
}

async function irAlUltimoPaso() {
  for (let i = 1; i < PASOS_TUTORIAL.length; i++) await userEvent.keyboard('{ArrowRight}')
}

describe('TutorialGuiado', () => {
  it('da la bienvenida con el nombre de la empresa y avanza con "Continuar"', async () => {
    mostrar()

    expect(screen.getByRole('dialog', { name: '¡Hola, Panadería Doña Rosa! Qué bueno tenerte aquí' })).toBeInTheDocument()
    expect(screen.getByText(`Paso 1 de ${PASOS_TUTORIAL.length}`)).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: /¡Vamos!/ }))
    expect(screen.getByRole('dialog', { name: 'Primero, cuéntanos de tu negocio' })).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Continuar →' }))
    expect(screen.getByText('Paso 3 de 8')).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Atrás' }))
    expect(screen.getByText('Paso 2 de 8')).toBeInTheDocument()
  })

  it('"Terminar tutorial" pregunta primero y "Continuar el tutorial" sigue en el mismo paso', async () => {
    const { alSaltar } = mostrar()
    await userEvent.click(screen.getByRole('button', { name: /¡Vamos!/ }))

    await userEvent.click(screen.getByRole('button', { name: 'Terminar tutorial' }))
    expect(screen.getByText(/¿Terminar el recorrido aquí\?/)).toBeInTheDocument()
    expect(screen.getByText(/Te faltan 6 pasos/)).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Continuar el tutorial' }))
    expect(screen.queryByText(/¿Terminar el recorrido aquí\?/)).not.toBeInTheDocument()
    expect(screen.getByText('Paso 2 de 8')).toBeInTheDocument()
    expect(alSaltar).not.toHaveBeenCalled()
  })

  it('con la ✕ o con Esc también pregunta, y "Sí, terminar" lo cierra', async () => {
    const { alSaltar } = mostrar()

    await userEvent.click(screen.getByRole('button', { name: 'Cerrar tutorial' }))
    await userEvent.click(screen.getByRole('button', { name: 'Sí, terminar' }))
    expect(alSaltar).toHaveBeenCalledTimes(1)
  })

  it('Esc abre la pregunta y otro Esc vuelve al recorrido', async () => {
    mostrar()

    await userEvent.keyboard('{Escape}')
    expect(screen.getByText(/¿Terminar el recorrido aquí\?/)).toBeInTheDocument()
    await userEvent.keyboard('{Escape}')
    expect(screen.queryByText(/¿Terminar el recorrido aquí\?/)).not.toBeInTheDocument()
  })

  it('en el último paso se puede ir a Mi negocio o quedarse en la pantalla actual', async () => {
    const { alTerminar, alSaltar } = mostrar()
    await irAlUltimoPaso()

    expect(screen.getByRole('dialog', { name: '¡Todo listo para despegar!' })).toBeInTheDocument()
    expect(screen.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '100')

    await userEvent.click(screen.getByRole('button', { name: 'Quedarme aquí' }))
    expect(alSaltar).toHaveBeenCalled()

    await userEvent.click(screen.getByRole('button', { name: /Ir a mi negocio/ }))
    expect(alTerminar).toHaveBeenCalled()
  })
})
