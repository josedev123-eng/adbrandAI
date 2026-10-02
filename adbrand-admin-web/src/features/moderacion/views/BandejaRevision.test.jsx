// HU-14, tarea 4: prueba el flujo de revisión manual como lo haría un moderador.
// La API se reemplaza por datos de prueba, así no se necesita Django encendido.
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { listarDudosos, obtenerContenido } from '../models/moderacionApi'
import BandejaRevisionView from './BandejaRevisionView'

vi.mock('../models/moderacionApi', async (importOriginal) => ({
  ...(await importOriginal()),
  listarDudosos: vi.fn(),
  obtenerContenido: vi.fn(),
}))

const CIGARRILLOS = {
  id: 2,
  tipo: 'ANUNCIO',
  red_social: 'INSTAGRAM',
  oferta: 'Cigarrillos importados a S/ 10',
  extracto: '¡Esto no te lo puedes perder! Cigarrillos importados a S/ 10...',
  motivo_revision: '"cigarrillos": Publicidad de tabaco.',
  fecha_creacion: '2026-10-04T15:32:08-05:00',
}

const TE_MILAGROSO = {
  id: 5,
  tipo: 'ANUNCIO',
  red_social: 'FACEBOOK',
  oferta: 'Té que cura el cáncer',
  extracto: 'Nuestro té cura el cáncer...',
  motivo_revision: '"cura el cáncer": Promete una cura médica.',
  fecha_creacion: '2026-10-04T16:10:00-05:00',
}

const DETALLE_CIGARRILLOS = {
  ...CIGARRILLOS,
  estado: 'DUDOSO',
  tono: 'CERCANO',
  texto: '¡Esto no te lo puedes perder! Cigarrillos importados a S/ 10.\nVen a visitarnos.',
  negocio: { nombre: 'Panadería Doña Rosa', rubro: 'Panadería' },
}

describe('Bandeja de revisión manual (HU-14)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('criterio 1: muestra los contenidos dudosos con su motivo y el total', async () => {
    listarDudosos.mockResolvedValue([CIGARRILLOS, TE_MILAGROSO])

    render(<BandejaRevisionView />)

    expect(await screen.findByText('Cigarrillos importados a S/ 10')).toBeInTheDocument()
    expect(screen.getByText('Té que cura el cáncer')).toBeInTheDocument()
    expect(screen.getByText('"cigarrillos": Publicidad de tabaco.')).toBeInTheDocument()
    expect(screen.getAllByText('Dudoso')).toHaveLength(2)
    expect(screen.getByText('2')).toBeInTheDocument()
  })

  it('si no hay dudosos avisa que la bandeja está vacía', async () => {
    listarDudosos.mockResolvedValue([])

    render(<BandejaRevisionView />)

    expect(await screen.findByText(/No hay contenidos pendientes/)).toBeInTheDocument()
  })

  it('si Django no responde muestra un mensaje claro', async () => {
    listarDudosos.mockRejectedValue(new Error('sin conexión'))

    render(<BandejaRevisionView />)

    expect(await screen.findByText(/No se pudo cargar la bandeja/)).toBeInTheDocument()
  })

  it('criterio 2: al abrir un contenido se ve el texto completo, el motivo y el negocio', async () => {
    listarDudosos.mockResolvedValue([CIGARRILLOS, TE_MILAGROSO])
    obtenerContenido.mockResolvedValue(DETALLE_CIGARRILLOS)
    const usuario = userEvent.setup()

    render(<BandejaRevisionView />)
    await usuario.click(await screen.findByRole('button', { name: /Cigarrillos importados/ }))

    expect(obtenerContenido).toHaveBeenCalledWith(2)
    expect(await screen.findByText('Motivo de la observación')).toBeInTheDocument()
    expect(screen.getByText('"cigarrillos": Publicidad de tabaco.')).toBeInTheDocument()
    expect(screen.getByText(/Ven a visitarnos/)).toBeInTheDocument()
    expect(screen.getByText('Panadería Doña Rosa (Panadería)')).toBeInTheDocument()
    expect(screen.getByText('Cercano')).toBeInTheDocument()
    // Mientras se ve el detalle, la lista no se muestra.
    expect(screen.queryByText('Té que cura el cáncer')).not.toBeInTheDocument()
  })

  it('desde el detalle se puede volver a la bandeja', async () => {
    listarDudosos.mockResolvedValue([CIGARRILLOS, TE_MILAGROSO])
    obtenerContenido.mockResolvedValue(DETALLE_CIGARRILLOS)
    const usuario = userEvent.setup()

    render(<BandejaRevisionView />)
    await usuario.click(await screen.findByRole('button', { name: /Cigarrillos importados/ }))
    await usuario.click(await screen.findByRole('button', { name: /Volver a la bandeja/ }))

    expect(screen.getByText('Pendientes de revisión')).toBeInTheDocument()
    expect(screen.getByText('Té que cura el cáncer')).toBeInTheDocument()
  })

  it('si el contenido ya no existe lo dice en el detalle', async () => {
    listarDudosos.mockResolvedValue([CIGARRILLOS])
    obtenerContenido.mockRejectedValue(Object.assign(new Error('no existe'), { status: 404 }))
    const usuario = userEvent.setup()

    render(<BandejaRevisionView />)
    await usuario.click(await screen.findByRole('button', { name: /Cigarrillos importados/ }))

    expect(await screen.findByText('Este contenido ya no existe.')).toBeInTheDocument()
  })
})
