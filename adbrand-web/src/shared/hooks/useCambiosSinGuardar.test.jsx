// Aviso de "tienes cambios sin guardar" al salir de Mi negocio o del Kit a mitad de editar.
import { act, renderHook } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { hayCambiosPendientes, navegar } from '../navegacion/navegacion'
import { useCambiosSinGuardar } from './useCambiosSinGuardar'

describe('useCambiosSinGuardar', () => {
  afterEach(() => {
    vi.restoreAllMocks()
    window.history.replaceState({}, '', '/')
  })

  it('sin cambios no pregunta nada al navegar', () => {
    const confirmar = vi.spyOn(window, 'confirm')
    renderHook(() => useCambiosSinGuardar({ nombre: 'Rosa' }))

    expect(hayCambiosPendientes()).toBe(false)
    expect(navegar('/calendario')).toBe(true)
    expect(confirmar).not.toHaveBeenCalled()
  })

  it('con cambios pregunta antes de salir y, si el usuario se queda, no navega', () => {
    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(false)
    const { rerender, unmount } = renderHook(({ datos }) => useCambiosSinGuardar(datos), {
      initialProps: { datos: { nombre: 'Rosa' } },
    })
    rerender({ datos: { nombre: 'Rosa Quispe' } })

    expect(hayCambiosPendientes()).toBe(true)
    expect(navegar('/calendario')).toBe(false)
    expect(confirmar).toHaveBeenCalled()
    expect(window.location.pathname).toBe('/')
    unmount()
  })

  it('si confirma que quiere salir, navega igual', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    const { rerender, unmount } = renderHook(({ datos }) => useCambiosSinGuardar(datos), {
      initialProps: { datos: { nombre: 'Rosa' } },
    })
    rerender({ datos: { nombre: 'Otro' } })

    expect(navegar('/planes')).toBe(true)
    expect(window.location.pathname).toBe('/planes')
    unmount()
  })

  it('al cerrar el editor (guardar o cancelar) ya no quedan cambios pendientes', () => {
    const { rerender, unmount } = renderHook(({ datos }) => useCambiosSinGuardar(datos), {
      initialProps: { datos: { nombre: 'Rosa' } },
    })
    rerender({ datos: { nombre: 'Otro' } })
    expect(hayCambiosPendientes()).toBe(true)

    act(() => unmount())

    expect(hayCambiosPendientes()).toBe(false)
  })

  it('compara contra lo guardado y solo cuando se está editando', () => {
    const guardado = { nombre: 'Rosa' }
    const { result, rerender } = renderHook(({ actual, activo }) => useCambiosSinGuardar(actual, { original: guardado, activo }), {
      initialProps: { actual: { nombre: 'Rosa 2' }, activo: false },
    })
    expect(result.current).toBe(false)

    rerender({ actual: { nombre: 'Rosa 2' }, activo: true })
    expect(result.current).toBe(true)

    rerender({ actual: { nombre: 'Rosa' }, activo: true })
    expect(result.current).toBe(false)
  })
})
