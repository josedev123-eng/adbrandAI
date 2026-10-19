// Login de administradores: el cliente HTTP manda el token y avisa cuando vence.
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { apiRequest, EVENTO_SESION_VENCIDA } from './apiClient'
import { guardarToken, leerToken } from './sesionToken'

function respuesta(status, datos) {
  return Promise.resolve({ ok: status < 400, status, json: () => Promise.resolve(datos) })
}

describe('apiClient con token de sesión', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.stubGlobal('fetch', vi.fn())
  })
  afterEach(() => vi.unstubAllGlobals())

  it('sin token no manda Authorization', async () => {
    fetch.mockReturnValue(respuesta(200, []))
    await apiRequest('/usuarios/roles/')

    expect(fetch.mock.calls[0][1].headers).not.toHaveProperty('Authorization')
  })

  it('con token manda Authorization: Bearer', async () => {
    guardarToken('abc123')
    fetch.mockReturnValue(respuesta(200, []))
    await apiRequest('/usuarios/roles/')

    expect(fetch.mock.calls[0][1].headers.Authorization).toBe('Bearer abc123')
  })

  it('un 401 con token borra la sesión y avisa a la web', async () => {
    guardarToken('abc123')
    fetch.mockReturnValue(respuesta(401, { detail: 'Tu sesión venció o no es válida.' }))
    const alVencer = vi.fn()
    window.addEventListener(EVENTO_SESION_VENCIDA, alVencer)

    await expect(apiRequest('/usuarios/sesion/')).rejects.toMatchObject({ status: 401 })
    expect(leerToken()).toBeNull()
    expect(alVencer).toHaveBeenCalledTimes(1)
    window.removeEventListener(EVENTO_SESION_VENCIDA, alVencer)
  })

  it('un 401 del login (sin token) no dispara el aviso', async () => {
    fetch.mockReturnValue(respuesta(401, { detail: 'Correo o contraseña incorrectos.' }))
    const alVencer = vi.fn()
    window.addEventListener(EVENTO_SESION_VENCIDA, alVencer)

    await expect(apiRequest('/usuarios/login/', { method: 'POST', body: {} })).rejects.toMatchObject({ status: 401 })
    expect(alVencer).not.toHaveBeenCalled()
    window.removeEventListener(EVENTO_SESION_VENCIDA, alVencer)
  })
})
