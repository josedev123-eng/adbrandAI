// ViewModel: "Mis anuncios". Carga los anuncios con su estado, deja filtrar por estado y, al abrir la pantalla,
// marca como vistas las decisiones del moderador (el número del menú vuelve a cero).
import { useEffect, useState } from 'react'
import { listarMisAnuncios, marcarAvisosVistos } from '../models/misAnunciosApi'

export const FILTROS = [
  { valor: 'TODOS', nombre: 'Todos' },
  { valor: 'APROBADO', nombre: 'Aprobados' },
  { valor: 'DUDOSO', nombre: 'En revisión' },
  { valor: 'RECHAZADO', nombre: 'Rechazados' },
]

export function useMisAnuncios(alVerAvisos) {
  const [anuncios, setAnuncios] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [filtro, setFiltro] = useState('TODOS')

  useEffect(() => {
    let vigente = true
    listarMisAnuncios()
      .then((lista) => {
        if (!vigente) return
        setAnuncios(lista ?? [])
        // Las decisiones nuevas se resaltan en esta visita y quedan vistas para la próxima.
        if (lista?.some((anuncio) => anuncio.avisoPendiente)) {
          marcarAvisosVistos()
            .then(() => alVerAvisos?.())
            .catch(() => {})
        }
      })
      .catch((falla) => {
        if (vigente) setError(falla?.message || 'No se pudieron cargar tus anuncios.')
      })
      .finally(() => {
        if (vigente) setCargando(false)
      })
    return () => {
      vigente = false
    }
    // Solo al abrir la pantalla.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const visibles = filtro === 'TODOS' ? anuncios : anuncios.filter((anuncio) => anuncio.estado === filtro)
  const cantidades = Object.fromEntries(
    FILTROS.map(({ valor }) => [valor, valor === 'TODOS' ? anuncios.length : anuncios.filter((a) => a.estado === valor).length]),
  )

  return { anuncios: visibles, cantidades, cargando, error, filtro, setFiltro }
}
