// HU-02: prueba el viewmodel de edición de permisos sin necesitar Django encendido.
import { act, renderHook } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { cambiarRolAdministrador } from '../models/administradoresApi'
import { useEditarPermisos } from './useEditarPermisos'

vi.mock('../models/administradoresApi', () => ({
  cambiarRolAdministrador: vi.fn(),
}))

const SUPERADMIN = { id: 1, correo: 'super@adbrand.pe', rol: { id: 1, nombre: 'SUPERADMIN' } }
const MODERADOR = { id: 2, correo: 'ana@adbrand.pe', rol: { id: 2, nombre: 'MODERADOR' } }

describe('useEditarPermisos (HU-02)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('no deja editar los permisos de la propia cuenta', () => {
    const { result } = renderHook(() => useEditarPermisos(undefined, 1))

    expect(result.current.puedeEditar(SUPERADMIN)).toBe(false)
    expect(result.current.puedeEditar(MODERADOR)).toBe(true)
  })

  it('al abrir la edición carga el rol actual del administrador', () => {
    const { result } = renderHook(() => useEditarPermisos(undefined, 1))

    act(() => result.current.abrirEdicion(MODERADOR))

    expect(result.current.adminAEditar).toEqual(MODERADOR)
    expect(result.current.rolId).toBe('2')
  })

  it('si el rol es el mismo muestra el error y no llama a la API', async () => {
    const { result } = renderHook(() => useEditarPermisos(undefined, 1))

    act(() => result.current.abrirEdicion(MODERADOR))
    await act(async () => {
      await result.current.guardarPermisos()
    })

    expect(result.current.errorPermisos).toBe('Selecciona un rol distinto al actual.')
    expect(cambiarRolAdministrador).not.toHaveBeenCalled()
  })

  it('si no hay rol elegido muestra el error y no llama a la API', async () => {
    const { result } = renderHook(() => useEditarPermisos(undefined, 1))

    act(() => result.current.abrirEdicion(MODERADOR))
    act(() => result.current.cambiarRol(''))
    await act(async () => {
      await result.current.guardarPermisos()
    })

    expect(result.current.errorPermisos).toBe('Selecciona un rol.')
    expect(cambiarRolAdministrador).not.toHaveBeenCalled()
  })

  it('guarda el rol nuevo, cierra el modal y recarga la lista', async () => {
    cambiarRolAdministrador.mockResolvedValue({ ...MODERADOR, rol: { id: 3, nombre: 'FINANZAS' } })
    const alGuardar = vi.fn()
    const { result } = renderHook(() => useEditarPermisos(alGuardar))

    act(() => result.current.abrirEdicion(MODERADOR))
    act(() => result.current.cambiarRol('3'))
    await act(async () => {
      await result.current.guardarPermisos()
    })

    expect(cambiarRolAdministrador).toHaveBeenCalledWith(2, 3)
    expect(result.current.adminAEditar).toBeNull()
    expect(result.current.mensajePermisos.texto).toContain('FINANZAS')
    expect(alGuardar).toHaveBeenCalledTimes(1)
  })

  it('si el servidor responde 403 muestra su mensaje y deja el modal abierto', async () => {
    cambiarRolAdministrador.mockRejectedValue({
      status: 403,
      detalles: { detail: 'Solo un superadministrador puede modificar permisos.' },
    })
    const alGuardar = vi.fn()
    const { result } = renderHook(() => useEditarPermisos(alGuardar))

    act(() => result.current.abrirEdicion(MODERADOR))
    act(() => result.current.cambiarRol('3'))
    await act(async () => {
      await result.current.guardarPermisos()
    })

    expect(result.current.errorPermisos).toBe('Solo un superadministrador puede modificar permisos.')
    expect(result.current.adminAEditar).toEqual(MODERADOR)
    expect(alGuardar).not.toHaveBeenCalled()
  })
})
