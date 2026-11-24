// ViewModel: datos de la pantalla de Inicio de la PYME. Cada tarjeta se pide por separado
// (si una falla, las demás se muestran igual). Usa los mismos endpoints que las otras pantallas.
import { useEffect, useState } from 'react'
import { leerPaleta, listarKitsMarca } from '../../brandkit/models/brandkitApi'
import { obtenerCalendario } from '../../calendario/models/calendarioApi'
import { obtenerPerfil } from '../../negocio/models/perfilNegocioApi'
import { obtenerConsumoMes } from '../../pagos/models/pagosApi'
import { obtenerInformeCampanas } from '../../reportes/models/reportesApi'

const CARGANDO = { cargando: true, error: false, datos: null }

// "2026-11" del mes de una fecha.
function mesDe(fecha) {
  return `${fecha.getFullYear()}-${String(fecha.getMonth() + 1).padStart(2, '0')}`
}

function hoyIso(fecha) {
  return `${mesDe(fecha)}-${String(fecha.getDate()).padStart(2, '0')}`
}

// Las próximas publicaciones (de hoy en adelante), del mes actual y del siguiente, de la más cercana a la más lejana.
export function proximasPublicaciones(publicaciones, hoy, cantidad = 3) {
  return publicaciones
    .filter((publicacion) => publicacion.fecha >= hoy && publicacion.estado !== 'PUBLICADA')
    .sort((a, b) => `${a.fecha}T${a.hora ?? ''}`.localeCompare(`${b.fecha}T${b.hora ?? ''}`))
    .slice(0, cantidad)
}

const PEDIDOS = {
  perfil: () => obtenerPerfil(),
  anuncios: async () => {
    const informe = await obtenerInformeCampanas()
    return {
      total: informe.total ?? 0,
      aprobados: informe.aprobados ?? 0,
      enRevision: informe.dudosos ?? 0,
      ultimo: informe.campanas?.[0] ?? null,
    }
  },
  kit: async () => {
    const kits = await listarKitsMarca()
    if (!kits?.length) return null
    const kit = [...kits].sort((a, b) => (b.id ?? 0) - (a.id ?? 0))[0]
    return { id: kit.id, nombre: kit.nombreNegocio, colores: leerPaleta(kit.paletaColores).slice(0, 5) }
  },
  // Tokens de IA usados este mes frente al límite de su plan (o del plan gratuito).
  consumo: () => obtenerConsumoMes(),
  calendario: async (ahora) => {
    const siguiente = new Date(ahora.getFullYear(), ahora.getMonth() + 1, 1)
    const [esteMes, proximoMes] = await Promise.all([obtenerCalendario(mesDe(ahora)), obtenerCalendario(mesDe(siguiente))])
    return proximasPublicaciones([...(esteMes ?? []), ...(proximoMes ?? [])], hoyIso(ahora))
  },
}

export function useInicio(ahora = new Date()) {
  const [tarjetas, setTarjetas] = useState(() => Object.fromEntries(Object.keys(PEDIDOS).map((clave) => [clave, CARGANDO])))
  const [version, setVersion] = useState(0)

  useEffect(() => {
    let vigente = true
    for (const [clave, pedir] of Object.entries(PEDIDOS)) {
      pedir(ahora)
        .then((datos) => {
          if (vigente) setTarjetas((actual) => ({ ...actual, [clave]: { cargando: false, error: false, datos } }))
        })
        .catch((error) => {
          // Sin perfil registrado, Spring responde 404 PERFIL_NO_ENCONTRADO: no es un error, es "aún no lo creaste".
          const sinDatos = error?.codigo === 'PERFIL_NO_ENCONTRADO'
          if (vigente) {
            setTarjetas((actual) => ({ ...actual, [clave]: { cargando: false, error: !sinDatos, datos: null } }))
          }
        })
    }
    return () => {
      vigente = false
    }
    // "ahora" se fija al abrir la pantalla; solo se vuelve a pedir al pulsar Actualizar.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [version])

  function recargar() {
    setTarjetas(Object.fromEntries(Object.keys(PEDIDOS).map((clave) => [clave, CARGANDO])))
    setVersion((actual) => actual + 1)
  }

  return { tarjetas, recargar }
}
