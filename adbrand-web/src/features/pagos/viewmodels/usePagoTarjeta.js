// ViewModel: valida la tarjeta en el navegador y llama a la pasarela (HU 26).
import { useState } from 'react'
import { pagarConTarjeta } from '../models/pagosApi'

const FORMULARIO_VACIO = { numeroTarjeta: '', titular: '', vencimiento: '', cvv: '' }

export function soloDigitos(valor) {
  return valor.replace(/\D/g, '')
}

// Mismo algoritmo de Luhn que usa el backend, para avisar antes de llamar a la API.
export function pasaAlgoritmoLuhn(numero) {
  const digitos = soloDigitos(numero)
  if (digitos.length < 13 || digitos.length > 19) return false

  let suma = 0
  let duplicar = false
  for (let i = digitos.length - 1; i >= 0; i -= 1) {
    let digito = Number(digitos[i])
    if (duplicar) {
      digito *= 2
      if (digito > 9) digito -= 9
    }
    suma += digito
    duplicar = !duplicar
  }
  return suma % 10 === 0
}

export function vencimientoVigente(valor) {
  if (!/^(0[1-9]|1[0-2])\/\d{2}$/.test(valor)) return false
  const [mes, anio] = valor.split('/').map(Number)
  const hoy = new Date()
  const primerDiaDeEsteMes = new Date(hoy.getFullYear(), hoy.getMonth(), 1)
  return new Date(2000 + anio, mes, 1) > primerDiaDeEsteMes
}

export function validarTarjeta(formulario) {
  const errores = {}
  if (!formulario.numeroTarjeta.trim()) {
    errores.numeroTarjeta = 'Escribe el número de la tarjeta.'
  } else if (!pasaAlgoritmoLuhn(formulario.numeroTarjeta)) {
    errores.numeroTarjeta = 'El número de la tarjeta no es válido.'
  }
  if (!formulario.titular.trim()) {
    errores.titular = 'Escribe el nombre del titular.'
  } else if (formulario.titular.trim().length < 5) {
    errores.titular = 'El nombre del titular tiene entre 5 y 120 caracteres.'
  }
  if (!formulario.vencimiento.trim()) {
    errores.vencimiento = 'Escribe el vencimiento (MM/AA).'
  } else if (!vencimientoVigente(formulario.vencimiento)) {
    errores.vencimiento = 'Usa una tarjeta vigente con formato MM/AA.'
  }
  if (!/^\d{3,4}$/.test(formulario.cvv)) {
    errores.cvv = 'El CVV tiene 3 o 4 dígitos.'
  }
  return errores
}

export function usePagoTarjeta(plan, alPagar) {
  const [formulario, setFormulario] = useState(FORMULARIO_VACIO)
  const [errores, setErrores] = useState({})
  const [mensaje, setMensaje] = useState(null)
  const [enviando, setEnviando] = useState(false)

  function cambiarCampo(campo, valor) {
    setFormulario((actual) => ({ ...actual, [campo]: valor }))
    setErrores((actuales) => ({ ...actuales, [campo]: undefined }))
  }

  async function pagar() {
    setMensaje(null)
    const locales = validarTarjeta(formulario)
    setErrores(locales)
    if (Object.keys(locales).length > 0) return

    setEnviando(true)
    try {
      const pago = await pagarConTarjeta({
        planId: plan.id,
        numeroTarjeta: soloDigitos(formulario.numeroTarjeta),
        titular: formulario.titular.trim(),
        vencimiento: formulario.vencimiento,
        cvv: formulario.cvv,
      })
      alPagar(pago)
    } catch (error) {
      if (error.campos && Object.keys(error.campos).length > 0) setErrores(error.campos)
      setMensaje({ tipo: 'error', texto: error.message })
    } finally {
      setEnviando(false)
    }
  }

  return { formulario, errores, mensaje, enviando, cambiarCampo, pagar }
}
