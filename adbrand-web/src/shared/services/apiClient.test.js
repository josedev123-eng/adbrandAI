// Cliente HTTP: cada falla llega a la pantalla con un mensaje claro en español.
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { apiRequest, EVENTO_SESION_VENCIDA, MENSAJES, mensajePorEstado } from './apiClient'
import { guardarToken, leerToken } from './sesionToken'

function respuesta(status, cuerpo) {
  return Promise.resolve({
    ok: status >= 200 && status < 300,
    status,
    json: () => (cuerpo === undefined ? Promise.reject(new Error('sin cuerpo')) : Promise.resolve(cuerpo)),
  })
}

describe('apiRequest', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    vi.useRealTimers()
    localStorage.clear()
  })

  it('devuelve los datos cuando todo sale bien', async () => {
    fetch.mockReturnValue(respuesta(200, { id: 1 }))

    await expect(apiRequest('/planes')).resolves.toEqual({ id: 1 })
  })

  it('con el servidor apagado avisa que no se pudo conectar', async () => {
    fetch.mockRejectedValue(new TypeError('Failed to fetch'))

    await expect(apiRequest('/planes')).rejects.toMatchObject({
      message: MENSAJES.SIN_CONEXION,
      codigo: 'SIN_CONEXION',
      status: 0,
    })
  })

  it('sin internet lo dice así', async () => {
    fetch.mockRejectedValue(new TypeError('Failed to fetch'))
    vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(false)

    await expect(apiRequest('/planes')).rejects.toMatchObject({ message: MENSAJES.SIN_INTERNET })
  })

  it('si el servidor tarda demasiado corta la espera', async () => {
    vi.useFakeTimers()
    fetch.mockImplementation((url, { signal }) =>
      new Promise((resolver, rechazar) => {
        signal.addEventListener('abort', () => rechazar(Object.assign(new Error('aborted'), { name: 'AbortError' })))
      }),
    )

    const pedido = apiRequest('/planes', { tiempoMaximo: 1000 })
    const verificacion = expect(pedido).rejects.toMatchObject({ codigo: 'TIEMPO_AGOTADO', message: MENSAJES.TIEMPO_AGOTADO })
    await vi.advanceTimersByTimeAsync(1000)
    await verificacion
  })

  it('respeta el mensaje y el código que manda Spring', async () => {
    fetch.mockReturnValue(respuesta(400, { codigo: 'RUC_REPETIDO', mensaje: 'Ese RUC ya está registrado.', campos: { ruc: 'x' } }))

    await expect(apiRequest('/cuenta')).rejects.toMatchObject({
      status: 400,
      codigo: 'RUC_REPETIDO',
      message: 'Ese RUC ya está registrado.',
      campos: { ruc: 'x' },
    })
  })

  it('si el servidor no manda mensaje usa uno según el estado', async () => {
    fetch.mockReturnValue(respuesta(503))

    await expect(apiRequest('/planes')).rejects.toMatchObject({ status: 503, message: MENSAJES.SERVICIO_NO_DISPONIBLE })
  })

  it('un 401 con sesión borra el token y avisa que la sesión venció', async () => {
    guardarToken('token-viejo')
    const escuchar = vi.fn()
    window.addEventListener(EVENTO_SESION_VENCIDA, escuchar)
    fetch.mockReturnValue(respuesta(401, { mensaje: 'Inicia sesión para continuar.' }))

    await expect(apiRequest('/planes')).rejects.toMatchObject({ status: 401 })
    expect(leerToken()).toBeNull()
    expect(escuchar).toHaveBeenCalled()
    window.removeEventListener(EVENTO_SESION_VENCIDA, escuchar)
  })
})

describe('cuenta suspendida', () => {
  beforeEach(() => vi.stubGlobal('fetch', vi.fn()))
  afterEach(() => {
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  it('si el administrador suspendió la cuenta, cierra la sesión y avisa el motivo', async () => {
    guardarToken('token')
    const escuchar = vi.fn()
    window.addEventListener(EVENTO_SESION_VENCIDA, escuchar)
    fetch.mockReturnValue(respuesta(401, { codigo: 'CUENTA_SUSPENDIDA', mensaje: 'Tu cuenta fue suspendida. Motivo: abuso.' }))

    await expect(apiRequest('/planes')).rejects.toMatchObject({ status: 401, codigo: 'CUENTA_SUSPENDIDA' })
    expect(leerToken()).toBeNull()
    expect(escuchar.mock.calls[0][0].detail.motivo).toBe('Tu cuenta fue suspendida. Motivo: abuso.')
    window.removeEventListener(EVENTO_SESION_VENCIDA, escuchar)
  })
})

describe('mensajePorEstado', () => {
  it('tiene un mensaje para cada tipo de error', () => {
    expect(mensajePorEstado(403)).toBe(MENSAJES.SIN_PERMISO)
    expect(mensajePorEstado(404)).toBe(MENSAJES.NO_ENCONTRADO)
    expect(mensajePorEstado(429)).toBe(MENSAJES.DEMASIADOS_INTENTOS)
    expect(mensajePorEstado(500)).toBe(MENSAJES.ERROR_SERVIDOR)
    expect(mensajePorEstado(502)).toBe(MENSAJES.SERVICIO_NO_DISPONIBLE)
  })
})
