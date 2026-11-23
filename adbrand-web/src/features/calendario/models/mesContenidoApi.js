// Model: HU 63 "Tu mes de contenido" (fechas peruanas con ideas según el rubro y plan de publicaciones).
import { apiRequest } from '../../../shared/services/apiClient'

// Sin anio/mes devuelve el mes actual (hora de Lima).
export function obtenerMesContenido(anio, mes) {
  const consulta = anio && mes ? `?anio=${anio}&mes=${mes}` : ''
  return apiRequest(`/calendario/mes-de-contenido${consulta}`)
}
