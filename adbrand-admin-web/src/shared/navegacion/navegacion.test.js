// Navegación con URLs reales: cambiar de pantalla cambia la dirección y Atrás del navegador vuelve.
import { act, renderHook } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'
import { navegar, pantallaDeRuta, rutaActual, useRuta } from './navegacion'

describe('navegacion', () => {
  afterEach(() => window.history.replaceState({}, '', '/'))

  it('navegar cambia la URL y useRuta se entera', () => {
    const { result } = renderHook(() => useRuta())

    act(() => navegar('/calendario'))

    expect(window.location.pathname).toBe('/calendario')
    expect(result.current).toBe('/calendario')
  })

  it('el botón Atrás del navegador (popstate) actualiza la ruta', () => {
    const { result } = renderHook(() => useRuta())
    act(() => navegar('/mi-cuenta'))

    act(() => {
      window.history.replaceState({}, '', '/inicio')
      window.dispatchEvent(new PopStateEvent('popstate'))
    })

    expect(result.current).toBe('/inicio')
  })

  it('ignora la barra final y traduce la ruta a la pantalla', () => {
    window.history.replaceState({}, '', '/planes/')

    expect(rutaActual()).toBe('/planes')
    expect(pantallaDeRuta({ planes: '/planes' }, '/planes')).toBe('planes')
    expect(pantallaDeRuta({ planes: '/planes' }, '/no-existe')).toBeNull()
  })
})
