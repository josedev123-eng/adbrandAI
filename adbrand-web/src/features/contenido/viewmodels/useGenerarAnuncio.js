// ViewModel: "Generar → Elegir". Pide 4 opciones de anuncio (texto + imagen), deja elegir una
// y la guarda como anuncio. Después se puede copiar o regenerar (HU-10, HU-12).
import { useEffect, useRef, useState } from 'react'
import { obtenerPerfil } from '../../negocio/models/perfilNegocioApi'
import { leerOcasionSugerida, leerOfertaSugerida, olvidarOfertaSugerida } from '../models/ofertaSugerida'
import {
  descargarImagenFinal,
  elegirOpcion,
  generarOpciones,
  regenerarAnuncio,
  retocarImagen,
  usarVersion,
} from '../models/anunciosApi'
import { comoDataUrl } from './useImagenConMarca'
import { generacionActual, iniciarGeneracion, olvidarGeneracion } from '../models/generacionEnCurso'

export const MAXIMO_OFERTA = 300
export const MAXIMO_INSTRUCCION = 300

function validar(solicitud) {
  const errores = {}
  if (!solicitud.oferta.trim()) errores.oferta = 'Escribe la oferta que quieres anunciar.'
  else if (solicitud.oferta.length > MAXIMO_OFERTA) errores.oferta = `Máximo ${MAXIMO_OFERTA} caracteres.`
  if (!solicitud.redSocial) errores.redSocial = 'Selecciona la red social.'
  return errores
}

// Foto del producto (opcional): JPG o PNG de hasta 5 MB.
const TIPOS_FOTO = ['image/jpeg', 'image/png']
const TAMANO_MAXIMO_FOTO = 5 * 1024 * 1024

