// HU 38: botón "Descargar PDF" en la pantalla de informes, sin necesitar Spring encendido.
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { descargarInformePdf, guardarArchivo, nombreArchivoInforme, obtenerInformeCampanas } from '../models/reportesApi'
import InformeCampanasView from './InformeCampanasView'

vi.mock('../models/reportesApi', async (importOriginal) => ({
  ...(await importOriginal()),
  obtenerInformeCampanas: vi.fn(),
  descargarInformePdf: vi.fn(),
  guardarArchivo: vi.fn(),
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

describe('Descargar PDF (HU-38)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('descarga el PDF con el nombre del informe de hoy', async () => {
    obtenerInformeCampanas.mockResolvedValue(INFORME)
    const pdf = new Blob(['%PDF-'], { type: 'application/pdf' })
    descargarInformePdf.mockResolvedValue(pdf)
    render(<InformeCampanasView />)

    fireEvent.click(await screen.findByRole('button', { name: '📄 Descargar PDF' }))

    await waitFor(() => expect(guardarArchivo).toHaveBeenCalledWith(pdf, nombreArchivoInforme('pdf')))
  })

  it('sin campañas no muestra el botón', async () => {
    obtenerInformeCampanas.mockResolvedValue({ ...INFORME, total: 0, aprobados: 0, campanas: [] })
    render(<InformeCampanasView />)

    await screen.findByRole('status')
    expect(screen.queryByRole('button', { name: /Descargar PDF/ })).toBeNull()
  })

  it('si falla la descarga lo avisa y deja el informe en pantalla', async () => {
    obtenerInformeCampanas.mockResolvedValue(INFORME)
    descargarInformePdf.mockRejectedValue({ status: 500 })
    render(<InformeCampanasView />)

    fireEvent.click(await screen.findByRole('button', { name: '📄 Descargar PDF' }))

    expect(await screen.findByText(/No se pudo descargar el PDF/)).toBeInTheDocument()
    expect(screen.getByText('2 panetones por S/ 35')).toBeInTheDocument()
    expect(guardarArchivo).not.toHaveBeenCalled()
  })

  it('el nombre del archivo lleva la fecha como AAAA-MM-DD', () => {
    expect(nombreArchivoInforme('pdf', new Date(2026, 10, 3))).toBe('informe-campanas-2026-11-03.pdf')
  })
})
