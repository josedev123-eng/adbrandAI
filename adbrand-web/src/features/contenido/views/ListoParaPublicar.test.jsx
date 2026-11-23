// Listo para publicar: texto con hashtags, enviar por WhatsApp y afiche con QR solo si hay celular.
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { prepararPublicacion } from '../models/anunciosApi'
import ListoParaPublicar from './ListoParaPublicar'

vi.mock('../models/anunciosApi', () => ({ prepararPublicacion: vi.fn() }))

const CON_WHATSAPP = {
  id: 5,
  textoParaPublicar: '¡2x1 en panetones!\n\n📲 Pide por WhatsApp: https://wa.me/51987654321\n\n#PanaderiaRosa #ComproLocal',
  hashtags: ['#PanaderiaRosa', '#ComproLocal'],
  enlaceWhatsapp: 'https://wa.me/51987654321',
  tieneWhatsapp: true,
}

describe('ListoParaPublicar', () => {
  beforeEach(() => vi.clearAllMocks())

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
})
