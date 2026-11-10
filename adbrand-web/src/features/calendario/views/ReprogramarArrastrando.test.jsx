// HU 36: arrastrar una publicación a otro día; si el día no es válido, vuelve a su lugar con un aviso.
import { createEvent, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { listarAnuncios } from '../models/anunciosApi'
import { obtenerCalendario, reprogramarPublicacion } from '../models/calendarioApi'
import { listarCampanas } from '../models/campanasApi'
import { diasDelMes, fechaDeDia, mesActual } from '../viewmodels/useCalendario'
import CalendarioView from './CalendarioView'

vi.mock('../models/calendarioApi', () => ({
  obtenerCalendario: vi.fn(),
  crearPublicacion: vi.fn(),
  reprogramarPublicacion: vi.fn(),
}))

vi.mock('../models/campanasApi', () => ({
  listarCampanas: vi.fn(),
  crearCampana: vi.fn(),
}))

vi.mock('../models/anunciosApi', () => ({
  listarAnuncios: vi.fn(),
}))

// Se usan días del mes actual que se ven en pantalla: hoy, el último día y (si existe) ayer.
const MES = mesActual()
const HOY = new Date().getDate()
const DIA_ORIGEN = fechaDeDia(MES, HOY)
const DIA_DESTINO = fechaDeDia(MES, diasDelMes(MES))
const DIA_PASADO = HOY > 1 ? fechaDeDia(MES, HOY - 1) : null
// El último día del mes no tiene un día siguiente visible: ahí se saltan las pruebas de mover.
const HAY_DESTINO = DIA_DESTINO !== DIA_ORIGEN

const PUBLICACION = {
  id: 9,
  titulo: 'Oferta del viernes',
  redSocial: 'INSTAGRAM',
  fecha: DIA_ORIGEN,
  hora: '23:59:00',
  estado: 'PROGRAMADA',
}

// jsdom no trae DataTransfer; basta con guardar el id que se arrastra.
function nuevoDataTransfer() {
  const datos = {}
  return {
    setData: (tipo, valor) => {
      datos[tipo] = valor
    },
    getData: (tipo) => datos[tipo] ?? '',
    effectAllowed: '',
    dropEffect: '',
  }
}

function arrastrar(chip, celda) {
  const dataTransfer = nuevoDataTransfer()
  for (const [elemento, tipo] of [[chip, 'dragStart'], [celda, 'dragOver'], [celda, 'drop']]) {
    const evento = createEvent[tipo](elemento)
    Object.defineProperty(evento, 'dataTransfer', { value: dataTransfer })
    fireEvent(elemento, evento)
  }
}

function celdaDe(fecha) {
  return document.querySelector(`[data-fecha="${fecha}"]`)
}

function chipDe(titulo) {
  return screen.getByText(new RegExp(titulo)).closest('.calendario-chip')
}

describe('Reprogramar arrastrando', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    obtenerCalendario.mockResolvedValue([PUBLICACION])
    listarCampanas.mockResolvedValue([])
    listarAnuncios.mockResolvedValue([])
  })

  it('una publicación programada se puede arrastrar y una publicada no', async () => {
    obtenerCalendario.mockResolvedValue([
      PUBLICACION,
      { ...PUBLICACION, id: 10, titulo: 'Ya publicada', estado: 'PUBLICADA' },
    ])

    render(<CalendarioView />)
    await screen.findByText(/Oferta del viernes/)

    expect(chipDe('Oferta del viernes')).toHaveAttribute('draggable', 'true')
    expect(chipDe('Ya publicada')).toHaveAttribute('draggable', 'false')
  })

  it.runIf(HAY_DESTINO)('al soltarla en otro día guarda la fecha nueva y la muestra ahí', async () => {
    reprogramarPublicacion.mockResolvedValue({ ...PUBLICACION, fecha: DIA_DESTINO })

    render(<CalendarioView />)
    await screen.findByText(/Oferta del viernes/)

    arrastrar(chipDe('Oferta del viernes'), celdaDe(DIA_DESTINO))

    await waitFor(() => expect(reprogramarPublicacion).toHaveBeenCalledWith(9, DIA_DESTINO))
    await waitFor(() => expect(celdaDe(DIA_DESTINO)).toHaveTextContent('Oferta del viernes'))
    expect(celdaDe(DIA_ORIGEN)).not.toHaveTextContent('Oferta del viernes')
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it.runIf(DIA_PASADO)('si la suelta en un día pasado no llama a Spring y avisa', async () => {
    render(<CalendarioView />)
    await screen.findByText(/Oferta del viernes/)

    arrastrar(chipDe('Oferta del viernes'), celdaDe(DIA_PASADO))

    expect(await screen.findByRole('alert')).toHaveTextContent('No puedes mover una publicación a un día que ya pasó.')
    expect(reprogramarPublicacion).not.toHaveBeenCalled()
    expect(celdaDe(DIA_ORIGEN)).toHaveTextContent('Oferta del viernes')
  })

  it.runIf(HAY_DESTINO)('si Spring no acepta el cambio, vuelve a su día y muestra el mensaje', async () => {
    reprogramarPublicacion.mockRejectedValue(new Error('Solo se pueden mover las publicaciones programadas.'))

    render(<CalendarioView />)
    await screen.findByText(/Oferta del viernes/)

    arrastrar(chipDe('Oferta del viernes'), celdaDe(DIA_DESTINO))

    expect(await screen.findByRole('alert')).toHaveTextContent('Solo se pueden mover las publicaciones programadas.')
    await waitFor(() => expect(celdaDe(DIA_ORIGEN)).toHaveTextContent('Oferta del viernes'))
    expect(celdaDe(DIA_DESTINO)).not.toHaveTextContent('Oferta del viernes')
  })
})
