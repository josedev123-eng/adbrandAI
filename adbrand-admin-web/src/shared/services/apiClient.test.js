// Login de administradores: el cliente HTTP manda el token y avisa cuando vence.
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { apiRequest, EVENTO_SESION_VENCIDA, MENSAJES, mensajePorEstado } from './apiClient'
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

describe('apiClient con errores claros', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.stubGlobal('fetch', vi.fn())
  })
  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('con Django apagado avisa que no se pudo conectar', async () => {
    fetch.mockRejectedValue(new TypeError('Failed to fetch'))

    await expect(apiRequest('/tokens/consumo/')).rejects.toMatchObject({
      codigo: 'SIN_CONEXION',
      status: 0,
      message: MENSAJES.SIN_CONEXION,
    })
  })

  it('usa el "detail" de Django como mensaje y conserva los detalles', async () => {
    fetch.mockReturnValue(respuesta(400, { detail: 'No puedes desactivar tu propia cuenta.' }))

    await expect(apiRequest('/usuarios/1/desactivar/')).rejects.toMatchObject({
      status: 400,
      message: 'No puedes desactivar tu propia cuenta.',
      detalles: { detail: 'No puedes desactivar tu propia cuenta.' },
    })
  })

  it('sin "detail" usa un mensaje según el estado', async () => {
    fetch.mockReturnValue(respuesta(403, {}))
    await expect(apiRequest('/auditoria/')).rejects.toMatchObject({ message: MENSAJES.SIN_PERMISO })

    fetch.mockReturnValue(respuesta(500, null))
    await expect(apiRequest('/auditoria/')).rejects.toMatchObject({ message: mensajePorEstado(500) })
  })
})
