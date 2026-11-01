// HU 31: editor de tipografías y voz de marca con vista previa, sin necesitar Spring encendido.
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { guardarIdentidadKitMarca } from '../models/brandkitApi'
import EditarIdentidadView from './EditarIdentidadView'

vi.mock('../models/brandkitApi', async (importOriginal) => ({
  ...(await importOriginal()),
  guardarIdentidadKitMarca: vi.fn(),
}))

const KIT = {
  id: 7,
  nombreNegocio: 'Panadería Doña Rosa',
  paletaColores: '#C62828 - Rojo horno\n#263238 - Gris',
  tipografiaPrimaria: 'Bricolage Grotesque',
  tipografiaSecundaria: null,
  vozMarca: 'Cercana, usa tú.',
  logoDiseno: null,
}

function vistaPrevia() {
  return screen.getByRole('region', { name: 'Vista previa de la identidad' })
}

describe('EditarIdentidadView (HU-31)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('empieza con la identidad que el kit ya tiene', () => {
    render(<EditarIdentidadView kit={KIT} alGuardar={vi.fn()} alCancelar={vi.fn()} />)

    expect(screen.getByLabelText('Tipografía principal (títulos)')).toHaveValue('Bricolage Grotesque')
    expect(screen.getByLabelText('Tipografía secundaria (textos)')).toHaveValue('')
    expect(screen.getByLabelText('Voz de marca')).toHaveValue('Cercana, usa tú.')
  })

  it('la vista previa cambia mientras se edita, antes de guardar (criterio 1)', () => {
    render(<EditarIdentidadView kit={KIT} alGuardar={vi.fn()} alCancelar={vi.fn()} />)

    fireEvent.change(screen.getByLabelText('Tipografía principal (títulos)'), { target: { value: 'Georgia' } })
    fireEvent.change(screen.getByLabelText('Voz de marca'), { target: { value: 'Elegante, trata de usted.' } })

    expect(screen.getByText(/Oferta de la semana/)).toHaveStyle({ fontFamily: "'Georgia', sans-serif" })
    expect(vistaPrevia()).toHaveTextContent('Elegante, trata de usted.')
    expect(guardarIdentidadKitMarca).not.toHaveBeenCalled()
  })

  it('guarda la identidad y avisa al kit con la versión nueva', async () => {
    const actualizado = { ...KIT, tipografiaPrimaria: 'Georgia', vozMarca: 'Elegante, trata de usted.' }
    guardarIdentidadKitMarca.mockResolvedValue(actualizado)
    const alGuardar = vi.fn()
    render(<EditarIdentidadView kit={KIT} alGuardar={alGuardar} alCancelar={vi.fn()} />)

    fireEvent.change(screen.getByLabelText('Tipografía principal (títulos)'), { target: { value: ' Georgia ' } })
    fireEvent.change(screen.getByLabelText('Voz de marca'), { target: { value: 'Elegante, trata de usted.' } })
    fireEvent.click(screen.getByRole('button', { name: 'Guardar identidad' }))

    await waitFor(() => expect(alGuardar).toHaveBeenCalledWith(actualizado))
    expect(guardarIdentidadKitMarca).toHaveBeenCalledWith(7, {
      tipografiaPrimaria: 'Georgia',
      tipografiaSecundaria: null,
      vozMarca: 'Elegante, trata de usted.',
    })
  })

  it('sin voz de marca no llama a Spring y muestra el error', () => {
    render(<EditarIdentidadView kit={KIT} alGuardar={vi.fn()} alCancelar={vi.fn()} />)

    fireEvent.change(screen.getByLabelText('Voz de marca'), { target: { value: '   ' } })
    fireEvent.click(screen.getByRole('button', { name: 'Guardar identidad' }))

    expect(screen.getByText('Describe la voz de tu marca.')).toBeInTheDocument()
    expect(guardarIdentidadKitMarca).not.toHaveBeenCalled()
  })
})
