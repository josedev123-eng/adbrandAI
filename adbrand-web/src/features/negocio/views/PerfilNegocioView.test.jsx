// HU-09: con perfil guardado se ve el resumen; "Editar" abre el formulario y al guardar vuelve al resumen.
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { guardarPerfil, obtenerPerfil } from '../models/perfilNegocioApi'
import PerfilNegocioView from './PerfilNegocioView'

vi.mock('../models/perfilNegocioApi', async (importOriginal) => ({
  ...(await importOriginal()),
  obtenerPerfil: vi.fn(),
  guardarPerfil: vi.fn(),
}))

const PERFIL = {
  nombreComercial: 'Cafetería Don Pepe',
  rubro: 'Restaurante y comida',
  publicoObjetivo: 'Oficinistas del centro',
  tono: 'CERCANO',
  descripcion: 'Café y empanadas',
}

describe('PerfilNegocioView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    obtenerPerfil.mockResolvedValue(PERFIL)
  })

  it('con perfil guardado muestra el resumen y no el formulario', async () => {
    render(<PerfilNegocioView />)

    expect(await screen.findByRole('heading', { name: 'Cafetería Don Pepe' })).toBeInTheDocument()
    expect(screen.getByText('Oficinistas del centro')).toBeInTheDocument()
    expect(screen.queryByRole('form', { name: 'Editar perfil del negocio' })).not.toBeInTheDocument()
  })

  it('Editar abre el formulario y al guardar vuelve al resumen', async () => {
    guardarPerfil.mockResolvedValue({ ...PERFIL, publicoObjetivo: 'Estudiantes' })
    render(<PerfilNegocioView />)

    await userEvent.click(await screen.findByRole('button', { name: '✎ Editar' }))
    const publico = screen.getByLabelText(/Público objetivo/)
    await userEvent.clear(publico)
    await userEvent.type(publico, 'Estudiantes')
    await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }))

    expect(guardarPerfil).toHaveBeenCalledWith(expect.objectContaining({ publicoObjetivo: 'Estudiantes' }))
    expect(await screen.findByText('Perfil guardado. La IA usará estos datos en tus anuncios.')).toBeInTheDocument()
    expect(screen.queryByRole('form', { name: 'Editar perfil del negocio' })).not.toBeInTheDocument()
    expect(screen.getByText('Estudiantes')).toBeInTheDocument()
  })

  it('Cancelar vuelve al resumen sin guardar', async () => {
    render(<PerfilNegocioView />)

    await userEvent.click(await screen.findByRole('button', { name: '✎ Editar' }))
    await userEvent.click(screen.getByRole('button', { name: 'Cancelar' }))

    expect(guardarPerfil).not.toHaveBeenCalled()
    expect(screen.getByRole('heading', { name: 'Cafetería Don Pepe' })).toBeInTheDocument()
  })

  it('sin perfil guardado abre directo el formulario', async () => {
    obtenerPerfil.mockRejectedValue(Object.assign(new Error('No existe'), { status: 404 }))
    render(<PerfilNegocioView />)

    expect(await screen.findByRole('heading', { name: 'Completa el perfil de tu negocio' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Cancelar' })).not.toBeInTheDocument()
  })
})
