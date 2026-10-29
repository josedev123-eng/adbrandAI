// HU 29: editor interactivo del logo con vista previa, sin necesitar Spring encendido.
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { guardarLogoKitMarca } from '../models/brandkitApi'
import EditarLogoView from './EditarLogoView'

vi.mock('../models/brandkitApi', async (importOriginal) => ({
  ...(await importOriginal()),
  guardarLogoKitMarca: vi.fn(),
}))

const KIT = {
  id: 7,
  nombreNegocio: 'Panadería Doña Rosa',
  sector: 'Panadería',
  paletaColores: '#C62828 - Rojo horno\n#FFF3E0 - Crema',
  tipografiaPrimaria: 'Bricolage Grotesque',
  logoDiseno: null,
}

function vistaPrevia() {
  return screen.getByRole('region', { name: 'Vista previa del logo' })
}

describe('EditarLogoView (HU-29)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('sin logo previo empieza con las iniciales del negocio y su primer color', () => {
    render(<EditarLogoView kit={KIT} alGuardar={vi.fn()} alCancelar={vi.fn()} />)

    expect(screen.getByLabelText('Texto o iniciales')).toHaveValue('PDR')
    expect(screen.getAllByRole('textbox')[1]).toHaveValue('#C62828')
    expect(vistaPrevia().querySelector('svg')).toHaveAccessibleName('Logo: PDR')
  })

  it('la vista previa cambia mientras se edita, antes de guardar (criterio 1)', () => {
    render(<EditarLogoView kit={KIT} alGuardar={vi.fn()} alCancelar={vi.fn()} />)

    fireEvent.change(screen.getByLabelText('Texto o iniciales'), { target: { value: 'ROSA' } })
    fireEvent.click(screen.getByLabelText('Cuadrado'))

    const logo = vistaPrevia().querySelector('svg')
    expect(logo).toHaveAccessibleName('Logo: ROSA')
    expect(logo.querySelector('rect')).toHaveAttribute('rx', '0')
    expect(guardarLogoKitMarca).not.toHaveBeenCalled()
  })

  it('un color mal escrito muestra el error y no llama a la API', async () => {
    render(<EditarLogoView kit={KIT} alGuardar={vi.fn()} alCancelar={vi.fn()} />)

    fireEvent.change(screen.getAllByRole('textbox')[1], { target: { value: 'rojo' } })
    fireEvent.click(screen.getByRole('button', { name: 'Guardar logo' }))

    expect(await screen.findByText('Escribe el color como #RRGGBB.')).toBeInTheDocument()
    expect(guardarLogoKitMarca).not.toHaveBeenCalled()
  })

  it('al guardar manda el diseño y devuelve el kit actualizado (criterio 2)', async () => {
    const actualizado = { ...KIT, logoDiseno: { texto: 'DR', forma: 'CIRCULO', colorFondo: '#C62828', colorTexto: '#FFFFFF', tipografia: 'Bricolage Grotesque' } }
    guardarLogoKitMarca.mockResolvedValue(actualizado)
    const alGuardar = vi.fn()
    render(<EditarLogoView kit={KIT} alGuardar={alGuardar} alCancelar={vi.fn()} />)

    fireEvent.change(screen.getByLabelText('Texto o iniciales'), { target: { value: '  DR ' } })
    fireEvent.click(screen.getByRole('button', { name: 'Guardar logo' }))

    await waitFor(() => expect(alGuardar).toHaveBeenCalledWith(actualizado))
    expect(guardarLogoKitMarca).toHaveBeenCalledWith(7, {
      texto: 'DR',
      forma: 'CIRCULO',
      colorFondo: '#C62828',
      colorTexto: '#FFFFFF',
      tipografia: 'Bricolage Grotesque',
    })
  })

  it('si el kit ya tiene logo, el editor parte de ese diseño', () => {
    const conLogo = { ...KIT, logoDiseno: { texto: 'ROSA', forma: 'REDONDEADO', colorFondo: '#2E7D32', colorTexto: '#FFFFFF', tipografia: 'Georgia' } }
    render(<EditarLogoView kit={conLogo} alGuardar={vi.fn()} alCancelar={vi.fn()} />)

    expect(screen.getByLabelText('Texto o iniciales')).toHaveValue('ROSA')
    expect(screen.getByLabelText('Redondeado')).toBeChecked()
    expect(screen.getByLabelText('Tipografía')).toHaveValue('Georgia')
  })

  it('si Spring rechaza un campo, muestra su mensaje', async () => {
    guardarLogoKitMarca.mockRejectedValue({ status: 400, campos: { texto: 'Máximo 20 caracteres.' } })
    render(<EditarLogoView kit={KIT} alGuardar={vi.fn()} alCancelar={vi.fn()} />)

    fireEvent.click(screen.getByRole('button', { name: 'Guardar logo' }))

    expect(await screen.findByText('Máximo 20 caracteres.')).toBeInTheDocument()
  })
})
