// ViewModel: "Generar → Elegir". Pide 4 opciones de anuncio (texto + imagen), deja elegir una
// y la guarda como anuncio. Después se puede copiar o regenerar (HU-10, HU-12).
import { useState } from 'react'
import { elegirOpcion, generarOpciones, regenerarAnuncio, retocarImagen, usarVersion } from '../models/anunciosApi'

export const MAXIMO_OFERTA = 300
export const MAXIMO_INSTRUCCION = 300

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
  const [opciones, setOpciones] = useState([])
  const [anuncio, setAnuncio] = useState(null)
  const [opcionElegida, setOpcionElegida] = useState(null)
  const [aviso, setAviso] = useState(null)
  const [faltaPerfil, setFaltaPerfil] = useState(false)
  const [generando, setGenerando] = useState(false)
  const [eligiendoId, setEligiendoId] = useState(null)
  const [regenerando, setRegenerando] = useState(false)
  const [copiado, setCopiado] = useState(false)
  // Retoques de la imagen elegida y su historial de versiones (v1 = original).
  const [instruccion, setInstruccion] = useState('')
  const [errorInstruccion, setErrorInstruccion] = useState(null)
  const [historial, setHistorial] = useState(null)
  const [retocando, setRetocando] = useState(false)

  function cambiarCampo(campo, valor) {
    setSolicitud((actual) => ({ ...actual, [campo]: valor }))
    setErrores((actuales) => ({ ...actuales, [campo]: undefined }))
  }

  function mostrarError(error, mensajePorDefecto) {
    if (error.codigo === 'PERFIL_NO_ENCONTRADO') {
      // Criterio 1: primero hay que registrar el perfil del negocio (HU 9).
      setFaltaPerfil(true)
    } else if (['IA_NO_DISPONIBLE', 'OPCION_NO_ENCONTRADA', 'OPCION_NO_ELEGIDA', 'VERSION_NO_ENCONTRADA', 'LIMITE_RETOQUES'].includes(error.codigo)) {
      // Criterio 2: si la IA falla (o se llegó al límite de retoques), se muestra un mensaje claro.
      // Va antes del 400 genérico porque OPCION_NO_ELEGIDA y LIMITE_RETOQUES también son 400.
      setAviso(error.message)
    } else if (error.status === 400) {
      setErrores(error.campos ?? {})
    } else {
      setAviso(mensajePorDefecto)
    }
  }

  // Paso 1: generar 4 opciones.
  async function generar() {
    setAviso(null)
    setFaltaPerfil(false)
    setCopiado(false)
    const erroresLocales = validar(solicitud)
    setErrores(erroresLocales)
    if (Object.keys(erroresLocales).length > 0) return

    setGenerando(true)
    setOpciones([])
    setAnuncio(null)
    setOpcionElegida(null)
    setHistorial(null)
    setInstruccion('')
    try {
      const respuesta = await generarOpciones(solicitud)
      setOpciones(respuesta.opciones ?? [])
    } catch (error) {
      mostrarError(error, 'No se pudo conectar con el servidor.')
    } finally {
      setGenerando(false)
    }
  }

  // Paso 2: elegir una opción; se guarda como anuncio y pasa por el filtro (HU 13).
  async function elegir(opcionId) {
    setAviso(null)
    setCopiado(false)
    setEligiendoId(opcionId)
    try {
      const respuesta = await elegirOpcion(opcionId)
      setAnuncio(respuesta.anuncio)
      setOpcionElegida(respuesta.opcion)
      setHistorial(null)
      setInstruccion('')
      setOpciones((actuales) => actuales.map((opcion) => ({ ...opcion, elegida: opcion.id === opcionId })))
    } catch (error) {
      mostrarError(error, 'No se pudo guardar la opción elegida.')
    } finally {
      setEligiendoId(null)
    }
  }

  // HU 12: regenerar el texto del anuncio elegido con los mismos parámetros (la imagen se mantiene).
  async function regenerar() {
    if (!anuncio) return

    setAviso(null)
    setCopiado(false)
    setRegenerando(true)
    try {
      setAnuncio(await regenerarAnuncio(anuncio.id))
    } catch (error) {
      if (error.codigo === 'IA_NO_DISPONIBLE') {
        setAviso(error.message)
      } else if (error.status === 404) {
        setAviso('El contenido original ya no existe.')
      } else {
        setAviso('No se pudo regenerar el contenido.')
      }
    } finally {
      setRegenerando(false)
    }
  }

  function cambiarInstruccion(valor) {
    setInstruccion(valor)
    setErrorInstruccion(null)
  }

  // Aplica la instrucción corta sobre la imagen actual y crea una versión nueva.
  async function retocar() {
    if (!opcionElegida) return
    const texto = instruccion.trim()
    if (!texto) {
      setErrorInstruccion('Escribe qué quieres cambiar.')
      return
    }
    if (texto.length > MAXIMO_INSTRUCCION) {
      setErrorInstruccion(`Máximo ${MAXIMO_INSTRUCCION} caracteres.`)
      return
    }
    setAviso(null)
    setRetocando(true)
    try {
      const respuesta = await retocarImagen(opcionElegida.id, texto)
      setHistorial(respuesta)
      setOpcionElegida((actual) => ({ ...actual, imagen: respuesta.imagenActual }))
      setInstruccion('')
    } catch (error) {
      if (error.status === 400 && error.campos?.instruccion) setErrorInstruccion(error.campos.instruccion)
      else mostrarError(error, 'No se pudo retocar la imagen.')
    } finally {
      setRetocando(false)
    }
  }

  // Deshacer: volver a una versión anterior (o a una posterior, si ya se había vuelto atrás).
  async function volverAVersion(numero) {
    if (!opcionElegida) return
    setAviso(null)
    setRetocando(true)
    try {
      const respuesta = await usarVersion(opcionElegida.id, numero)
      setHistorial(respuesta)
      setOpcionElegida((actual) => ({ ...actual, imagen: respuesta.imagenActual }))
    } catch (error) {
      mostrarError(error, 'No se pudo cambiar de versión.')
    } finally {
      setRetocando(false)
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

  return {
    solicitud,
    errores,
    opciones,
    anuncio,
    opcionElegida,
    aviso,
    faltaPerfil,
    generando,
    eligiendoId,
    regenerando,
    copiado,
    instruccion,
    errorInstruccion,
    historial,
    retocando,
    cambiarCampo,
    cambiarInstruccion,
    retocar,
    volverAVersion,
    generar,
    elegir,
    regenerar,
    copiar,
  }
}
