// HU 37: la pantalla de informes muestra las campañas de la empresa, sin necesitar Spring encendido.
import { render, screen, within } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { obtenerInformeCampanas } from '../models/reportesApi'
import InformeCampanasView from './InformeCampanasView'

vi.mock('../models/reportesApi', async (importOriginal) => ({
  ...(await importOriginal()),
  obtenerInformeCampanas: vi.fn(),
}))

const INFORME = {
  empresa: 'Panadería Doña Rosa',
  total: 2,
  aprobados: 1,
  dudosos: 0,
  rechazados: 1,
  campanas: [
    { id: 8, fecha: '2026-11-03T10:05:00', redSocial: 'INSTAGRAM', oferta: '2 panetones por S/ 35', estado: 'APROBADO' },
    { id: 5, fecha: '2026-11-01T09:00:00', redSocial: 'FACEBOOK', oferta: 'Cigarrillos importados', estado: 'RECHAZADO' },
  ],
}

describe('InformeCampanasView (HU-37)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('muestra cada campaña con su fecha, red social, oferta y estado (criterio 1)', async () => {
    obtenerInformeCampanas.mockResolvedValue(INFORME)
    render(<InformeCampanasView />)

    const fila = (await screen.findByText('2 panetones por S/ 35')).closest('tr')
    expect(within(fila).getByText('03/11/2026 10:05')).toBeInTheDocument()
    expect(within(fila).getByText('Instagram')).toBeInTheDocument()
    expect(within(fila).getByText('Aprobado')).toHaveClass('estado-aprobado')
    expect(screen.getByText('Cigarrillos importados').closest('tr')).toHaveTextContent('Rechazado')
  })

  it('muestra el nombre de la empresa y los totales', async () => {
    obtenerInformeCampanas.mockResolvedValue(INFORME)
    render(<InformeCampanasView />)

    const resumen = await screen.findByRole('region', { name: 'Resumen' })
    expect(resumen).toHaveTextContent('Panadería Doña Rosa')
    expect(within(resumen).getByText('Campañas').nextSibling).toHaveTextContent('2')
    expect(within(resumen).getByText('Rechazadas').nextSibling).toHaveTextContent('1')
  })

  it('sin campañas muestra un aviso y no muestra la tabla (criterio 2)', async () => {
    obtenerInformeCampanas.mockResolvedValue({ ...INFORME, total: 0, aprobados: 0, rechazados: 0, campanas: [] })
    render(<InformeCampanasView />)

    expect(await screen.findByRole('status')).toHaveTextContent('Todavía no tienes campañas')
    expect(screen.queryByRole('table')).toBeNull()
  })

  it('muestra un error si no se puede cargar el informe', async () => {
    obtenerInformeCampanas.mockRejectedValue({ status: 500 })
    render(<InformeCampanasView />)

    expect(await screen.findByText(/No se pudo cargar el informe/)).toBeInTheDocument()
  })
})
