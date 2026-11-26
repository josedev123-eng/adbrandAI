// HU 66: video con IA. Solo Premium (con candado si no), cuántos quedan, y se descarga solo cuando la IA termina.
import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { consultarVideoIa, descargarVideoIa, disponibilidadVideoIa, iniciarVideoIa } from '../models/anunciosApi'
import { CADA_CUANTO_MS } from '../viewmodels/useVideoConIa'
import VideoConIa from './VideoConIa'

vi.mock('../models/anunciosApi', async (importOriginal) => ({
  ...(await importOriginal()),
  disponibilidadVideoIa: vi.fn(),
  iniciarVideoIa: vi.fn(),
  consultarVideoIa: vi.fn(),
  descargarVideoIa: vi.fn(),
}))

describe('VideoConIa', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    URL.createObjectURL = vi.fn(() => 'blob:video')
    URL.revokeObjectURL = vi.fn()
  })
  afterEach(() => vi.useRealTimers())

  it('sin el plan Premium sale con candado y lleva a Planes', async () => {
    disponibilidadVideoIa.mockResolvedValue({ disponible: false, motivo: 'PLAN', usados: 0, limite: 3 })
    const irAPlanes = vi.fn()
    render(<VideoConIa anuncioId={5} irAPlanes={irAPlanes} />)

    await userEvent.click(await screen.findByRole('button', { name: /Video con IA · plan Premium/ }))
    expect(irAPlanes).toHaveBeenCalled()
    expect(iniciarVideoIa).not.toHaveBeenCalled()
  })

  it('con los 3 del mes usados no deja pedir otro', async () => {
    disponibilidadVideoIa.mockResolvedValue({ disponible: false, motivo: 'LIMITE', usados: 3, limite: 3 })
    render(<VideoConIa anuncioId={5} />)

    expect(await screen.findByRole('button', { name: '✨ Video con IA' })).toBeDisabled()
    expect(screen.getByText(/Ya usaste tus 3 videos con IA/)).toBeInTheDocument()
  })

  it('pide el video, pregunta en qué va y lo descarga al terminar', async () => {
    disponibilidadVideoIa.mockResolvedValue({ disponible: true, motivo: null, usados: 1, limite: 3 })
    iniciarVideoIa.mockResolvedValue({ id: 40, estado: 'CREANDO', progreso: 10, usados: 2, limite: 3 })
    consultarVideoIa.mockResolvedValue({ id: 40, estado: 'LISTO', progreso: 100, usados: 2, limite: 3 })
    descargarVideoIa.mockResolvedValue(new Blob(['mp4'], { type: 'video/mp4' }))
    const clic = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
    render(<VideoConIa anuncioId={5} />)

    expect(await screen.findByText(/Te quedan 2 de 3/)).toBeInTheDocument()
    vi.useFakeTimers({ shouldAdvanceTime: true })
    await userEvent.click(screen.getByRole('button', { name: '✨ Video con IA' }))

    expect(iniciarVideoIa).toHaveBeenCalledWith(5)
    expect(screen.getByRole('button', { name: /La IA está creando tu video/ })).toBeDisabled()
    await act(() => vi.advanceTimersByTimeAsync(CADA_CUANTO_MS))

    expect(consultarVideoIa).toHaveBeenCalledWith(40)
    expect(descargarVideoIa).toHaveBeenCalledWith(40)
    expect(await screen.findByText(/Tu video con IA se descargó/)).toBeInTheDocument()
    expect(clic).toHaveBeenCalled()
    clic.mockRestore()
  })

  it('si la IA falla lo explica', async () => {
    disponibilidadVideoIa.mockResolvedValue({ disponible: true, motivo: null, usados: 0, limite: 3 })
    iniciarVideoIa.mockRejectedValue(new Error('No se pudo pedir el video a la IA. Inténtalo en unos minutos.'))
    render(<VideoConIa anuncioId={5} />)

    await userEvent.click(await screen.findByRole('button', { name: '✨ Video con IA' }))
    expect(await screen.findByText(/No se pudo pedir el video a la IA/)).toBeInTheDocument()
  })
})
