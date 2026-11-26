// "Generar → Elegir": pedir 4 opciones, elegir una y ver el anuncio guardado con su imagen.
import { fireEvent, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { elegirOpcion, generarOpciones, retocarImagen, usarVersion } from '../models/anunciosApi'
import { obtenerPerfil } from '../../negocio/models/perfilNegocioApi'
import GenerarAnuncioView from './GenerarAnuncioView'
import { olvidarGeneracion } from '../models/generacionEnCurso'

vi.mock('../models/anunciosApi', async (importOriginal) => ({
  ...(await importOriginal()),
  generarOpciones: vi.fn(),
  elegirOpcion: vi.fn(),
  regenerarAnuncio: vi.fn(),
  retocarImagen: vi.fn(),
  usarVersion: vi.fn(),
  descargarImagenFinal: vi.fn(() => Promise.reject(new Error('sin diseño'))),
}))

vi.mock('../../../app/context/useSesion', () => ({ useSesion: () => ({ usuario: { nombreEmpresa: 'Panadería Rosa' } }) }))

vi.mock('../../negocio/models/perfilNegocioApi', async (importOriginal) => ({
  ...(await importOriginal()),
  obtenerPerfil: vi.fn(() => Promise.resolve({ nombreComercial: 'Panadería Rosa' })),
}))

const ESTILOS = ['Foto realista', 'Ilustración', 'Minimalista', 'Colorido']

function opcion(numero, elegida = false) {
  return {
    id: 100 + numero,
    numero,
    estilo: ESTILOS[numero - 1],
    texto: `Texto de la opción ${numero}`,
    imagen: `data:image/jpeg;base64,IMG${numero}`,
    proveedorImagen: 'cloudflare',
    imagenSimulada: false,
    textoSimulado: false,
    elegida,
  }
}

async function pedirOpciones() {
  fireEvent.change(screen.getByPlaceholderText(/panetones/), { target: { value: '2x1 en pan de yema' } })
  await userEvent.click(screen.getByRole('button', { name: '✦ Generar 4 opciones' }))
}

describe('GenerarAnuncioView (Generar → Elegir)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    olvidarGeneracion()
    generarOpciones.mockResolvedValue({
      generacionId: 7,
      oferta: '2x1 en pan de yema',
      redSocial: 'INSTAGRAM',
      opciones: [1, 2, 3, 4].map((numero) => opcion(numero)),
    })
  })

  it('muestra las 4 opciones con su imagen, su estilo y su texto', async () => {
    render(<GenerarAnuncioView />)
    await pedirOpciones()

    expect(generarOpciones).toHaveBeenCalledWith({ oferta: '2x1 en pan de yema', redSocial: 'INSTAGRAM' })
    const tarjetas = await screen.findAllByRole('article', { name: /Opción \d/ })
    expect(tarjetas).toHaveLength(4)
    expect(within(tarjetas[1]).getByText('Ilustración')).toBeInTheDocument()
    expect(within(tarjetas[1]).getByText('Texto de la opción 2')).toBeInTheDocument()
    expect(within(tarjetas[1]).getByRole('img')).toHaveAttribute('src', 'data:image/jpeg;base64,IMG2')
  })

  it('HU 67: cada opción muestra su enfoque y su titular corto', async () => {
    generarOpciones.mockResolvedValue({
      generacionId: 7,
      oferta: '2x1 en pan de yema',
      redSocial: 'INSTAGRAM',
      opciones: [1, 2, 3, 4].map((numero) => ({
        ...opcion(numero),
        angulo: ['Precio', 'Antojo', 'Confianza', 'Urgencia'][numero - 1],
        titular: numero === 2 ? 'Tu pan, recién salido' : null,
      })),
    })
    render(<GenerarAnuncioView />)
    await pedirOpciones()

    const tarjetas = await screen.findAllByRole('article', { name: /Opción \d/ })
    expect(within(tarjetas[0]).getByText('Enfoque: Precio')).toBeInTheDocument()
    expect(within(tarjetas[1]).getByText('Tu pan, recién salido')).toBeInTheDocument()
    expect(within(tarjetas[3]).getByText('Enfoque: Urgencia')).toBeInTheDocument()
  })

  it('deja agregar una foto del producto y la manda para que las 4 opciones la usen', async () => {
    render(<GenerarAnuncioView />)
    const foto = new File(['png'], 'pan.png', { type: 'image/png' })
    await userEvent.upload(screen.getByLabelText('Agregar una foto de tu producto'), foto)

    expect(await screen.findByRole('img', { name: 'Foto de tu producto' })).toHaveAttribute('src', expect.stringMatching(/^data:image\/png;base64,/))
    await pedirOpciones()
    expect(generarOpciones).toHaveBeenCalledWith(expect.objectContaining({ foto: expect.stringMatching(/^data:image\/png/) }))

    await userEvent.click(screen.getByRole('button', { name: 'Quitar foto' }))
    expect(screen.queryByRole('img', { name: 'Foto de tu producto' })).toBeNull()
  })

  it('una foto que no es JPG ni PNG se rechaza', async () => {
    render(<GenerarAnuncioView />)
    await userEvent.upload(screen.getByLabelText('Agregar una foto de tu producto'), new File(['x'], 'a.gif', { type: 'image/gif' }), {
      applyAccept: false,
    })
    expect(await screen.findByText('Sube tu foto en JPG o PNG.')).toBeInTheDocument()
  })

  it('sin oferta no pide opciones', async () => {
    render(<GenerarAnuncioView />)
    await userEvent.click(screen.getByRole('button', { name: '✦ Generar 4 opciones' }))

    expect(screen.getByText('Escribe la oferta que quieres anunciar.')).toBeInTheDocument()
    expect(generarOpciones).not.toHaveBeenCalled()
  })

  it('al elegir una opción muestra el anuncio guardado con su imagen y la marca como elegida', async () => {
    elegirOpcion.mockResolvedValue({
      anuncio: { id: 50, texto: 'Texto de la opción 3', redSocial: 'INSTAGRAM', tono: 'CERCANO', simulado: false, estado: 'APROBADO', motivoRevision: null },
      opcion: opcion(3, true),
    })
    render(<GenerarAnuncioView />)
    await pedirOpciones()

    const tercera = (await screen.findAllByRole('article', { name: /Opción \d/ }))[2]
    await userEvent.click(within(tercera).getByRole('button', { name: 'Elegir esta' }))

    expect(elegirOpcion).toHaveBeenCalledWith(103)
    expect(await screen.findByRole('button', { name: 'Copiar texto' })).toBeInTheDocument()
    expect(screen.getByAltText('Tu anuncio (Minimalista)')).toHaveAttribute('src', 'data:image/jpeg;base64,IMG3')
    expect(within(tercera).getByText('✓ Elegida')).toBeInTheDocument()
  })

  it('si la IA no responde muestra el aviso', async () => {
    generarOpciones.mockRejectedValue(Object.assign(new Error('El servicio de IA no responde en este momento.'), {
      codigo: 'IA_NO_DISPONIBLE',
      status: 503,
    }))
    render(<GenerarAnuncioView />)
    await pedirOpciones()

    expect(await screen.findByText('El servicio de IA no responde en este momento.')).toBeInTheDocument()
  })

  describe('retocar la imagen elegida', () => {
    function historial(versionActual) {
      return {
        opcionId: 103,
        versionActual,
        imagenActual: versionActual === 1 ? 'data:image/jpeg;base64,IMG3' : 'data:image/png;base64,RETOQUE',
        versiones: [
          { numero: 1, instruccion: null, imagen: 'data:image/jpeg;base64,IMG3', proveedorImagen: 'cloudflare', imagenSimulada: false, actual: versionActual === 1 },
          { numero: 2, instruccion: 'fondo más claro', imagen: 'data:image/png;base64,RETOQUE', proveedorImagen: 'openrouter', imagenSimulada: false, actual: versionActual === 2 },
        ],
      }
    }

    async function elegirLaTercera() {
      elegirOpcion.mockResolvedValue({
        anuncio: { id: 50, texto: 'Texto de la opción 3', redSocial: 'INSTAGRAM', tono: 'CERCANO', simulado: false, estado: 'APROBADO', motivoRevision: null },
        opcion: opcion(3, true),
      })
      render(<GenerarAnuncioView />)
      await pedirOpciones()
      const tercera = (await screen.findAllByRole('article', { name: /Opción \d/ }))[2]
      await userEvent.click(within(tercera).getByRole('button', { name: 'Elegir esta' }))
      await screen.findByRole('button', { name: 'Copiar texto' })
    }

    it('aplica la instrucción corta, cambia la imagen y muestra las versiones', async () => {
      retocarImagen.mockResolvedValue(historial(2))
      await elegirLaTercera()

      await userEvent.type(screen.getByPlaceholderText('Ej.: pon el fondo más claro'), 'fondo más claro')
      await userEvent.click(screen.getByRole('button', { name: '✦ Retocar' }))

      expect(retocarImagen).toHaveBeenCalledWith(103, 'fondo más claro')
      expect(await screen.findByAltText('Tu anuncio (Minimalista)')).toHaveAttribute('src', 'data:image/png;base64,RETOQUE')
      expect(screen.getByRole('button', { name: 'Usar la versión 1 (original)' })).toBeEnabled()
      expect(screen.getByRole('button', { name: 'Usar la versión 2: fondo más claro' })).toBeDisabled()
    })

    it('sin instrucción no llama a la IA', async () => {
      await elegirLaTercera()

      await userEvent.click(screen.getByRole('button', { name: '✦ Retocar' }))

      expect(screen.getByText('Escribe qué quieres cambiar.')).toBeInTheDocument()
      expect(retocarImagen).not.toHaveBeenCalled()
    })

    it('deshacer vuelve a la versión original', async () => {
      retocarImagen.mockResolvedValue(historial(2))
      usarVersion.mockResolvedValue(historial(1))
      await elegirLaTercera()
      await userEvent.type(screen.getByPlaceholderText('Ej.: pon el fondo más claro'), 'fondo más claro')
      await userEvent.click(screen.getByRole('button', { name: '✦ Retocar' }))

      await userEvent.click(await screen.findByRole('button', { name: 'Usar la versión 1 (original)' }))

      expect(usarVersion).toHaveBeenCalledWith(103, 1)
      expect(await screen.findByAltText('Tu anuncio (Minimalista)')).toHaveAttribute('src', 'data:image/jpeg;base64,IMG3')
    })
    it('muestra cuántos retoques quedan y avisa al llegar al límite', async () => {
      retocarImagen.mockResolvedValueOnce({ ...historial(2), retoquesRestantes: 19 })
      await elegirLaTercera()
      await userEvent.type(screen.getByPlaceholderText('Ej.: pon el fondo más claro'), 'fondo más claro')
      await userEvent.click(screen.getByRole('button', { name: '✦ Retocar' }))

      expect(await screen.findByText('Te quedan 19 retoques este mes.')).toBeInTheDocument()

      retocarImagen.mockRejectedValueOnce(Object.assign(new Error('Llegaste a tu límite de 20 retoques de imagen este mes.'), {
        status: 400,
        codigo: 'LIMITE_RETOQUES',
      }))
      await userEvent.type(screen.getByPlaceholderText('Ej.: pon el fondo más claro'), 'logo arriba')
      await userEvent.click(screen.getByRole('button', { name: '✦ Retocar' }))

      expect(await screen.findByText('Llegaste a tu límite de 20 retoques de imagen este mes.')).toBeInTheDocument()
    })
  })
})

