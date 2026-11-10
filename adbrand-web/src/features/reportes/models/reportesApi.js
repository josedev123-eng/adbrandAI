// Model: llamadas a la API de informes (HU-37).
// No llevan id: Spring usa siempre la empresa del usuario que inició sesión.
import { apiDescargar, apiRequest } from '../../../shared/services/apiClient'

// HU-40: { desde, hasta, campanaId } → "?desde=2026-11-01&hasta=2026-11-30&campanaId=3". Los vacíos no se envían.
export function consultaFiltros(filtros = {}) {
  const parametros = new URLSearchParams()
  for (const campo of ['desde', 'hasta', 'campanaId']) {
    if (filtros[campo]) parametros.set(campo, filtros[campo])
  }
  const texto = parametros.toString()
  return texto ? `?${texto}` : ''
}

// Devuelve { empresa, total, aprobados, dudosos, rechazados, campanas: [{ id, fecha, redSocial, oferta, estado }] }.
export function obtenerInformeCampanas(filtros) {
  return apiRequest(`/reportes/campanas${consultaFiltros(filtros)}`)
}

// HU-38: el mismo informe como PDF (Blob). Spring lo arma con los mismos datos de obtenerInformeCampanas.
// HU-40: con los mismos filtros que la pantalla.
export function descargarInformePdf(filtros) {
  return apiDescargar(`/reportes/campanas/pdf${consultaFiltros(filtros)}`)
}

// "informe-campanas-2026-11-03.pdf", con la fecha de hoy (igual que el nombre que pone Spring).
export function nombreArchivoInforme(extension, hoy = new Date()) {
  const dos = (numero) => String(numero).padStart(2, '0')
  return `informe-campanas-${hoy.getFullYear()}-${dos(hoy.getMonth() + 1)}-${dos(hoy.getDate())}.${extension}`
}

// Hace que el navegador guarde el archivo recibido con ese nombre.
export function guardarArchivo(blob, nombre) {
  const url = URL.createObjectURL(blob)
  const enlace = document.createElement('a')
  enlace.href = url
  enlace.download = nombre
  document.body.appendChild(enlace)
  enlace.click()
  enlace.remove()
  URL.revokeObjectURL(url)
}

export const NOMBRES_RED = { INSTAGRAM: 'Instagram', FACEBOOK: 'Facebook' }
export const NOMBRES_ESTADO = { APROBADO: 'Aprobado', DUDOSO: 'En revisión', RECHAZADO: 'Rechazado' }

// "2026-11-03T10:05:00" → "03/11/2026 10:05"
export function formatearFecha(texto) {
  const fecha = new Date(texto)
  const dos = (numero) => String(numero).padStart(2, '0')
  return `${dos(fecha.getDate())}/${dos(fecha.getMonth() + 1)}/${fecha.getFullYear()} ${dos(fecha.getHours())}:${dos(fecha.getMinutes())}`
}
