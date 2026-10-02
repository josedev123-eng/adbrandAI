// ViewModel: carga el detalle completo de un contenido y el motivo de la observación (HU-14).
import { useEffect, useState } from 'react'
import { obtenerContenido } from '../models/moderacionApi'

export function useDetalleContenido(id) {
  const [contenido, setContenido] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    obtenerContenido(id)
      .then(setContenido)
      .catch((e) =>
        setError(e.status === 404 ? 'Este contenido ya no existe.' : 'No se pudo cargar el detalle del contenido.'),
      )
  }, [id])

  return { contenido, error, cargando: !contenido && !error }
}