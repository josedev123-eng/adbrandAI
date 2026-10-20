// Model: llamadas a la API de cuentas del usuario PYME (HU-21).
import { apiRequest } from '../../../shared/services/apiClient'

// Responde 201 con { id, nombre, nombreEmpresa, correo }; 409 CORREO_REPETIDO si el correo ya existe.
export function registrar(datos) {
  return apiRequest('/auth/registro', { method: 'POST', body: datos })
}
