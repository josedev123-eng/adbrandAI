// Model: anuncios de la empresa para elegir cuál programar (HU 34).
// Usa el informe de la HU 37, que ya devuelve cada anuncio con su red social, oferta y estado.
import { apiRequest } from '../../../shared/services/apiClient'

// Devuelve [{ id, fecha, redSocial, oferta, estado }], del más reciente al más antiguo.
export async function listarAnuncios() {
  const informe = await apiRequest('/reportes/campanas')
  return informe?.campanas ?? []
}
