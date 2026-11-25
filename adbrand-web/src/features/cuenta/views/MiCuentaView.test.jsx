// HU-23: los datos de la empresa se ven primero para leer; "Editar" abre el formulario.
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { cambiarAvatar, guardarEmpresa, obtenerEmpresa, quitarAvatar } from '../models/cuentaApi'
import { obtenerResumenUso } from '../../pagos/models/pagosApi'
import MiCuentaView from './MiCuentaView'

vi.mock('../models/cuentaApi', () => ({
  obtenerEmpresa: vi.fn(),
  guardarEmpresa: vi.fn(),
  cambiarAvatar: vi.fn(),
  quitarAvatar: vi.fn(),
}))

vi.mock('../../pagos/models/pagosApi', () => ({ obtenerResumenUso: vi.fn(() => Promise.reject(new Error('sin datos'))) }))

const EMPRESA = {
  nombreEmpresa: 'Cafetería Don Pepe',
  correo: 'pepe@correo.pe',
  ruc: '20674968935',
  razonSocial: 'Cafetería Don Pepe S.A.C.',
  telefono: '987623231',
  direccion: 'Av. Los Girasoles 456, Lima',
}

describe('MiCuentaView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    obtenerEmpresa.mockResolvedValue(EMPRESA)
  })

  it('muestra los datos solo para leer, sin formulario', async () => {
    render(<MiCuentaView />)

    const datos = await screen.findByRole('region', { name: 'Datos de la empresa' })
    expect(within(datos).getByText('20674968935')).toBeInTheDocument()
    expect(within(datos).getByText('Cafetería Don Pepe S.A.C.')).toBeInTheDocument()
    expect(screen.queryByRole('textbox')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: '✎ Editar' })).toBeInTheDocument()
  })

  it('Editar abre el formulario y al guardar vuelve a la vista con el mensaje', async () => {
    guardarEmpresa.mockResolvedValue({ ...EMPRESA, direccion: 'Jr. Nuevo 100, Lima' })
    render(<MiCuentaView />)

    await userEvent.click(await screen.findByRole('button', { name: '✎ Editar' }))
    const direccion = screen.getByLabelText('Dirección')
    await userEvent.clear(direccion)
    await userEvent.type(direccion, 'Jr. Nuevo 100, Lima')
    await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }))

    expect(guardarEmpresa).toHaveBeenCalledWith(expect.objectContaining({ direccion: 'Jr. Nuevo 100, Lima' }))
    expect(await screen.findByText('Datos guardados')).toBeInTheDocument()
    expect(screen.queryByRole('textbox')).not.toBeInTheDocument()
    expect(screen.getByText('Jr. Nuevo 100, Lima')).toBeInTheDocument()
  })

  it('Cancelar descarta lo escrito y vuelve a los datos guardados', async () => {
    render(<MiCuentaView />)

    await userEvent.click(await screen.findByRole('button', { name: '✎ Editar' }))
    const direccion = screen.getByLabelText('Dirección')
    await userEvent.clear(direccion)
    await userEvent.type(direccion, 'Dirección a medias')
    await userEvent.click(screen.getByRole('button', { name: 'Cancelar' }))

    expect(guardarEmpresa).not.toHaveBeenCalled()
    expect(screen.getByText('Av. Los Girasoles 456, Lima')).toBeInTheDocument()
    expect(screen.queryByText('Dirección a medias')).not.toBeInTheDocument()
  })

  it('si la empresa todavía no tiene datos abre directo el formulario', async () => {
    obtenerEmpresa.mockResolvedValue({ nombreEmpresa: 'Nueva', correo: 'n@correo.pe', ruc: null, razonSocial: null, telefono: null, direccion: null })
    render(<MiCuentaView />)

    expect(await screen.findByRole('heading', { name: 'Completa los datos de tu empresa' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Cancelar' })).not.toBeInTheDocument()
  })

  it('si el servidor está caído muestra el mensaje claro', async () => {
    obtenerEmpresa.mockRejectedValue(
      Object.assign(new Error('No pudimos conectar con el servidor. Revisa tu conexión o inténtalo en unos minutos.'), {
        status: 0,
        codigo: 'SIN_CONEXION',
      }),
    )
    render(<MiCuentaView />)

    expect(await screen.findByRole('alert')).toHaveTextContent('No pudimos conectar con el servidor.')
  })
})

describe('MiCuentaView: color del avatar', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    obtenerEmpresa.mockResolvedValue({ ...EMPRESA, colorAvatar: null })
  })

  it('al elegir un color se guarda y avisa a la app para pintar el menú', async () => {
    guardarEmpresa.mockResolvedValue({ ...EMPRESA, colorAvatar: 'VERDE' })
    const alGuardar = vi.fn()
    render(<MiCuentaView alGuardar={alGuardar} />)

    await userEvent.click(await screen.findByRole('button', { name: '✎ Editar' }))
    await userEvent.click(screen.getByRole('radio', { name: 'Verde' }))
    await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }))

    expect(guardarEmpresa).toHaveBeenCalledWith(expect.objectContaining({ colorAvatar: 'VERDE' }))
    expect(alGuardar).toHaveBeenCalledWith(expect.objectContaining({ colorAvatar: 'VERDE' }))
  })

  it('el avatar puede ser el logo del kit y se puede quitar', async () => {
    obtenerEmpresa.mockResolvedValue(EMPRESA)
    cambiarAvatar.mockResolvedValue({ avatarImagen: 'data:image/png;base64,LOGO' })
    quitarAvatar.mockResolvedValue({ avatarImagen: null })
    const alCambiarAvatar = vi.fn()
    const { rerender } = render(<MiCuentaView alCambiarAvatar={alCambiarAvatar} />)

    await userEvent.click(await screen.findByRole('button', { name: 'Usar el logo de mi kit' }))
    expect(cambiarAvatar).toHaveBeenCalledWith({ usarLogoDelKit: true })
    expect(alCambiarAvatar).toHaveBeenCalledWith('data:image/png;base64,LOGO')

    rerender(<MiCuentaView alCambiarAvatar={alCambiarAvatar} avatarImagen="data:image/png;base64,LOGO" />)
    expect(screen.getByAltText('Tu avatar actual')).toHaveAttribute('src', 'data:image/png;base64,LOGO')
    await userEvent.click(screen.getByRole('button', { name: 'Quitar imagen' }))
    expect(alCambiarAvatar).toHaveBeenLastCalledWith(null)
  })

  it('no acepta archivos que no son PNG o JPG', async () => {
    obtenerEmpresa.mockResolvedValue(EMPRESA)
    render(<MiCuentaView alCambiarAvatar={vi.fn()} />)

    const archivo = new File(['gif'], 'foto.gif', { type: 'image/gif' })
    await userEvent.upload(await screen.findByLabelText('Elegir foto para el avatar'), archivo, { applyAccept: false })

    expect(screen.getByText('Elige una imagen PNG o JPG.')).toBeInTheDocument()
    expect(cambiarAvatar).not.toHaveBeenCalled()
  })

  it('muestra el plan y el uso del mes con barras, y lleva a Planes', async () => {
    obtenerEmpresa.mockResolvedValue(EMPRESA)
    obtenerResumenUso.mockResolvedValueOnce({
      consumo: { tokensUsados: 23520, limite: 150000, porcentaje: 16, origen: 'PLAN', plan: 'Pro', enAlerta: false, agotado: false },
      anunciosCreados: 1,
      retoquesUsados: 0,
      limiteRetoques: 20,
    })
    const irAPlanes = vi.fn()
    render(<MiCuentaView irAPlanes={irAPlanes} />)

    expect(await screen.findByText('Tu plan y tu uso este mes')).toBeInTheDocument()
    expect(screen.getByText('Pro')).toBeInTheDocument()
    expect(screen.getByRole('progressbar', { name: 'IA del plan' })).toHaveAttribute('aria-valuenow', '16')
    expect(screen.getByText(/te alcanza para ≈ 25 anuncios más/i)).toBeInTheDocument()
    expect(screen.getByRole('progressbar', { name: 'Retoques de imagen' })).toHaveAttribute('aria-valuenow', '0')

    await userEvent.click(screen.getByRole('button', { name: 'Cambiar de plan' }))
    expect(irAPlanes).toHaveBeenCalled()
  })
})
