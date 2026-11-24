// Model: llamadas a la API del perfil del negocio (HU-09).
import { apiRequest } from '../../../shared/services/apiClient'
import { conCache, olvidarDespues } from '../../../shared/services/cacheConsultas'

export const RUBROS = [
  'Panadería y pastelería',
  'Restaurante y comida',
  'Bodega y minimarket',
  'Ropa y calzado',
  'Belleza y cuidado personal',
  'Salud y bienestar',
  'Tecnología',
  'Educación',
  'Servicios profesionales',
  'Otro',
]

// Los mismos valores que el enum Tono de Spring.
export const TONOS = [
  { valor: 'CERCANO', nombre: 'Cercano', ejemplo: 'Como un vecino que te recomienda algo.' },
  { valor: 'PROFESIONAL', nombre: 'Profesional', ejemplo: 'Serio, claro y confiable.' },
  { valor: 'DIVERTIDO', nombre: 'Divertido', ejemplo: 'Con humor y mucha energía.' },
  { valor: 'ELEGANTE', nombre: 'Elegante', ejemplo: 'Sobrio, cuidado y exclusivo.' },
]

// El perfil lo leen Inicio y otras pantallas; se guarda unos minutos y se olvida al guardar cambios.
export function obtenerPerfil() {
  return conCache('perfil', () => apiRequest('/negocio/perfil'))
}

export function guardarPerfil(datos) {
  return apiRequest('/negocio/perfil', { method: 'PUT', body: datos }).then(olvidarDespues('perfil'))
}