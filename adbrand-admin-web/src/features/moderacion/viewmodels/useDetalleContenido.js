// ViewModel: carga el detalle completo de un contenido y el motivo de la observación (HU-14).
// Incluye acciones para aprobar o rechazar el contenido (HU-15).
import { useEffect, useState } from 'react'
import { obtenerContenido, moderarContenido, actualizarEstado } from '../models/moderacionApi'

export function useDetalleContenido(id, onActualizado) {
  const [contenido, setContenido] = useState(null)
  const [error, setError] = useState(null)
  const [moderando, setModerando] = useState(false)

  useEffect(() => {
    obtenerContenido(id)
      .then(setContenido)
      .catch((e) =>
        setError(e.status === 404 ? 'Este contenido ya no existe.' : 'No se pudo cargar el detalle del contenido.'),
      )
  }, [id])

  async function aprobar(moderadorId) {
    if (!window.confirm('¿Confirmas aprobar este contenido?')) return null

    setModerando(true)
    setError(null)
    try {
      const actualizado = await actualizarEstado(id, 'APROBADO', moderadorId)
      setContenido(actualizado)
      onActualizado?.()
      return actualizado
    } catch (e) {
      setError(e.detalles?.detail || e.message || 'No se pudo aprobar el contenido.')
      throw e
    } finally {
      setModerando(false)
    }
  }

  async function rechazar(moderadorId, motivoRechazo) {
    if (!window.confirm('¿Confirmas rechazar este contenido?')) return null
    if (!motivoRechazo.trim()) {
      setError('El motivo de rechazo es obligatorio.')
      return null
    }

    setModerando(true)
    setError(null)
    try {
      const actualizado = await actualizarEstado(id, 'RECHAZADO', moderadorId, motivoRechazo)
      setContenido(actualizado)
      onActualizado?.()
      return actualizado
    } catch (e) {
      setError(e.detalles?.detail || e.message || 'No se pudo rechazar el contenido.')
      throw e
    } finally {
      setModerando(false)
    }
  }

  return { contenido, error, cargando: !contenido && !error, aprobar, rechazar, moderando }
}