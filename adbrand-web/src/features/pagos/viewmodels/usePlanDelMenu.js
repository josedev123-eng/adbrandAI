// ViewModel: el plan de la empresa que se muestra debajo del nombre en el menú ("Plan Gratis", "Plan Pro"…).
// Se vuelve a pedir al cambiar de pantalla, así se actualiza solo después de pagar un plan.
import { useEffect, useState } from 'react'
import { obtenerConsumoMes } from '../models/pagosApi'

export function nombrePlan(consumo) {
  if (!consumo) return null
  if (consumo.origen === 'PLAN' && consumo.plan) {
    return /^plan\b/i.test(consumo.plan) ? consumo.plan : `Plan ${consumo.plan}`
  }
  if (consumo.origen === 'GRATUITO') return 'Plan Gratis'
  return null
}

export function usePlanDelMenu(conSesion, pantalla) {
  const [plan, setPlan] = useState(null)

  useEffect(() => {
    if (!conSesion) return undefined
    let vigente = true
    obtenerConsumoMes()
      .then((consumo) => {
        if (vigente) setPlan(nombrePlan(consumo))
      })
      .catch(() => {})
    return () => {
      vigente = false
    }
  }, [conSesion, pantalla])

  return conSesion ? plan : null
}
