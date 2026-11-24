// Empresas: el administrador suspende (con motivo) o reactiva una PYME que abusa.
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { listarEmpresas, reactivarEmpresa, suspenderEmpresa } from '../models/empresasApi'
import EmpresasView from './EmpresasView'

vi.mock('../models/empresasApi', async (importOriginal) => ({
  ...(await importOriginal()),
  listarEmpresas: vi.fn(),
  suspenderEmpresa: vi.fn(),
  reactivarEmpresa: vi.fn(),
}))

const EMPRESAS = [
  { id: 2, nombre_empresa: 'Tabaquería Humo', correo: 'humo@tienda.pe', estado: 'ACTIVO', plan: null, tokens_mes: 900, anuncios: 4, rechazados: 3 },
  {
    id: 3,
    nombre_empresa: 'Bodega Antigua',
    correo: 'bodega@tienda.pe',
    estado: 'INACTIVO',
    motivo_suspension: 'Publicidad engañosa repetida.',
    fecha_suspension: '2026-11-20T10:00:00Z',
    plan: 'Pro',
    tokens_mes: 0,
    anuncios: 2,
    rechazados: 2,
  },
]

function fila(nombre) {
  return screen.getByText(nombre).closest('tr')
}

describe('EmpresasView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    listarEmpresas.mockResolvedValue(EMPRESAS)
  })

  it('muestra cada empresa con su plan, su consumo, sus rechazos y si está suspendida', async () => {
    render(<EmpresasView />)

    expect(await screen.findByText('Tabaquería Humo')).toBeInTheDocument()
    expect(within(fila('Tabaquería Humo')).getByText('Gratuito')).toBeInTheDocument()
    expect(within(fila('Tabaquería Humo')).getByText('3 de 4')).toBeInTheDocument()
    expect(within(fila('Bodega Antigua')).getByText('Publicidad engañosa repetida.')).toBeInTheDocument()
    expect(within(fila('Bodega Antigua')).getByRole('button', { name: 'Reactivar' })).toBeInTheDocument()
  })

  it('suspender pide un motivo y no envía si es muy corto', async () => {
    render(<EmpresasView />)
    await screen.findByText('Tabaquería Humo')
    await userEvent.click(within(fila('Tabaquería Humo')).getByRole('button', { name: 'Suspender' }))

    const ventana = screen.getByRole('dialog', { name: 'Suspender a Tabaquería Humo' })
    await userEvent.type(within(ventana).getByLabelText(/Motivo de la suspensión/), 'Porque sí')
    await userEvent.click(within(ventana).getByRole('button', { name: 'Suspender cuenta' }))

    expect(within(ventana).getByText('Explica el motivo en al menos 10 caracteres.')).toBeInTheDocument()
    expect(suspenderEmpresa).not.toHaveBeenCalled()
  })

  it('con un motivo válido suspende, actualiza la fila y avisa', async () => {
    suspenderEmpresa.mockResolvedValue({ id: 2, estado: 'INACTIVO', motivo_suspension: 'Contenido de tabaco repetido.' })
    render(<EmpresasView />)
    await screen.findByText('Tabaquería Humo')
    await userEvent.click(within(fila('Tabaquería Humo')).getByRole('button', { name: 'Suspender' }))

    const ventana = screen.getByRole('dialog', { name: 'Suspender a Tabaquería Humo' })
    await userEvent.type(within(ventana).getByLabelText(/Motivo de la suspensión/), 'Contenido de tabaco repetido.')
    await userEvent.click(within(ventana).getByRole('button', { name: 'Suspender cuenta' }))

    expect(suspenderEmpresa).toHaveBeenCalledWith(2, 'Contenido de tabaco repetido.')
    expect(await screen.findByText(/quedó suspendida/)).toBeInTheDocument()
    expect(within(fila('Tabaquería Humo')).getByRole('button', { name: 'Reactivar' })).toBeInTheDocument()
  })

  it('reactivar pide confirmar y vuelve a dejar activa la empresa', async () => {
    reactivarEmpresa.mockResolvedValue({ id: 3, estado: 'ACTIVO', motivo_suspension: null, fecha_suspension: null })
    render(<EmpresasView />)
    await screen.findByText('Bodega Antigua')
    await userEvent.click(within(fila('Bodega Antigua')).getByRole('button', { name: 'Reactivar' }))

    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Sí, reactivar' }))

    expect(reactivarEmpresa).toHaveBeenCalledWith(3)
    await waitFor(() => expect(within(fila('Bodega Antigua')).getByRole('button', { name: 'Suspender' })).toBeInTheDocument())
  })
})
