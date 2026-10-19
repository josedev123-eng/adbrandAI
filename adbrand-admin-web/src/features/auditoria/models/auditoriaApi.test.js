// HU-04: cada acción que Django guarda en auditoria_admin tiene un nombre para mostrar.
import { describe, expect, it } from 'vitest'
import { NOMBRES_ACCION } from './auditoriaApi'

// Las mismas acciones que registra Django (ver CONTEXTO.md, "Qué se registra en la auditoría").
const ACCIONES_DE_DJANGO = [
  'CREAR_ADMIN',
  'DESACTIVAR_ADMIN',
  'CAMBIAR_ROL',
  'APROBAR_CONTENIDO',
  'RECHAZAR_CONTENIDO',
]

describe('NOMBRES_ACCION (HU-04)', () => {
  it('ninguna acción de Django se muestra sin nombre', () => {
    for (const accion of ACCIONES_DE_DJANGO) {
      expect(NOMBRES_ACCION[accion], accion).toBeTruthy()
    }
    expect(NOMBRES_ACCION.CREAR_ADMIN).toBe('Creó una cuenta')
  })
})
