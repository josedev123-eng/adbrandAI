// HU-16: historial de contenidos aprobados y rechazados, sin necesitar Django encendido.
import { fireEvent, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { listarHistorial } from '../models/moderacionApi'
import { AVISO_FECHAS } from '../viewmodels/useHistorialContenidos'
import HistorialContenidosView from './HistorialContenidosView'

vi.mock('../models/moderacionApi', async (importOriginal) => ({
  ...(await importOriginal()),
  listarHistorial: vi.fn(),
}))

const RECHAZADO = {
  id: 7,
  empresa: 'Panadería Doña Rosa',
  tipo: 'ANUNCIO',
  red_social: 'INSTAGRAM',
  oferta: 'Apuestas deportivas',
  texto: 'Texto completo del anuncio rechazado.',
  estado: 'RECHAZADO',
  motivo: 'Publicidad de apuestas',
  moderador: 'Ana Quispe',
  fecha_decision: '2026-10-27T09:05:00-05:00',
}
const APROBADO = {
  ...RECHAZADO,
  id: 3,
  oferta: '2 panetones por S/ 35',
  estado: 'APROBADO',
  motivo: null,
  moderador: 'Filtro automático',
}

function pagina(resultados, extra = {}) {
  return { total: resultados.length, pagina: 1, paginas: 1, resultados, ...extra }
}

describe('HistorialContenidosView (HU-16)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    listarHistorial.mockResolvedValue(pagina([RECHAZADO, APROBADO]))
  })

  it('muestra cada contenido con fecha, moderador, estado y motivo (criterio 1)', async () => {
    render(<HistorialContenidosView />)

    const filaRechazada = (await screen.findByText('Apuestas deportivas')).closest('tr')
    expect(within(filaRechazada).getByText('Ana Quispe')).toBeInTheDocument()
    expect(within(filaRechazada).getByText('Publicidad de apuestas')).toBeInTheDocument()
    expect(within(filaRechazada).getByText('Rechazado')).toHaveClass('estado-rechazado')
    expect(within(filaRechazada).getByText('Panadería Doña Rosa')).toBeInTheDocument()

    const filaAprobada = screen.getByText('2 panetones por S/ 35').closest('tr')
    expect(within(filaAprobada).getByText('Filtro automático')).toBeInTheDocument()
    expect(within(filaAprobada).getByText('—')).toBeInTheDocument()
    expect(within(filaAprobada).getByText('Aprobado')).toHaveClass('estado-aprobado')
  })

  it('filtra por estado y fechas (criterio 2)', async () => {
    render(<HistorialContenidosView />)
    await screen.findByText('Apuestas deportivas')

    fireEvent.change(screen.getByLabelText('Estado'), { target: { value: 'RECHAZADO' } })
    fireEvent.change(screen.getByLabelText('Desde'), { target: { value: '2026-10-01' } })
    fireEvent.change(screen.getByLabelText('Hasta'), { target: { value: '2026-10-27' } })
    fireEvent.click(screen.getByRole('button', { name: 'Filtrar' }))

    await screen.findByText('Apuestas deportivas')
    expect(listarHistorial).toHaveBeenLastCalledWith({
      estado: 'RECHAZADO',
      desde: '2026-10-01',
      hasta: '2026-10-27',
      pagina: 1,
    })
  })

  it('si el inicio es posterior al fin muestra el aviso y no llama a la API', async () => {
    render(<HistorialContenidosView />)
    await screen.findByText('Apuestas deportivas')

    fireEvent.change(screen.getByLabelText('Desde'), { target: { value: '2026-10-27' } })
    fireEvent.change(screen.getByLabelText('Hasta'), { target: { value: '2026-10-01' } })
    fireEvent.click(screen.getByRole('button', { name: 'Filtrar' }))

    expect(screen.getByRole('alert')).toHaveTextContent(AVISO_FECHAS)
    expect(listarHistorial).toHaveBeenCalledTimes(1)
  })

  it('al hacer clic en una fila muestra el texto completo', async () => {
    render(<HistorialContenidosView />)
    await userEvent.click(await screen.findByText('Apuestas deportivas'))

    expect(screen.getByText('Texto completo del anuncio rechazado.')).toBeInTheDocument()
  })

  it('pasa a la página siguiente', async () => {
    listarHistorial.mockResolvedValueOnce(pagina([RECHAZADO], { total: 21, paginas: 2 }))
    listarHistorial.mockResolvedValueOnce(pagina([APROBADO], { total: 21, pagina: 2, paginas: 2 }))
    render(<HistorialContenidosView />)
    await screen.findByText('Página 1 de 2 · 21 contenidos')
    expect(screen.getByRole('button', { name: 'Anterior' })).toBeDisabled()

    await userEvent.click(screen.getByRole('button', { name: 'Siguiente' }))

    expect(await screen.findByText('Página 2 de 2 · 21 contenidos')).toBeInTheDocument()
    expect(listarHistorial).toHaveBeenLastCalledWith({ estado: '', desde: '', hasta: '', pagina: 2 })
    expect(screen.getByRole('button', { name: 'Siguiente' })).toBeDisabled()
  })

  it('avisa si no hay resultados', async () => {
    listarHistorial.mockResolvedValue(pagina([]))
    render(<HistorialContenidosView />)

    expect(await screen.findByText('No hay contenidos aprobados ni rechazados con esos filtros.')).toBeInTheDocument()
  })

  it('muestra el error si la API falla', async () => {
    listarHistorial.mockRejectedValue({ status: 500 })
    render(<HistorialContenidosView />)

    expect(await screen.findByText(/No se pudo cargar el historial/)).toBeInTheDocument()
  })
})
