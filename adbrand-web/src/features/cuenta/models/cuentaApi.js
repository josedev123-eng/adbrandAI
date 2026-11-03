// Model: llamadas a la API de los datos de la empresa (HU-23).
// No llevan id: Spring usa siempre la empresa del usuario que inició sesión.
import { apiRequest } from '../../../shared/services/apiClient'

export function obtenerEmpresa() {
  return apiRequest('/cuenta/empresa')
}

// 400 con "campos" si algo está mal escrito; 409 RUC_REPETIDO si otra cuenta ya tiene ese RUC.
export function guardarEmpresa(datos) {
  return apiRequest('/cuenta/empresa', { method: 'PUT', body: datos })
}