describe('GenerarAnuncioView: límite de tokens del plan', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('si ya usó los tokens de su plan muestra el motivo y el botón para ver los planes', async () => {
    generarOpciones.mockRejectedValue(
      Object.assign(new Error('Usaste los 10 000 tokens de IA del plan gratuito este mes. Elige un plan para seguir creando.'), {
        status: 400,
        codigo: 'LIMITE_TOKENS',
      }),
    )
    const irAPlanes = vi.fn()
    render(<GenerarAnuncioView irAPlanes={irAPlanes} />)
    await pedirOpciones()

    expect(await screen.findByText(/Usaste los 10 000 tokens de IA del plan gratuito/)).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Ver planes' }))
    expect(irAPlanes).toHaveBeenCalled()
  })

  it('sin perfil del negocio avisa desde que entra y no deja generar', async () => {
    obtenerPerfil.mockRejectedValueOnce(Object.assign(new Error('No existe'), { status: 404 }))
    render(<GenerarAnuncioView />)

    expect(await screen.findByText(/Primero completa el perfil de tu negocio/)).toBeInTheDocument()
    expect(screen.getByText('Falta tu perfil')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '✦ Generar 4 opciones' })).toBeDisabled()
  })

  it('desde una fecha del calendario el anuncio se ambienta en la festividad, y se puede quitar', async () => {
    sessionStorage.setItem('adbrand.ofertaSugerida', 'Cupcakes de calabaza 2x1')
    sessionStorage.setItem('adbrand.ocasionSugerida', 'Halloween')
    render(<GenerarAnuncioView />)

    expect(screen.getByRole('status')).toHaveTextContent('Para Halloween')
    await userEvent.click(screen.getByRole('button', { name: '✦ Generar 4 opciones' }))
    expect(generarOpciones).toHaveBeenCalledWith({ oferta: 'Cupcakes de calabaza 2x1', redSocial: 'INSTAGRAM', ocasion: 'Halloween' })

    await userEvent.click(screen.getByRole('button', { name: 'Quitar' }))
    expect(screen.queryByText(/Para/, { selector: 'span' })).toBeNull()
  })

  it('cada opción se ve como un anuncio diseñado cuando Spring manda el diseño', async () => {
    generarOpciones.mockResolvedValue({
      generacionId: 7,
      oferta: '2x1 en pan de yema',
      redSocial: 'INSTAGRAM',
      opciones: [1, 2, 3, 4].map((numero) => ({ ...opcion(numero), diseno: `data:image/jpeg;base64,DISENO${numero}` })),
    })
    render(<GenerarAnuncioView />)
    await pedirOpciones()

    const tarjetas = await screen.findAllByRole('article', { name: /Opción \d/ })
    expect(within(tarjetas[0]).getByRole('img')).toHaveAttribute('src', 'data:image/jpeg;base64,DISENO1')
  })

  it('si sale de la pantalla mientras se crean las opciones, al volver las ve', async () => {
    let terminar
    generarOpciones.mockReturnValue(new Promise((resolver) => (terminar = resolver)))
    const { unmount } = render(<GenerarAnuncioView />)
    await pedirOpciones()
    unmount()

    terminar({ generacionId: 7, oferta: '2x1 en pan de yema', redSocial: 'INSTAGRAM', opciones: [1, 2, 3, 4].map((n) => opcion(n)) })
    await Promise.resolve()
    render(<GenerarAnuncioView />)

    expect(await screen.findAllByRole('article', { name: /Opción \d/ })).toHaveLength(4)
    expect(screen.getByDisplayValue('2x1 en pan de yema')).toBeInTheDocument()
  })

  it('si vuelve mientras todavía se crean, sigue esperando y luego las muestra', async () => {
    let terminar
    generarOpciones.mockReturnValue(new Promise((resolver) => (terminar = resolver)))
    const { unmount } = render(<GenerarAnuncioView />)
    await pedirOpciones()
    unmount()
    render(<GenerarAnuncioView />)

    expect(screen.getByRole('button', { name: 'Creando 4 opciones...' })).toBeDisabled()
    terminar({ generacionId: 7, oferta: '2x1 en pan de yema', redSocial: 'INSTAGRAM', opciones: [1, 2, 3, 4].map((n) => opcion(n)) })
    expect(await screen.findAllByRole('article', { name: /Opción \d/ })).toHaveLength(4)
  })
})
