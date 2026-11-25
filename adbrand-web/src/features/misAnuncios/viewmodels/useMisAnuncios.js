// ViewModel: "Mis anuncios". Carga los anuncios con su estado y, al abrir la pantalla, marca como vistas las
// decisiones del moderador (el número del menú vuelve a cero).
// HU 63: para que la lista no se llene: "Todos" no muestra los cancelados (van en "Cancelados"), se ven de a 12
// con "Ver más", hay buscador por texto o fecha, y los cancelados se pueden eliminar.
import { useEffect, useState } from 'react'
import {
  cancelarPublicacion,
  eliminarAnuncio,
  listarMisAnuncios,
  marcarAvisosVistos,
  reactivarAnuncio,
} from '../models/misAnunciosApi'

export const FILTROS = [
  { valor: 'TODOS', nombre: 'Todos' },
  { valor: 'APROBADO', nombre: 'Aprobados' },
  { valor: 'DUDOSO', nombre: 'En revisión' },
  { valor: 'RECHAZADO', nombre: 'Rechazados' },
  { valor: 'CANCELADOS', nombre: 'Cancelados' },
]

export const POR_PAGINA = 12

function sinTildes(texto) {
  return String(texto ?? '')
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .toLowerCase()
}

export function filtrar(anuncios, filtro) {
  if (filtro === 'CANCELADOS') return anuncios.filter((a) => a.cancelado)
  const activos = anuncios.filter((a) => !a.cancelado)
  return filtro === 'TODOS' ? activos : activos.filter((a) => a.estado === filtro)
}

// Busca en el titular, el pedido y el texto (sin importar tildes ni mayúsculas) y por día de creación (AAAA-MM-DD).
export function buscar(anuncios, texto, fecha) {
  const palabra = sinTildes(texto).trim()
  return anuncios.filter((a) => {
    const coincideTexto = !palabra || sinTildes(`${a.titular ?? ''} ${a.oferta ?? ''} ${a.texto ?? ''}`).includes(palabra)
    const coincideFecha = !fecha || String(a.fechaCreacion ?? '').startsWith(fecha)
    return coincideTexto && coincideFecha
  })
}

export function useMisAnuncios(alVerAvisos) {
  const [anuncios, setAnuncios] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [filtro, setFiltroInterno] = useState('TODOS')
  const [mostrar, setMostrar] = useState(POR_PAGINA)
  const [busqueda, setBusquedaInterna] = useState('')
  const [fecha, setFechaInterna] = useState('')
  // Aviso después de cancelar, volver a activar o eliminar ({ tipo, texto }).
  const [mensaje, setMensaje] = useState(null)
  const [procesando, setProcesando] = useState(null)

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

  const encontrados = buscar(anuncios, busqueda, fecha)
  const filtrados = filtrar(encontrados, filtro)
  const visibles = filtrados.slice(0, mostrar)
  const cantidades = Object.fromEntries(FILTROS.map(({ valor }) => [valor, filtrar(encontrados, valor).length]))

  // Al cambiar de filtro o de búsqueda se vuelve a empezar por los 12 más recientes.
  function setFiltro(valor) {
    setFiltroInterno(valor)
    setMostrar(POR_PAGINA)
  }
  function setBusqueda(valor) {
    setBusquedaInterna(valor)
    setMostrar(POR_PAGINA)
  }
  function setFecha(valor) {
    setFechaInterna(valor)
    setMostrar(POR_PAGINA)
  }
  function limpiarBusqueda() {
    setBusqueda('')
    setFecha('')
  }

  function verMas() {
    setMostrar((actual) => actual + POR_PAGINA)
  }

  function marcar(id, cancelado) {
    setAnuncios((lista) => lista.map((a) => (a.id === id ? { ...a, cancelado } : a)))
  }

  async function ejecutar(id, accion, textoError) {
    setMensaje(null)
    setProcesando(id)
    try {
      await accion()
    } catch (falla) {
      setMensaje({ tipo: 'error', texto: falla?.message || textoError })
    } finally {
      setProcesando(null)
    }
  }

  function cancelar(id) {
    return ejecutar(
      id,
      async () => {
        await cancelarPublicacion(id)
        marcar(id, true)
        // Corto y claro: lo único que le queda por hacer al dueño.
        setMensaje({ tipo: 'exito', texto: 'Bórralo si ya lo subiste.' })
      },
      'No se pudo cancelar.',
    )
  }

  function reactivar(id) {
    return ejecutar(
      id,
      async () => {
        await reactivarAnuncio(id)
        marcar(id, false)
        setMensaje({ tipo: 'exito', texto: 'Anuncio activado otra vez: ya puedes publicarlo.' })
      },
      'No se pudo volver a activar.',
    )
  }

  function eliminar(id) {
    return ejecutar(
      id,
      async () => {
        await eliminarAnuncio(id)
        setAnuncios((lista) => lista.filter((a) => a.id !== id))
        setMensaje({ tipo: 'exito', texto: 'Anuncio eliminado de tu lista.' })
      },
      'No se pudo eliminar.',
    )
  }

  return {
    anuncios: visibles,
    hayMas: filtrados.length > visibles.length,
    restantes: filtrados.length - visibles.length,
    verMas,
    cantidades,
    cargando,
    error,
    filtro,
    setFiltro,
    busqueda,
    setBusqueda,
    fecha,
    setFecha,
    limpiarBusqueda,
    mensaje,
    procesando,
    cancelar,
    reactivar,
    eliminar,
  }
}
