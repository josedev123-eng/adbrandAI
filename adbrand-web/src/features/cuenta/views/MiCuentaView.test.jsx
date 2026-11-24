// HU-23: los datos de la empresa se ven primero para leer; "Editar" abre el formulario.
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { guardarEmpresa, obtenerEmpresa } from '../models/cuentaApi'
import MiCuentaView from './MiCuentaView'

vi.mock('../models/cuentaApi', () => ({
  obtenerEmpresa: vi.fn(),
  guardarEmpresa: vi.fn(),
}))

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
})
