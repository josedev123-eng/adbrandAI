// ViewModel: carga la bandeja de contenidos dudosos (HU-14).
import { useEffect, useState } from 'react'
import { listarDudosos } from '../models/moderacionApi'

const ERROR_CARGA = 'No se pudo cargar la bandeja. Revisa que el servidor de administración esté encendido.'

export function useBandejaRevision() {
  const [contenidos, setContenidos] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [version, setVersion] = useState(0)
  const [seleccionadoId, setSeleccionadoId] = useState(null)

  // Se vuelve a ejecutar cada vez que cambia "version" (botón Actualizar).
  useEffect(() => {
    listarDudosos()
      .then((datos) => {
        setContenidos(datos)
        setError(null)
      })
      .catch(() => setError(ERROR_CARGA))
      .finally(() => setCargando(false))
  }, [version])

  function recargar() {
    setCargando(true)
    setVersion((actual) => actual + 1)
  }

  // HU 14, criterio 2: al abrir un contenido se muestra su detalle.
  function abrir(id) {
    setSeleccionadoId(id)
  }

  function volverALaBandeja() {
    setSeleccionadoId(null)
  }

  return { contenidos, cargando, error, recargar, seleccionadoId, abrir, volverALaBandeja }
}