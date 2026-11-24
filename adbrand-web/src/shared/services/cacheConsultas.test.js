// Caché de consultas: no repite pedidos al cambiar de pantalla, pero nunca mezcla sesiones ni guarda errores.
import { afterEach, describe, expect, it, vi } from 'vitest'
import { conCache, limpiarCache, olvidarCache } from './cacheConsultas'
import { borrarToken, guardarToken } from './sesionToken'

describe('cacheConsultas', () => {
  afterEach(() => {
    limpiarCache()
    borrarToken()
    vi.useRealTimers()
  })

  it('la segunda vez responde de la caché sin volver a pedir', async () => {
    const pedir = vi.fn().mockResolvedValue(['Pro'])

    await expect(conCache('planes', pedir)).resolves.toEqual(['Pro'])
    await expect(conCache('planes', pedir)).resolves.toEqual(['Pro'])

    expect(pedir).toHaveBeenCalledTimes(1)
  })

  it('cuando vence, vuelve a pedir', async () => {
    vi.useFakeTimers()
    const pedir = vi.fn().mockResolvedValue(1)

    await conCache('planes', pedir, { duracion: 1000 })
    vi.advanceTimersByTime(1001)
    await conCache('planes', pedir, { duracion: 1000 })

    expect(pedir).toHaveBeenCalledTimes(2)
  })

  it('si el pedido falla no guarda nada y el siguiente intento pregunta otra vez', async () => {
    const pedir = vi.fn().mockRejectedValueOnce(new Error('sin conexión')).mockResolvedValueOnce('ok')

    await expect(conCache('kits', pedir)).rejects.toThrow('sin conexión')
    await expect(conCache('kits', pedir)).resolves.toBe('ok')
  })

  it('otro usuario no ve lo guardado por la sesión anterior', async () => {
    guardarToken('token-de-rosa')
    await conCache('perfil', () => Promise.resolve('Panadería Doña Rosa'))

    guardarToken('token-de-luis')
    const pedir = vi.fn().mockResolvedValue('Ferretería Luis')

    await expect(conCache('perfil', pedir)).resolves.toBe('Ferretería Luis')
    expect(pedir).toHaveBeenCalled()
  })

  it('después de guardar cambios, olvidarCache hace que se lea lo nuevo', async () => {
    const pedir = vi.fn().mockResolvedValueOnce('viejo').mockResolvedValueOnce('nuevo')

    await conCache('perfil', pedir)
    olvidarCache('perfil')

    await expect(conCache('perfil', pedir)).resolves.toBe('nuevo')
  })
})
