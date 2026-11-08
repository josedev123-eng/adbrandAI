// ViewModel: datos de las tarjetas de la pantalla de Inicio del administrador.
// Cada tarjeta se pide por separado (si una falla, las demás se muestran igual) y solo si el rol ve esa pantalla.
// Usa los mismos endpoints de Django que las pantallas de cada tema; no hay endpoint nuevo.
import { useEffect, useState } from 'react'
import { listarDudosos } from '../../moderacion/models/moderacionApi'
import { verificarEstado } from '../../servidor/models/servidorApi'
import { obtenerIngresosMensuales, obtenerSuscripcionesActivas } from '../../suscripciones/models/suscripcionesApi'
import { obtenerConsumoActual } from '../../tokens/models/tokensApi'

// Qué pide cada tarjeta y qué pantalla tiene que ver el rol para mostrarla.
const TARJETAS = {
  pendientes: {
    pantalla: 'revision',
    pedir: () => listarDudosos().then((datos) => ({ total: datos?.length ?? 0 })),
  },
  suscripciones: {
    pantalla: 'panel-financiero',
    pedir: () => obtenerSuscripcionesActivas().then((datos) => ({ total: datos?.total ?? 0 })),
  },
  ingresos: {
    pantalla: 'panel-financiero',
    // Los ingresos llegan del mes más antiguo al actual; el último es el mes en curso.
    pedir: () =>
      obtenerIngresosMensuales().then((filas) => {
        const ultimo = filas?.[filas.length - 1]
        return { mes: ultimo?.mes ?? null, total: Number(ultimo?.total ?? 0) }
      }),
  },
  tokens: {
    pantalla: 'consumo-tokens',
    pedir: () =>
      obtenerConsumoActual().then((datos) => ({
        total: datos?.total ?? 0,
        empresas: datos?.empresas?.length ?? 0,
        alertas: datos?.alertas?.length ?? 0,
      })),
  },
  servidor: {
    pantalla: 'estado-servidor',
    pedir: () =>
      verificarEstado().then((datos) => ({
        verificadoEn: datos?.verificado_en ?? null,
        caidos: (datos?.servicios ?? []).filter((servicio) => servicio.estado !== 'ACTIVO').map((servicio) => servicio.nombre),
      })),
  },
}

const CARGANDO = { cargando: true, error: false, datos: null }

// Las tarjetas que le tocan al rol, según las pantallas que ve en el menú.
export function tarjetasDelRol(pantallas) {
  const ids = new Set(pantallas.map((pantalla) => pantalla.id))
  return Object.keys(TARJETAS).filter((clave) => ids.has(TARJETAS[clave].pantalla))
}

export function pantallaDeTarjeta(clave) {
  return TARJETAS[clave].pantalla
}

export function useInicio(claves) {
  const firma = claves.join(',')
  const [tarjetas, setTarjetas] = useState(() => Object.fromEntries(claves.map((clave) => [clave, CARGANDO])))
  const [version, setVersion] = useState(0)

  // Se vuelve a pedir todo al pulsar Actualizar ("version").
  useEffect(() => {
    let vigente = true
    for (const clave of firma ? firma.split(',') : []) {
      TARJETAS[clave]
        .pedir()
        .then((datos) => {
          if (vigente) setTarjetas((actual) => ({ ...actual, [clave]: { cargando: false, error: false, datos } }))
        })
        .catch(() => {
          if (vigente) setTarjetas((actual) => ({ ...actual, [clave]: { cargando: false, error: true, datos: null } }))
        })
    }
    return () => {
      vigente = false
    }
  }, [firma, version])

  function recargar() {
    setTarjetas(Object.fromEntries(claves.map((clave) => [clave, CARGANDO])))
    setVersion((actual) => actual + 1)
  }

  return { tarjetas, recargar }
}
