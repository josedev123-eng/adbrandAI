// HU 40: filtrar el informe (y su PDF) por periodo y campaña, sin necesitar Spring encendido.
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { listarCampanas } from '../../calendario/models/campanasApi'
import { consultaFiltros, descargarInformePdf, obtenerInformeCampanas } from '../models/reportesApi'
import { validarPeriodo } from '../viewmodels/useInformeCampanas'
import InformeCampanasView from './InformeCampanasView'

vi.mock('../models/reportesApi', async (importOriginal) => ({
  ...(await importOriginal()),
  obtenerInformeCampanas: vi.fn(),
  descargarInformePdf: vi.fn(),
  guardarArchivo: vi.fn(),
}))

vi.mock('../../calendario/models/campanasApi', () => ({
  listarCampanas: vi.fn(),
  crearCampana: vi.fn(),
}))

const COMPLETO = {
  empresa: 'Panadería Doña Rosa',
  total: 2,
  aprobados: 2,
  dudosos: 0,
  rechazados: 0,
  campanas: [
    { id: 8, fecha: '2026-11-03T10:05:00', redSocial: 'INSTAGRAM', oferta: '2 panetones por S/ 35', estado: 'APROBADO' },
    { id: 7, fecha: '2026-10-20T09:00:00', redSocial: 'FACEBOOK', oferta: 'Pan de yema', estado: 'APROBADO' },
  ],
}

const NOVIEMBRE = { ...COMPLETO, total: 1, aprobados: 1, campanas: [COMPLETO.campanas[0]] }

async function escribirFecha(usuario, etiqueta, valor) {
  const campo = screen.getByLabelText(etiqueta)
  await usuario.clear(campo)
  await usuario.type(campo, valor)
}

describe('Filtros del informe (HU-40)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    listarCampanas.mockResolvedValue([{ id: 3, nombre: 'Navidad 2026' }])
  })

  it('aplica el periodo y la campaña y muestra solo esos datos', async () => {
    obtenerInformeCampanas.mockResolvedValueOnce(COMPLETO).mockResolvedValueOnce(NOVIEMBRE)

    render(<InformeCampanasView />)
    await screen.findByText('Pan de yema')

    const usuario = userEvent.setup()
    await escribirFecha(usuario, 'Desde', '2026-11-01')
    await escribirFecha(usuario, 'Hasta', '2026-11-30')
    await waitFor(() => expect(screen.getByLabelText('Campaña')).toHaveTextContent('Navidad 2026'))
    await usuario.selectOptions(screen.getByLabelText('Campaña'), '3')
    await usuario.click(screen.getByRole('button', { name: 'Aplicar filtros' }))

    await waitFor(() =>
      expect(obtenerInformeCampanas).toHaveBeenLastCalledWith({ desde: '2026-11-01', hasta: '2026-11-30', campanaId: '3' }),
    )
    await waitFor(() => expect(screen.queryByText('Pan de yema')).not.toBeInTheDocument())
    expect(screen.getByText('2 panetones por S/ 35')).toBeInTheDocument()
  })

  it('con la fecha de inicio después de la de fin avisa y no consulta', async () => {
    obtenerInformeCampanas.mockResolvedValue(COMPLETO)

    render(<InformeCampanasView />)
    await screen.findByText('Pan de yema')

    const usuario = userEvent.setup()
    await escribirFecha(usuario, 'Desde', '2026-11-30')
    await escribirFecha(usuario, 'Hasta', '2026-11-01')
    await usuario.click(screen.getByRole('button', { name: 'Aplicar filtros' }))

    expect(await screen.findByText('La fecha de inicio no puede ser posterior a la fecha de fin.')).toBeInTheDocument()
    expect(obtenerInformeCampanas).toHaveBeenCalledTimes(1)
    // El informe que se veía no cambia.
    expect(screen.getByText('Pan de yema')).toBeInTheDocument()
  })

  it('sin resultados con los filtros lo dice, y "Quitar filtros" vuelve al informe completo', async () => {
    obtenerInformeCampanas
      .mockResolvedValueOnce(COMPLETO)
      .mockResolvedValueOnce({ ...COMPLETO, total: 0, aprobados: 0, campanas: [] })
      .mockResolvedValueOnce(COMPLETO)

    render(<InformeCampanasView />)
    await screen.findByText('Pan de yema')

    const usuario = userEvent.setup()
    await escribirFecha(usuario, 'Desde', '2027-01-01')
    await usuario.click(screen.getByRole('button', { name: 'Aplicar filtros' }))

    expect(await screen.findByText(/No hay campañas con esos filtros/)).toBeInTheDocument()

    await usuario.click(screen.getByRole('button', { name: 'Quitar filtros' }))

    await waitFor(() =>
      expect(obtenerInformeCampanas).toHaveBeenLastCalledWith({ desde: '', hasta: '', campanaId: '' }),
    )
    expect(await screen.findByText('Pan de yema')).toBeInTheDocument()
    expect(screen.getByLabelText('Desde')).toHaveValue('')
  })

  it('el PDF se descarga con los filtros aplicados', async () => {
    obtenerInformeCampanas.mockResolvedValueOnce(COMPLETO).mockResolvedValueOnce(NOVIEMBRE)
    descargarInformePdf.mockResolvedValue(new Blob(['%PDF'], { type: 'application/pdf' }))

    render(<InformeCampanasView />)
    await screen.findByText('Pan de yema')

    const usuario = userEvent.setup()
    await escribirFecha(usuario, 'Desde', '2026-11-01')
    await usuario.click(screen.getByRole('button', { name: 'Aplicar filtros' }))
    await waitFor(() => expect(screen.queryByText('Pan de yema')).not.toBeInTheDocument())

    await usuario.click(screen.getByRole('button', { name: /Descargar PDF/ }))

    await waitFor(() =>
      expect(descargarInformePdf).toHaveBeenCalledWith({ desde: '2026-11-01', hasta: '', campanaId: '' }),
    )
  })

  it('consultaFiltros y validarPeriodo', () => {
    expect(consultaFiltros({ desde: '2026-11-01', hasta: '', campanaId: '3' })).toBe('?desde=2026-11-01&campanaId=3')
    expect(consultaFiltros({})).toBe('')
    expect(consultaFiltros()).toBe('')

    expect(validarPeriodo({ desde: '2026-11-01', hasta: '2026-11-30' })).toBeNull()
    expect(validarPeriodo({ desde: '2026-11-01', hasta: '' })).toBeNull()
    expect(validarPeriodo({ desde: '2026-11-30', hasta: '2026-11-01' })).toBe(
      'La fecha de inicio no puede ser posterior a la fecha de fin.',
    )
  })
})