export function useGenerarAnuncio() {
  // HU 63: si viene de "Tu mes de contenido", la idea de la fecha llega ya escrita y con su festividad ("ocasion"),
  // para que el texto y la imagen se ambienten en ella. Desde "Crear anuncio" directo no hay ocasión.
  // Si el dueño salió mientras se creaban las opciones, al volver se retoma ese pedido (sigue o ya terminó).
  const [alAbrir] = useState(generacionActual)
  const [solicitud, setSolicitud] = useState(
    () =>
      alAbrir?.solicitud ?? {
        oferta: leerOfertaSugerida().slice(0, MAXIMO_OFERTA),
        redSocial: 'INSTAGRAM',
        ocasion: leerOcasionSugerida() || undefined,
      },
  )
  useEffect(() => olvidarOfertaSugerida(), [])
  const [errores, setErrores] = useState({})
  const [opciones, setOpciones] = useState(() => (alAbrir?.estado === 'lista' ? (alAbrir.respuesta?.opciones ?? []) : []))
  const [anuncio, setAnuncio] = useState(null)
  const [opcionElegida, setOpcionElegida] = useState(null)
  const [aviso, setAviso] = useState(null)
  const [faltaPerfil, setFaltaPerfil] = useState(false)
  // Ya usó los tokens de IA de su plan este mes: se muestra el aviso con el botón para ver los planes.
  const [sinCupo, setSinCupo] = useState(null)
  const [generando, setGenerando] = useState(alAbrir?.estado === 'creando')
  const montado = useRef(true)
  const [eligiendoId, setEligiendoId] = useState(null)
  const [regenerando, setRegenerando] = useState(false)
  const [copiado, setCopiado] = useState(false)
  // Retoques de la imagen elegida y su historial de versiones (v1 = original).
  const [instruccion, setInstruccion] = useState('')
  const [errorInstruccion, setErrorInstruccion] = useState(null)
  const [historial, setHistorial] = useState(null)
  const [retocando, setRetocando] = useState(false)

  // Criterio 1 (HU 9): si todavía no hay perfil del negocio, se avisa desde que entra, no recién al generar.
  useEffect(() => {
    let activo = true
    obtenerPerfil()
      .then(() => activo && setFaltaPerfil(false))
      .catch((error) => activo && error?.status === 404 && setFaltaPerfil(true))
    return () => {
      activo = false
    }
  }, [])

  // Espera el pedido de las 4 opciones. Si el dueño salió de la pantalla, el resultado queda guardado
  // en generacionEnCurso para mostrarlo cuando vuelva.
  async function esperar(promesa) {
    try {
      const respuesta = await promesa
      if (!montado.current) return
      setOpciones(respuesta.opciones ?? [])
      olvidarGeneracion()
    } catch (error) {
      if (!montado.current) return
      mostrarError(error, error?.message || 'No se pudo conectar con el servidor.')
      olvidarGeneracion()
    } finally {
      if (montado.current) setGenerando(false)
    }
  }

  useEffect(() => {
    montado.current = true
    if (alAbrir?.estado === 'lista') olvidarGeneracion()
    // esperar() solo cambia el estado cuando la promesa termina (después del await), no de inmediato.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    else if (alAbrir) esperar(alAbrir.promesa)
    return () => {
      montado.current = false
    }
    // Solo al abrir la pantalla: alAbrir no cambia.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // El dueño puede quitar la festividad y crear un anuncio normal.
  function quitarOcasion() {
    setSolicitud((actual) => ({ ...actual, ocasion: undefined }))
  }

  // La foto de lo que vende: las 4 opciones la usan en vez de una imagen inventada por la IA.
  function ponerFoto(archivo) {
    if (!archivo) return
    if (!TIPOS_FOTO.includes(archivo.type)) {
      setErrores((actuales) => ({ ...actuales, foto: 'Sube tu foto en JPG o PNG.' }))
      return
    }
    if (archivo.size > TAMANO_MAXIMO_FOTO) {
      setErrores((actuales) => ({ ...actuales, foto: 'La foto puede pesar hasta 5 MB.' }))
      return
    }
    const lector = new FileReader()
    lector.onload = () => cambiarCampo('foto', String(lector.result))
    lector.onerror = () => setErrores((actuales) => ({ ...actuales, foto: 'No pudimos leer tu foto. Prueba con otra.' }))
    lector.readAsDataURL(archivo)
  }

  function quitarFoto() {
    cambiarCampo('foto', undefined)
  }

  function cambiarCampo(campo, valor) {
    setSolicitud((actual) => ({ ...actual, [campo]: valor }))
    setErrores((actuales) => ({ ...actuales, [campo]: undefined }))
  }

  function mostrarError(error, mensajePorDefecto) {
    if (error.codigo === 'LIMITE_TOKENS') {
      setSinCupo(error.message)
    } else if (error.codigo === 'PERFIL_NO_ENCONTRADO') {
      // Criterio 1: primero hay que registrar el perfil del negocio (HU 9).
      setFaltaPerfil(true)
    } else if (['IA_NO_DISPONIBLE', 'OPCION_NO_ENCONTRADA', 'OPCION_NO_ELEGIDA', 'VERSION_NO_ENCONTRADA', 'LIMITE_RETOQUES'].includes(error.codigo)) {
      // Criterio 2: si la IA falla (o se llegó al límite de retoques), se muestra un mensaje claro.
      // Va antes del 400 genérico porque OPCION_NO_ELEGIDA y LIMITE_RETOQUES también son 400.
      setAviso(error.message)
    } else if (error.codigo === 'FOTO_INVALIDA') {
      setErrores({ foto: error.message })
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
    setSinCupo(null)
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
    await esperar(iniciarGeneracion(solicitud, generarOpciones))
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
      if (error.codigo === 'LIMITE_TOKENS') {
        setSinCupo(error.message)
      } else if (error.codigo === 'IA_NO_DISPONIBLE') {
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
      actualizarDiseno()
    } catch (error) {
      if (error.status === 400 && error.campos?.instruccion) setErrorInstruccion(error.campos.instruccion)
      else mostrarError(error, 'No se pudo retocar la imagen.')
    } finally {
      setRetocando(false)
    }
  }

  // Ajuste 14: después de retocar la foto, el diseño del anuncio se vuelve a armar con la foto nueva.
  async function actualizarDiseno() {
    if (!anuncio?.id) return
    try {
      const diseno = await descargarImagenFinal(anuncio.id, 'CUADRADO').then(comoDataUrl)
      setOpcionElegida((actual) => (actual ? { ...actual, diseno } : actual))
    } catch {
      // Sin diseño nuevo se sigue viendo la foto: no es un error para el dueño.
      setOpcionElegida((actual) => (actual ? { ...actual, diseno: undefined } : actual))
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
      actualizarDiseno()
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
      // "¡Copiado!" se muestra un momento y el botón vuelve a "Copiar texto".
      setTimeout(() => setCopiado(false), 2500)
    } catch {
      setAviso('No se pudo copiar. Selecciona el texto y cópialo con Ctrl + C.')
    }
  }

  return {
    solicitud,
    quitarOcasion,
    errores,
    opciones,
    anuncio,
    opcionElegida,
    aviso,
    faltaPerfil,
    sinCupo,
    generando,
    eligiendoId,
    regenerando,
    copiado,
    instruccion,
    errorInstruccion,
    historial,
    retocando,
    cambiarCampo,
    ponerFoto,
    quitarFoto,
    cambiarInstruccion,
    retocar,
    volverAVersion,
    generar,
    elegir,
    regenerar,
    copiar,
  }
}
