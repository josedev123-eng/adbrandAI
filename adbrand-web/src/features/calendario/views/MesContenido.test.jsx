// HU 63: "Tu mes de contenido". Fechas con idea según el rubro, plan semanal y "Crear anuncio para esta fecha".
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { obtenerMesContenido } from '../models/mesContenidoApi'
import MesContenido from './MesContenido'

vi.mock('../models/mesContenidoApi', () => ({ obtenerMesContenido: vi.fn() }))

const DICIEMBRE = {
  anio: 2026,
  mes: 12,
  rubro: 'Panadería y pastelería',
  fechas: [
    { fecha: '2026-12-25', nombre: 'Navidad', idea: 'Panetones y chocolate caliente para la Nochebuena.', diasFaltan: 24 },
    { fecha: '2026-11-27', nombre: 'Black Friday', idea: 'Descuentos', diasFaltan: -4 },
  ],
  plan: [
    { fecha: '2026-12-01', tipo: 'OFERTA', titulo: 'Oferta de la semana', idea: 'Una promoción concreta.' },
    { fecha: '2026-12-25', tipo: 'FECHA_ESPECIAL', titulo: 'Navidad', idea: 'Panetones…' },
  ],
}

describe('MesContenido', () => {
  beforeEach(() => vi.clearAllMocks())

  it('muestra las fechas con su idea y el plan de publicaciones', async () => {
    obtenerMesContenido.mockResolvedValue(DICIEMBRE)
    render(<MesContenido alCrearAnuncio={vi.fn()} />)

    expect(await screen.findByText('Navidad', { selector: 'strong' })).toBeInTheDocument()
    expect(screen.getByText('Panetones y chocolate caliente para la Nochebuena.')).toBeInTheDocument()
    expect(screen.getByText('Faltan 24 días')).toBeInTheDocument()
    expect(screen.getAllByText('Oferta').length).toBeGreaterThan(0)
    // La leyenda explica qué es cada tipo de publicación.
    expect(screen.getByRole('list', { name: 'Qué es cada tipo de publicación' })).toHaveTextContent('Detrás de cámaras')
    expect(screen.getByText(/Genera confianza/)).toBeInTheDocument()
  })

  it('crear anuncio para una fecha manda su idea; las fechas pasadas no tienen el botón', async () => {
    obtenerMesContenido.mockResolvedValue(DICIEMBRE)
    const alCrearAnuncio = vi.fn()
    render(<MesContenido alCrearAnuncio={alCrearAnuncio} />)

    const botones = await screen.findAllByRole('button', { name: '✦ Crear anuncio para esta fecha' })
    expect(botones).toHaveLength(1)
    await userEvent.click(botones[0])

    expect(alCrearAnuncio).toHaveBeenCalledWith('Panetones y chocolate caliente para la Nochebuena.', 'Navidad')
  })

  it('cambia de mes con las flechas', async () => {
    obtenerMesContenido.mockResolvedValue(DICIEMBRE)
    render(<MesContenido />)
    await screen.findByText('Navidad', { selector: 'strong' })

    await userEvent.click(screen.getByRole('button', { name: 'Mes siguiente' }))

    expect(obtenerMesContenido).toHaveBeenCalledTimes(2)
  })
})
