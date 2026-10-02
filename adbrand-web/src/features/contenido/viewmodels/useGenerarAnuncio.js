// ViewModel: pide el anuncio a la IA, maneja los errores y permite copiarlo (HU-10).
import { useState } from 'react'
import { generarAnuncio } from '../models/anunciosApi'

export const MAXIMO_OFERTA = 300

function validar(solicitud) {
  const errores = {}
  if (!solicitud.oferta.trim()) errores.oferta = 'Escribe la oferta que quieres anunciar.'
  else if (solicitud.oferta.length > MAXIMO_OFERTA) errores.oferta = `Máximo ${MAXIMO_OFERTA} caracteres.`
  if (!solicitud.redSocial) errores.redSocial = 'Selecciona la red social.'
  return errores
}

export function useGenerarAnuncio() {
  const [solicitud, setSolicitud] = useState({ oferta: '', redSocial: 'INSTAGRAM' })
  const [errores, setErrores] = useState({})
  const [anuncio, setAnuncio] = useState(null)
  const [aviso, setAviso] = useState(null)
  const [faltaPerfil, setFaltaPerfil] = useState(false)
  const [generando, setGenerando] = useState(false)
  const [copiado, setCopiado] = useState(false)

  function cambiarCampo(campo, valor) {
    setSolicitud((actual) => ({ ...actual, [campo]: valor }))
    setErrores((actuales) => ({ ...actuales, [campo]: undefined }))
  }

  async function generar() {
    setAviso(null)
    setFaltaPerfil(false)
    setCopiado(false)
    const erroresLocales = validar(solicitud)
    setErrores(erroresLocales)
    if (Object.keys(erroresLocales).length > 0) return

    setGenerando(true)
    try {
      setAnuncio(await generarAnuncio(solicitud))
    } catch (error) {
      if (error.codigo === 'PERFIL_NO_ENCONTRADO') {
        // Criterio 1: primero hay que registrar el perfil del negocio (HU 9).
        setFaltaPerfil(true)
      } else if (error.status === 400) {
        setErrores(error.campos)
      } else if (error.codigo === 'IA_NO_DISPONIBLE') {
        // Criterio 2: si la IA falla, se muestra un mensaje claro.
        setAviso(error.message)
      } else {
        setAviso('No se pudo conectar con el servidor.')
      }
    } finally {
      setGenerando(false)
    }
  }

  async function copiar() {
    try {
      await navigator.clipboard.writeText(anuncio.texto)
      setCopiado(true)
    } catch {
      setAviso('No se pudo copiar. Selecciona el texto y cópialo con Ctrl + C.')
    }
  }

  return { solicitud, errores, anuncio, aviso, faltaPerfil, generando, copiado, cambiarCampo, generar, copiar }
}