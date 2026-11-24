// Validaciones del registro: nombre de persona, nombre de empresa y correo normalizado.
import { describe, expect, it } from 'vitest'
import { normalizarCorreo, problemaDeEmpresa, problemaDeNombre } from './validaciones'

describe('validaciones de nombres', () => {
  it('el nombre de persona acepta tildes y ñ, pero no números ni símbolos', () => {
    expect(problemaDeNombre('María Peña')).toBeNull()
    expect(problemaDeNombre("D'Angelo")).toBeNull()
    expect(problemaDeNombre('R2D2')).toBe('Usa solo letras y espacios, sin números ni símbolos.')
    expect(problemaDeNombre('<script>')).toBe('Usa solo letras y espacios, sin números ni símbolos.')
    expect(problemaDeNombre(' ')).toBe('Ingresa tu nombre.')
    expect(problemaDeNombre('A')).toBe('Debe tener al menos 2 letras.')
  })

  it('el nombre de empresa acepta números y signos simples', () => {
    expect(problemaDeEmpresa('Panadería & Café 24 S.A.C.')).toBeNull()
    expect(problemaDeEmpresa('Tienda <b>')).toBe('Usa letras, números y signos simples (& . , -).')
    expect(problemaDeEmpresa('x'.repeat(151))).toBe('Máximo 150 caracteres.')
  })

  it('el correo se guarda sin espacios y en minúsculas', () => {
    expect(normalizarCorreo('  Rosa@Panaderia.PE ')).toBe('rosa@panaderia.pe')
  })
})
