// Listo para publicar: texto con hashtags, enviar por WhatsApp y afiche con QR solo si hay celular.
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { descargarImagenFinal, descargarVideoAnuncio, prepararPublicacion } from '../models/anunciosApi'
import { conCondiciones, htmlAfiche } from '../viewmodels/useListoParaPublicar'
import ListoParaPublicar from './ListoParaPublicar'

vi.mock('../models/anunciosApi', () => ({
  prepararPublicacion: vi.fn(),
  descargarVideoAnuncio: vi.fn(),
  descargarImagenFinal: vi.fn(),
}))

const CON_WHATSAPP = {
  id: 5,
  textoParaPublicar: '¡2x1 en panetones!\n\n📲 Pide por WhatsApp: https://wa.me/51987654321\n\n#PanaderiaRosa #ComproLocal',
  hashtags: ['#PanaderiaRosa', '#ComproLocal'],
  enlaceWhatsapp: 'https://wa.me/51987654321',
  tieneWhatsapp: true,
}

describe('ListoParaPublicar', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    descargarImagenFinal.mockResolvedValue(new Blob(['jpg'], { type: 'image/jpeg' }))
    URL.createObjectURL = vi.fn(() => 'blob:imagen')
    URL.revokeObjectURL = vi.fn()
  })

  it('muestra el texto con hashtags y deja enviarlo por WhatsApp', async () => {
    prepararPublicacion.mockResolvedValue(CON_WHATSAPP)
    const abrir = vi.spyOn(window, 'open').mockReturnValue(null)
    render(<ListoParaPublicar anuncioId={5} />)

    await userEvent.click(screen.getByRole('button', { name: '📣 Listo para publicar' }))

    expect(prepararPublicacion).toHaveBeenCalledWith(5)
    expect(await screen.findByText('#PanaderiaRosa')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '🖨 Afiche con QR para tu local' })).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Enviar por WhatsApp' }))
    expect(abrir).toHaveBeenCalledWith(expect.stringMatching(/^https:\/\/wa\.me\/\?text=/), '_blank', 'noopener')
    abrir.mockRestore()
  })

  it('sin celular no ofrece el afiche y explica cómo activarlo', async () => {
    prepararPublicacion.mockResolvedValue({ ...CON_WHATSAPP, enlaceWhatsapp: null, tieneWhatsapp: false })
    const irAMiCuenta = vi.fn()
    render(<ListoParaPublicar anuncioId={5} irAMiCuenta={irAMiCuenta} />)

    await userEvent.click(screen.getByRole('button', { name: '📣 Listo para publicar' }))

    expect(await screen.findByText(/Agrega tu celular en/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Afiche con QR/ })).toBeNull()
    await userEvent.click(screen.getByRole('button', { name: 'Mi cuenta' }))
    expect(irAMiCuenta).toHaveBeenCalled()
  })

  it('HU 64: muestra lo que falta según INDECOPI y agrega la fecha y el stock al texto', async () => {
    prepararPublicacion.mockResolvedValue({
      ...CON_WHATSAPP,
      observaciones: [
        { codigo: 'FALTA_VIGENCIA', titulo: 'Di hasta cuándo vale la promoción', detalle: '…' },
        { codigo: 'FALTA_STOCK', titulo: 'Indica cuántas unidades hay', detalle: '…' },
      ],
    })
    render(<ListoParaPublicar anuncioId={5} />)
    await userEvent.click(screen.getByRole('button', { name: '📣 Listo para publicar' }))

    expect(await screen.findByText(/Tu oferta, sin multas/)).toBeInTheDocument()
    await userEvent.type(screen.getByLabelText('Unidades disponibles'), '50')

    expect(screen.getByText(/Hasta agotar stock \(50 unidades\)\./)).toBeInTheDocument()
  })

  it('HU 64: sin observaciones dice que cumple las reglas', async () => {
    prepararPublicacion.mockResolvedValue({ ...CON_WHATSAPP, observaciones: [] })
    render(<ListoParaPublicar anuncioId={5} />)
    await userEvent.click(screen.getByRole('button', { name: '📣 Listo para publicar' }))

    expect(await screen.findByText(/cumple las reglas básicas/)).toBeInTheDocument()
  })

  it('HU 64: las condiciones van después del texto y antes del enlace y los hashtags', () => {
    const texto = 'Promo 2x1\n\n📲 Pide por WhatsApp: wa.me/51987654321\n\n#Promo'
    expect(conCondiciones(texto, { hasta: '2026-12-24', stock: '50' })).toBe(
      'Promo 2x1\n\nVálido hasta el 24/12/2026 o hasta agotar stock (50 unidades).\n\n📲 Pide por WhatsApp: wa.me/51987654321\n\n#Promo',
    )
    expect(conCondiciones(texto, { hasta: '', stock: '' })).toBe(texto)
  })

  it('HU 65: descarga el video del anuncio para Reels y Estados', async () => {
    prepararPublicacion.mockResolvedValue({ ...CON_WHATSAPP, observaciones: [] })
    descargarVideoAnuncio.mockResolvedValue(new Blob(['mp4'], { type: 'video/mp4' }))
    render(<ListoParaPublicar anuncioId={5} />)
    await userEvent.click(screen.getByRole('button', { name: '📣 Listo para publicar' }))

    await userEvent.click(await screen.findByRole('button', { name: '🎬 Video para Reels y Estados' }))

    expect(descargarVideoAnuncio).toHaveBeenCalledWith(5)
    expect(URL.createObjectURL).toHaveBeenCalled()
  })

  it('HU 61: muestra la imagen con la marca y la pide en el formato elegido', async () => {
    prepararPublicacion.mockResolvedValue({ ...CON_WHATSAPP, observaciones: [] })
    render(<ListoParaPublicar anuncioId={5} />)
    await userEvent.click(screen.getByRole('button', { name: '📣 Listo para publicar' }))

    expect(await screen.findByAltText(/con tu logo, tus colores y tu eslogan/)).toHaveAttribute('src', 'blob:imagen')
    expect(descargarImagenFinal).toHaveBeenCalledWith(5, 'CUADRADO')

    await userEvent.click(screen.getByRole('button', { name: /Historia o estado/ }))
    expect(descargarImagenFinal).toHaveBeenCalledWith(5, 'VERTICAL')
    expect(await screen.findByRole('button', { name: 'Descargar imagen' })).toBeEnabled()
  })

  it('HU 61: el afiche lleva el logo, los colores y el eslogan del kit', () => {
    const html = htmlAfiche({
      marca: { nombre: 'Panadería Rosa', eslogan: 'El sabor de tu barrio', logo: 'data:image/png;base64,AAA', colorPrincipal: '#8B4513', colorAcento: '#F2C14E' },
      imagen: 'data:image/jpeg;base64,BBB',
      texto: 'Promo 2x1',
      qr: 'data:image/png;base64,CCC',
    })
    expect(html).toContain('color: #8B4513')
    expect(html).toContain('solid #F2C14E')
    expect(html).toContain('src="data:image/png;base64,AAA"')
    expect(html).toContain('“El sabor de tu barrio”')
  })

  it('HU 61: un color raro del kit no se mete en el afiche', () => {
    const html = htmlAfiche({ marca: { nombre: 'X', colorPrincipal: 'red;}</style>', logo: 'javascript:alert(1)' }, texto: '', qr: '' })
    expect(html).toContain('color: #4f46e5')
    expect(html).not.toContain('javascript:')
  })

  it('HU 61: las condiciones van antes de la firma con el eslogan', () => {
    const texto = 'Promo 2x1\n\n— Panadería Rosa: “El sabor de tu barrio”\n\n#Promo'
    expect(conCondiciones(texto, { hasta: '', stock: '20' })).toBe(
      'Promo 2x1\n\nHasta agotar stock (20 unidades).\n\n— Panadería Rosa: “El sabor de tu barrio”\n\n#Promo',
    )
  })
})
