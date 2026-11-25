// ViewModel: mensajes de Contacto. Filtra los pendientes y marca cada uno como atendido.
import { useCallback, useEffect, useState } from 'react'
import { listarMensajes, marcarAtendido } from '../models/contactoApi'

export function useMensajesContacto() {
  const [mensajes, setMensajes] = useState([])
  const [soloPendientes, setSoloPendientes] = useState(true)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [guardando, setGuardando] = useState(null)

  const cargar = useCallback(async (pendientes) => {
    setCargando(true)
    setError(null)
    try {
      setMensajes((await listarMensajes({ pendientes })) ?? [])
    } catch (falla) {
      setError(falla?.message || 'No se pudieron cargar los mensajes.')
    } finally {
      setCargando(false)
    }
  }, [])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- carga inicial y al cambiar el filtro
    cargar(soloPendientes)
  }, [soloPendientes, cargar])

  async function cambiarAtendido(mensaje) {
    setGuardando(mensaje.id)
    setError(null)
    try {
      const actualizado = await marcarAtendido(mensaje.id, !mensaje.atendido)
      setMensajes((lista) =>
        soloPendientes && actualizado.atendido
          ? lista.filter((m) => m.id !== actualizado.id)
          : lista.map((m) => (m.id === actualizado.id ? actualizado : m)),
      )
    } catch (falla) {
      setError(falla?.message || 'No se pudo guardar el cambio.')
    } finally {
      setGuardando(null)
    }
  }

  return { mensajes, soloPendientes, setSoloPendientes, cargando, error, guardando, cambiarAtendido }
}
