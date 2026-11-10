// HU 39: botón "Descargar Word" en la pantalla de informes, sin necesitar Spring encendido.
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { listarCampanas } from '../../calendario/models/campanasApi'
import { descargarInformeWord, guardarArchivo, nombreArchivoInforme, obtenerInformeCampanas } from '../models/reportesApi'
import InformeCampanasView from './InformeCampanasView'

vi.mock('../models/reportesApi', async (importOriginal) => ({
  ...(await importOriginal()),
  obtenerInformeCampanas: vi.fn(),
  descargarInformePdf: vi.fn(),
  descargarInformeWord: vi.fn(),
  guardarArchivo: vi.fn(),
}))

vi.mock('../../calendario/models/campanasApi', () => ({
  listarCampanas: vi.fn(),
  crearCampana: vi.fn(),
}))

const INFORME = {
  empresa: 'Panadería Doña Rosa',
  total: 1,
  aprobados: 1,
  dudosos: 0,
  rechazados: 0,
  campanas: [
    { id: 8, fecha: '2026-11-03T10:05:00', redSocial: 'INSTAGRAM', oferta: '2 panetones por S/ 35', estado: 'APROBADO' },
  ],
}

describe('Descargar Word (HU-39)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    listarCampanas.mockResolvedValue([])
  })

  it('descarga el Word con el nombre del informe de hoy', async () => {
    obtenerInformeCampanas.mockResolvedValue(INFORME)
    const word = new Blob(['PK'], { type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' })
    descargarInformeWord.mockResolvedValue(word)

    render(<InformeCampanasView />)
    const usuario = userEvent.setup()
    await usuario.click(await screen.findByRole('button', { name: /Descargar Word/ }))

    await waitFor(() => expect(guardarArchivo).toHaveBeenCalledWith(word, nombreArchivoInforme('docx')))
  })

  it('el Word sale con los filtros aplicados', async () => {
    obtenerInformeCampanas.mockResolvedValue(INFORME)
    descargarInformeWord.mockResolvedValue(new Blob(['PK']))

    render(<InformeCampanasView />)
    await screen.findByText('2 panetones por S/ 35')

    const usuario = userEvent.setup()
    const desde = screen.getByLabelText('Desde')
    await usuario.clear(desde)
    await usuario.type(desde, '2026-11-01')
    await usuario.click(screen.getByRole('button', { name: 'Aplicar filtros' }))
    await screen.findByRole('button', { name: 'Quitar filtros' })

    await usuario.click(screen.getByRole('button', { name: /Descargar Word/ }))

    await waitFor(() =>
      expect(descargarInformeWord).toHaveBeenCalledWith({ desde: '2026-11-01', hasta: '', campanaId: '' }),
    )
  })

  it('sin campañas no muestra el botón', async () => {
    obtenerInformeCampanas.mockResolvedValue({ ...INFORME, total: 0, aprobados: 0, campanas: [] })

    render(<InformeCampanasView />)

    expect(await screen.findByText(/Todavía no tienes campañas/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Descargar Word/ })).not.toBeInTheDocument()
  })

  it('si la descarga falla, avisa', async () => {
    obtenerInformeCampanas.mockResolvedValue(INFORME)
    descargarInformeWord.mockRejectedValue({ status: 500 })

    render(<InformeCampanasView />)
    const usuario = userEvent.setup()
    await usuario.click(await screen.findByRole('button', { name: /Descargar Word/ }))

    expect(await screen.findByText(/No se pudo descargar el Word/)).toBeInTheDocument()
    expect(guardarArchivo).not.toHaveBeenCalled()
  })
})
