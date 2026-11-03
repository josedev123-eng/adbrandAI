// HU 30: editor de colores de la marca, sin necesitar Spring encendido.
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { guardarColoresKitMarca } from '../models/brandkitApi'
import EditarColoresView from './EditarColoresView'

vi.mock('../models/brandkitApi', async (importOriginal) => ({
  ...(await importOriginal()),
  guardarColoresKitMarca: vi.fn(),
  listarVersionesKitMarca: vi.fn().mockResolvedValue([]),
}))

const KIT = {
  id: 7,
  nombreNegocio: 'Panadería Doña Rosa',
  paletaColores: '#C62828 - Rojo horno\n#FFF3E0 - Crema',
}

describe('EditarColoresView (HU-30)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('carga la paleta del kit con un selector por color', () => {
    render(<EditarColoresView kit={KIT} alGuardar={vi.fn()} alCancelar={vi.fn()} />)

    expect(screen.getByLabelText('Código del color 1')).toHaveValue('#C62828')
    expect(screen.getByLabelText('Nombre o uso del color 2')).toHaveValue('Crema')
    expect(screen.getByLabelText('Elegir color 1')).toHaveValue('#c62828')
  })

  it('el selector cambia el código del color', () => {
    render(<EditarColoresView kit={KIT} alGuardar={vi.fn()} alCancelar={vi.fn()} />)

    fireEvent.change(screen.getByLabelText('Elegir color 2'), { target: { value: '#263238' } })

    expect(screen.getByLabelText('Código del color 2')).toHaveValue('#263238')
  })

  it('un color mal escrito o repetido no se guarda (criterio 2)', async () => {
    render(<EditarColoresView kit={KIT} alGuardar={vi.fn()} alCancelar={vi.fn()} />)

    fireEvent.change(screen.getByLabelText('Código del color 2'), { target: { value: 'crema' } })
    fireEvent.click(screen.getByRole('button', { name: 'Guardar colores' }))
    expect(await screen.findByText('Escribe el color como #RRGGBB.')).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Código del color 2'), { target: { value: '#c62828' } })
    fireEvent.click(screen.getByRole('button', { name: 'Guardar colores' }))
    expect(await screen.findByText('El color #C62828 está repetido.')).toBeInTheDocument()

    expect(guardarColoresKitMarca).not.toHaveBeenCalled()
  })

  it('agrega y quita colores, hasta un máximo de 8', () => {
    render(<EditarColoresView kit={KIT} alGuardar={vi.fn()} alCancelar={vi.fn()} />)

    for (let i = 0; i < 6; i++) fireEvent.click(screen.getByRole('button', { name: '+ Agregar color' }))
    expect(screen.getByLabelText('Código del color 8')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: '+ Agregar color' })).toBeNull()

    fireEvent.click(screen.getByLabelText('Quitar color 8'))
    expect(screen.queryByLabelText('Código del color 8')).toBeNull()
  })

  it('al guardar manda la paleta limpia y devuelve el kit actualizado', async () => {
    const actualizado = { ...KIT, paletaColores: '#C62828 - Rojo horno\n#263238' }
    guardarColoresKitMarca.mockResolvedValue(actualizado)
    const alGuardar = vi.fn()
    render(<EditarColoresView kit={KIT} alGuardar={alGuardar} alCancelar={vi.fn()} />)

    fireEvent.change(screen.getByLabelText('Código del color 2'), { target: { value: '#263238' } })
    fireEvent.change(screen.getByLabelText('Nombre o uso del color 2'), { target: { value: '  ' } })
    fireEvent.click(screen.getByRole('button', { name: 'Guardar colores' }))

    await waitFor(() => expect(alGuardar).toHaveBeenCalledWith(actualizado))
    expect(guardarColoresKitMarca).toHaveBeenCalledWith(7, [
      { hex: '#C62828', nombre: 'Rojo horno' },
      { hex: '#263238', nombre: '' },
    ])
  })
})
