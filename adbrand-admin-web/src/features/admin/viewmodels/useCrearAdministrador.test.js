// HU-01: validaciones del formulario para crear administradores (sin necesitar Django encendido).
import { act, renderHook, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { crearAdministrador, listarRoles } from '../models/administradoresApi'
import { useCrearAdministrador, validar, validarContrasena } from './useCrearAdministrador'

vi.mock('../models/administradoresApi', () => ({
  crearAdministrador: vi.fn(),
  listarRoles: vi.fn(),
}))

const VALIDO = {
  nombres: 'María José',
  apellidos: 'De la Cruz',
  correo: 'maria@adbrand.pe',
  contrasena: 'Secreta123',
  rol_id: '2',
}

describe('Validaciones al crear administrador (HU-01)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    listarRoles.mockResolvedValue([{ id: 2, nombre: 'MODERADOR' }])
  })

  it('acepta un formulario con datos válidos', () => {
    expect(validar(VALIDO)).toEqual({})
  })

  it('rechaza contraseñas débiles y dice qué falta', () => {
    expect(validarContrasena('Sec12')).toBe('Mínimo 8 caracteres.')
    expect(validarContrasena('12345678')).toBe('Debe tener al menos una letra mayúscula.')
    expect(validarContrasena('SECRETA123')).toBe('Debe tener al menos una letra minúscula.')
    expect(validarContrasena('Secretaaa')).toBe('Debe tener al menos un número.')
    expect(validarContrasena('Secreta 123')).toBe('La contraseña no puede tener espacios.')
    expect(validarContrasena('Secreta123')).toBeUndefined()
  })

  it('rechaza nombres y apellidos con números o símbolos', () => {
    const errores = validar({ ...VALIDO, nombres: 'Juan123', apellidos: '@@@' })

    expect(errores.nombres).toBe('Usa solo letras y espacios, sin números ni símbolos.')
    expect(errores.apellidos).toBe('Usa solo letras y espacios, sin números ni símbolos.')
  })

  it('pide al menos 2 letras en nombres y apellidos', () => {
    const errores = validar({ ...VALIDO, nombres: 'A', apellidos: '   ' })

    expect(errores.nombres).toBe('Debe tener al menos 2 letras.')
    expect(errores.apellidos).toBe('Ingresa los apellidos.')
  })

  it('con una contraseña débil no llama a la API', async () => {
    const { result } = renderHook(() => useCrearAdministrador())
    await waitFor(() => expect(result.current.roles).toHaveLength(1))

    for (const [campo, valor] of Object.entries({ ...VALIDO, contrasena: '12345678' })) {
      act(() => result.current.cambiarCampo(campo, valor))
    }
    await act(async () => {
      await result.current.enviar()
    })

    expect(result.current.errores.contrasena).toBe('Debe tener al menos una letra mayúscula.')
    expect(crearAdministrador).not.toHaveBeenCalled()
  })
})
