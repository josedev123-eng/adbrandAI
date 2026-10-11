// HU-04: prueba el viewmodel de auditoría sin necesitar Django encendido.
import { act, renderHook, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { listarAdministradores } from '../../admin/models/administradoresApi'
import { listarAuditoria } from '../models/auditoriaApi'
import { useAuditoria } from './useAuditoria'

vi.mock('../../admin/models/administradoresApi', () => ({
  listarAdministradores: vi.fn(),
}))

vi.mock('../models/auditoriaApi', () => ({
  listarAuditoria: vi.fn(),
}))

const ADMINISTRADORES = [
  { id: 1, nombres: 'Super', apellidos: 'Admin', correo: 'super@adbrand.pe' },
  { id: 2, nombres: 'Ana', apellidos: 'Moderadora', correo: 'ana@adbrand.pe' },
]

const CAMBIO_DE_ROL = {
  id: 9,
  admin: { id: 1, nombre: 'Super Admin', correo: 'super@adbrand.pe' },
  accion: 'CAMBIAR_ROL',
  entidad: 'usuario_admin',
  entidad_id: 2,
  detalle: 'Cambió el rol de ana@adbrand.pe a FINANZAS',
  fecha: '2026-10-10T15:32:08-05:00',
}

const SIN_FILTROS = { adminId: '', desde: '', hasta: '' }

describe('useAuditoria (HU-04)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    listarAdministradores.mockResolvedValue(ADMINISTRADORES)
    listarAuditoria.mockResolvedValue([CAMBIO_DE_ROL])
  })

  it('al entrar carga el historial completo y los administradores del filtro', async () => {
    const { result } = renderHook(() => useAuditoria())

    await waitFor(() => expect(result.current.cargando).toBe(false))

    expect(listarAuditoria).toHaveBeenCalledWith(SIN_FILTROS)
    expect(result.current.registros).toEqual([CAMBIO_DE_ROL])
    expect(result.current.administradores).toEqual(ADMINISTRADORES)
    expect(result.current.hayFiltros).toBe(false)
  })

  it('al aplicar los filtros vuelve a consultar con el administrador y las fechas', async () => {
    const { result } = renderHook(() => useAuditoria())
    await waitFor(() => expect(result.current.cargando).toBe(false))

    act(() => result.current.cambiarFiltro('adminId', '2'))
    act(() => result.current.cambiarFiltro('desde', '2026-10-01'))
    act(() => result.current.cambiarFiltro('hasta', '2026-10-10'))
    act(() => result.current.aplicarFiltros())

    await waitFor(() =>
      expect(listarAuditoria).toHaveBeenLastCalledWith({ adminId: '2', desde: '2026-10-01', hasta: '2026-10-10' }),
    )
    await waitFor(() => expect(result.current.cargando).toBe(false))
    expect(result.current.hayFiltros).toBe(true)
  })

  it('si la fecha inicial es mayor que la final muestra el error y no llama a la API', async () => {
    const { result } = renderHook(() => useAuditoria())
    await waitFor(() => expect(result.current.cargando).toBe(false))

    act(() => result.current.cambiarFiltro('desde', '2026-10-10'))
    act(() => result.current.cambiarFiltro('hasta', '2026-10-01'))
    act(() => result.current.aplicarFiltros())

    expect(result.current.errorFiltros).toBe('La fecha inicial no puede ser mayor que la final.')
    expect(listarAuditoria).toHaveBeenCalledTimes(1)
  })

  it('al limpiar vuelve a pedir el historial completo', async () => {
    const { result } = renderHook(() => useAuditoria())
    await waitFor(() => expect(result.current.cargando).toBe(false))

    act(() => result.current.cambiarFiltro('adminId', '2'))
    act(() => result.current.aplicarFiltros())
    await waitFor(() => expect(listarAuditoria).toHaveBeenCalledTimes(2))

    act(() => result.current.limpiarFiltros())

    await waitFor(() => expect(listarAuditoria).toHaveBeenCalledTimes(3))
    expect(listarAuditoria).toHaveBeenLastCalledWith(SIN_FILTROS)
    expect(result.current.filtros).toEqual(SIN_FILTROS)
  })

  it('si Django no responde muestra un mensaje claro', async () => {
    listarAuditoria.mockRejectedValue(new Error('sin conexión'))

    const { result } = renderHook(() => useAuditoria())

    await waitFor(() => expect(result.current.error).toMatch(/No se pudo cargar el historial/))
    expect(result.current.registros).toEqual([])
  })
})
