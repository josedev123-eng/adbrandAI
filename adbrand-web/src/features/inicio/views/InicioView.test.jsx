// Pantalla de Inicio de la PYME: tarjetas con anuncios, kit, próximas publicaciones y accesos rápidos.
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { listarKitsMarca } from '../../brandkit/models/brandkitApi'
import { obtenerCalendario } from '../../calendario/models/calendarioApi'
import { obtenerPerfil } from '../../negocio/models/perfilNegocioApi'
import { obtenerInformeCampanas } from '../../reportes/models/reportesApi'
import { obtenerConsumoMes } from '../../pagos/models/pagosApi'
import { proximasPublicaciones } from '../viewmodels/useInicio'
import InicioView from './InicioView'

vi.mock('../../brandkit/models/brandkitApi', async (importOriginal) => ({ ...(await importOriginal()), listarKitsMarca: vi.fn() }))
vi.mock('../../calendario/models/calendarioApi', async (importOriginal) => ({ ...(await importOriginal()), obtenerCalendario: vi.fn() }))
vi.mock('../../negocio/models/perfilNegocioApi', async (importOriginal) => ({ ...(await importOriginal()), obtenerPerfil: vi.fn() }))
vi.mock('../../pagos/models/pagosApi', async (importOriginal) => ({ ...(await importOriginal()), obtenerConsumoMes: vi.fn() }))
vi.mock('../../reportes/models/reportesApi', async (importOriginal) => ({ ...(await importOriginal()), obtenerInformeCampanas: vi.fn() }))

function tarjeta(titulo) {
  return screen.getByRole('region', { name: titulo })
}

describe('InicioView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    obtenerPerfil.mockResolvedValue({ id: 1, nombreComercial: 'Panadería Doña Rosa' })
    obtenerInformeCampanas.mockResolvedValue({
      total: 12,
      aprobados: 10,
      dudosos: 2,
      rechazados: 0,
      campanas: [{ id: 9, fecha: '2026-11-20', redSocial: 'INSTAGRAM', oferta: '2x1 en pan de yema', estado: 'APROBADO' }],
    })
    listarKitsMarca.mockResolvedValue([
      { id: 3, nombreNegocio: 'Panadería Doña Rosa', paletaColores: '#D97724 - Ámbar\n#6E3B1E - Marrón' },
    ])
    obtenerConsumoMes.mockResolvedValue({ tokensUsados: 8500, limite: 10000, porcentaje: 85, origen: 'GRATUITO', plan: null, enAlerta: true, agotado: false })
    obtenerCalendario.mockResolvedValue([
      { id: 1, titulo: 'Pan de yema 2x1', redSocial: 'INSTAGRAM', fecha: '2099-12-01', hora: '10:00', estado: 'PROGRAMADA', campanaColor: '#C62828' },
    ])
  })

  it('muestra las cifras de los anuncios, los colores del kit y las próximas publicaciones', async () => {
    render(<InicioView empresa="Panadería Doña Rosa" alAbrir={vi.fn()} />)

    expect(screen.getByRole('heading', { name: 'Hola, Panadería Doña Rosa' })).toBeInTheDocument()
    expect(await within(tarjeta('Tus anuncios')).findByText('12')).toBeInTheDocument()
    expect(within(tarjeta('Tus anuncios')).getByText('2x1 en pan de yema')).toBeInTheDocument()
    expect(await within(tarjeta('Tu kit de marca')).findByLabelText('Colores de tu marca')).toBeInTheDocument()
    expect(await within(tarjeta('Próximas publicaciones')).findAllByText('Pan de yema 2x1')).not.toHaveLength(0)
  })

  it('cada tarjeta lleva a su pantalla', async () => {
    const alAbrir = vi.fn()
    render(<InicioView empresa="Bodega" alAbrir={alAbrir} />)

    await userEvent.click(screen.getByRole('button', { name: '✦ Crear anuncio' }))
    await userEvent.click(await within(tarjeta('Próximas publicaciones')).findByRole('button', { name: 'Ir al calendario →' }))

    expect(alAbrir).toHaveBeenCalledWith('anuncio')
    expect(alAbrir).toHaveBeenCalledWith('calendario')
  })

  it('sin perfil ni kit invita a completarlos', async () => {
    obtenerPerfil.mockRejectedValue(Object.assign(new Error('Sin perfil'), { status: 404, codigo: 'PERFIL_NO_ENCONTRADO' }))
    listarKitsMarca.mockResolvedValue([])
    render(<InicioView empresa="Bodega" alAbrir={vi.fn()} />)

    expect(await screen.findByRole('button', { name: 'Completar mi perfil' })).toBeInTheDocument()
    expect(await within(tarjeta('Tu kit de marca')).findByRole('button', { name: 'Crear kit →' })).toBeInTheDocument()
  })

  it('si una tarjeta falla, las demás se muestran igual', async () => {
    obtenerInformeCampanas.mockRejectedValue(Object.assign(new Error('caído'), { status: 500 }))
    render(<InicioView empresa="Bodega" alAbrir={vi.fn()} />)

    expect(await within(tarjeta('Tus anuncios')).findByText(/No se pudo cargar/)).toBeInTheDocument()
    expect(await within(tarjeta('Tu kit de marca')).findByLabelText('Colores de tu marca')).toBeInTheDocument()
  })

  it('las próximas publicaciones son de hoy en adelante, en orden y sin las ya publicadas', () => {
    const lista = [
      { id: 1, fecha: '2026-11-25', hora: '18:00', estado: 'PROGRAMADA' },
      { id: 2, fecha: '2026-11-20', hora: '09:00', estado: 'PROGRAMADA' },
      { id: 3, fecha: '2026-11-22', hora: '10:00', estado: 'PUBLICADA' },
      { id: 4, fecha: '2026-11-22', hora: '08:00', estado: 'PROGRAMADA' },
    ]

    expect(proximasPublicaciones(lista, '2026-11-22').map((p) => p.id)).toEqual([4, 1])
  })

  it('muestra el consumo de IA del mes con su plan y avisa al acercarse al límite', async () => {
    render(<InicioView empresa="Panadería Doña Rosa" alAbrir={() => {}} />)

    const consumo = await screen.findByRole('progressbar', { name: 'Tokens de IA usados este mes' })
    expect(consumo).toHaveAttribute('aria-valuenow', '85')
    expect(screen.getByText('Plan gratuito')).toBeInTheDocument()
    expect(screen.getByText(/Te estás acercando al límite/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Mejorar plan →' })).toBeInTheDocument()
  })
})
