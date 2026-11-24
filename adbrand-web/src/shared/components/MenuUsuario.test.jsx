// Menú lateral de la web del usuario: secciones, opción activa, iniciales de la empresa y cerrar sesión.
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { iniciales } from './opcionesMenuUsuario'
import MenuUsuario from './MenuUsuario'

describe('MenuUsuario (menú lateral)', () => {
  it('muestra las opciones agrupadas por sección y marca la actual', () => {
    render(<MenuUsuario actual="calendario" alCambiar={vi.fn()} empresa="Panadería Doña Rosa" alSalir={vi.fn()} />)

    for (const titulo of ['Crear', 'Planificar', 'Mi empresa']) {
      expect(screen.getByText(titulo)).toBeInTheDocument()
    }
    expect(screen.getByRole('button', { name: 'Calendario' })).toHaveAttribute('aria-current', 'page')
    expect(screen.getByRole('button', { name: 'Crear anuncio' })).not.toHaveAttribute('aria-current')
    expect(screen.getByText('PD')).toBeInTheDocument()
    expect(screen.getByText('Panadería Doña Rosa')).toBeInTheDocument()
  })

  it('al hacer clic cambia de pantalla y "Cerrar sesión" avisa a App', async () => {
    const alCambiar = vi.fn()
    const alSalir = vi.fn()
    render(<MenuUsuario actual="anuncio" alCambiar={alCambiar} empresa="Bodega" alSalir={alSalir} />)

    await userEvent.click(screen.getByRole('button', { name: 'Kit de Marca' }))
    await userEvent.click(screen.getByRole('button', { name: 'Cerrar sesión' }))

    expect(alCambiar).toHaveBeenCalledWith('kit')
    expect(alSalir).toHaveBeenCalled()
  })

  it('las iniciales usan las dos primeras palabras de la empresa', () => {
    expect(iniciales('Panadería Doña Rosa')).toBe('PD')
    expect(iniciales('bodega')).toBe('B')
    expect(iniciales('')).toBe('A')
  })

  it('en celular el botón ☰ abre y cierra el menú, y avisa si hay novedades dentro', async () => {
    render(<MenuUsuario actual="inicio" alCambiar={vi.fn()} empresa="Bodega" alSalir={vi.fn()} contadores={{ misAnuncios: 2 }} />)
    const boton = screen.getByRole('button', { name: 'Abrir menú' })

    expect(boton).toHaveAttribute('aria-expanded', 'false')
    expect(document.querySelector('.menu-hamburguesa-aviso')).toBeInTheDocument()

    await userEvent.click(boton)
    expect(screen.getByRole('button', { name: 'Cerrar menú' })).toHaveAttribute('aria-expanded', 'true')

    await userEvent.keyboard('{Escape}')
    expect(screen.getByRole('button', { name: 'Abrir menú' })).toHaveAttribute('aria-expanded', 'false')
  })

  it('al elegir una opción el menú se cierra solo', async () => {
    render(<MenuUsuario actual="inicio" alCambiar={vi.fn()} empresa="Bodega" alSalir={vi.fn()} />)

    await userEvent.click(screen.getByRole('button', { name: 'Abrir menú' }))
    await userEvent.click(screen.getByRole('button', { name: 'Calendario' }))

    expect(screen.getByRole('button', { name: 'Abrir menú' })).toHaveAttribute('aria-expanded', 'false')
  })
})
