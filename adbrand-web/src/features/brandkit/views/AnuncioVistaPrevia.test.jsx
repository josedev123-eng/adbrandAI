// HU 30, criterio 1: la vista previa del anuncio usa los colores de la marca y cambia mientras se editan.
import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { textoLegible } from '../models/brandkitApi'
import AnuncioVistaPrevia from './AnuncioVistaPrevia'
import EditarColoresView from './EditarColoresView'

vi.mock('../models/brandkitApi', async (importOriginal) => ({
  ...(await importOriginal()),
  guardarColoresKitMarca: vi.fn(),
}))

function anuncio() {
  return screen.getByRole('article', { name: 'Anuncio de ejemplo con tus colores' })
}

describe('AnuncioVistaPrevia (HU-30)', () => {
  it('pinta el fondo, el botón y el detalle con los 3 primeros colores', () => {
    render(<AnuncioVistaPrevia colores={['#C62828', '#263238', '#FFC107']} nombreNegocio="Panadería Doña Rosa" />)

    expect(anuncio()).toHaveStyle({ backgroundColor: '#C62828' })
    expect(screen.getByText('Ver oferta')).toHaveStyle({ backgroundColor: '#263238' })
    expect(screen.getByText('20% dscto.')).toHaveStyle({ backgroundColor: '#FFC107' })
    expect(anuncio()).toHaveTextContent('Panadería Doña Rosa')
  })

  it('con un solo color lo usa en todo el anuncio', () => {
    render(<AnuncioVistaPrevia colores={['#2E7D32']} nombreNegocio="Bodega" />)

    expect(screen.getByText('Ver oferta')).toHaveStyle({ backgroundColor: '#2E7D32' })
    expect(screen.getByText('20% dscto.')).toHaveStyle({ backgroundColor: '#2E7D32' })
  })

  it('sin colores válidos pide elegir uno', () => {
    render(<AnuncioVistaPrevia colores={[]} nombreNegocio="Bodega" />)

    expect(screen.getByText('Elige al menos un color válido para ver el anuncio.')).toBeInTheDocument()
  })

  it('elige texto blanco sobre colores oscuros y negro sobre claros, para que se lea', () => {
    expect(textoLegible('#263238')).toBe('#FFFFFF')
    expect(textoLegible('#C62828')).toBe('#FFFFFF')
    expect(textoLegible('#FFF3E0')).toBe('#1A1A1A')
    expect(textoLegible('#FFC107')).toBe('#1A1A1A')
  })

  it('en el editor, el anuncio cambia mientras se cambian los colores', () => {
    const kit = { id: 7, nombreNegocio: 'Panadería Doña Rosa', paletaColores: '#C62828 - Rojo\n#263238 - Gris' }
    render(<EditarColoresView kit={kit} alGuardar={vi.fn()} alCancelar={vi.fn()} />)
    expect(anuncio()).toHaveStyle({ backgroundColor: '#C62828' })

    fireEvent.change(screen.getByLabelText('Elegir color 1'), { target: { value: '#1565c0' } })

    expect(anuncio()).toHaveStyle({ backgroundColor: '#1565C0' })
  })
})
