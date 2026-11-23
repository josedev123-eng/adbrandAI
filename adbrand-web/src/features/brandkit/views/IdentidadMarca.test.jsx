// Identidad de marca: elegir eslogan, subir el logo propio o elegir uno de la IA, y descargar el manual.
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { elegirEslogan, guardarLogoImagen, proponerLogos } from '../models/brandkitApi'
import IdentidadMarca from './IdentidadMarca'

vi.mock('../models/brandkitApi', () => ({
  elegirEslogan: vi.fn(),
  guardarLogoImagen: vi.fn(),
  proponerLogos: vi.fn(),
  descargarManualMarca: vi.fn(),
}))

const KIT = {
  id: 7,
  nombreNegocio: 'Panadería Rosa',
  eslogan: 'El sabor de tu barrio',
  esloganOpciones: ['El sabor de tu barrio', 'Pan con cariño', 'Recién hecho, siempre'],
  logoImagen: null,
}

describe('IdentidadMarca', () => {
  beforeEach(() => vi.clearAllMocks())

  it('muestra el eslogan elegido y deja cambiarlo por otra propuesta', async () => {
    const alActualizar = vi.fn()
    elegirEslogan.mockResolvedValue({ ...KIT, eslogan: 'Pan con cariño' })
    render(<IdentidadMarca kit={KIT} alActualizar={alActualizar} />)

    expect(screen.getByText('“El sabor de tu barrio”')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Pan con cariño' }))

    expect(elegirEslogan).toHaveBeenCalledWith(7, 'Pan con cariño')
    await waitFor(() => expect(alActualizar).toHaveBeenCalledWith(expect.objectContaining({ eslogan: 'Pan con cariño' })))
  })

  it('deja usar un eslogan propio', async () => {
    elegirEslogan.mockResolvedValue(KIT)
    render(<IdentidadMarca kit={KIT} alActualizar={vi.fn()} />)

    await userEvent.type(screen.getByLabelText('¿Prefieres el tuyo?'), 'La casa del buen pan')
    await userEvent.click(screen.getByRole('button', { name: 'Usar este' }))

    expect(elegirEslogan).toHaveBeenCalledWith(7, 'La casa del buen pan')
  })

  it('la IA propone 3 logos y se elige uno', async () => {
    proponerLogos.mockResolvedValue({ opciones: ['data:image/png;base64,A', 'data:image/png;base64,B', 'data:image/png;base64,C'] })
    guardarLogoImagen.mockResolvedValue({ ...KIT, logoImagen: 'data:image/png;base64,B' })
    render(<IdentidadMarca kit={KIT} alActualizar={vi.fn()} />)

    await userEvent.click(screen.getByRole('button', { name: '✦ Proponme 3 logos' }))
    expect(await screen.findAllByRole('img', { name: /Propuesta de logo/ })).toHaveLength(3)

    await userEvent.click(screen.getAllByRole('button', { name: 'Elegir' })[1])
    expect(guardarLogoImagen).toHaveBeenCalledWith(7, 'data:image/png;base64,B', 'IA')
  })

  it('no acepta un logo que no sea PNG o JPG', async () => {
    render(<IdentidadMarca kit={KIT} alActualizar={vi.fn()} />)
    const archivo = new File(['hola'], 'logo.gif', { type: 'image/gif' })

    await userEvent.upload(screen.getByLabelText('Subir mi logo'), archivo, { applyAccept: false })

    expect(await screen.findByText('Sube tu logo en PNG o JPG.')).toBeInTheDocument()
    expect(guardarLogoImagen).not.toHaveBeenCalled()
  })
})
