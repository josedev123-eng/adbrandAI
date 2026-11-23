// ViewModel: identidad de marca del kit (V70). Elegir eslogan, subir el logo propio o elegir uno de 3 de la IA,
// y descargar el logo y el manual de marca en PDF.
import { useState } from 'react'
import { descargarManualMarca, elegirEslogan, guardarLogoImagen, proponerLogos } from '../models/brandkitApi'

export const TAMANO_MAXIMO_LOGO = 2 * 1024 * 1024
const TIPOS_LOGO = ['image/png', 'image/jpeg']

// "alActualizar" recibe el kit que devuelve Spring para que la pantalla se refresque.
export function useIdentidadMarca(kit, alActualizar) {
  const [aviso, setAviso] = useState(null)
  const [guardando, setGuardando] = useState(false)
  const [proponiendo, setProponiendo] = useState(false)
  const [propuestas, setPropuestas] = useState([])
  const [esloganPropio, setEsloganPropio] = useState('')

  async function usarEslogan(texto) {
    const eslogan = texto.trim()
    if (!eslogan) return
    setAviso(null)
    setGuardando(true)
    try {
      alActualizar(await elegirEslogan(kit.id, eslogan))
      setEsloganPropio('')
      setAviso({ tipo: 'exito', texto: 'Eslogan guardado.' })
    } catch (error) {
      setAviso({ tipo: 'error', texto: error?.message || 'No se pudo guardar el eslogan.' })
    } finally {
      setGuardando(false)
    }
  }

  // El logo que la empresa ya tenía: solo PNG o JPG de hasta 2 MB.
  function subirLogo(archivo) {
    setAviso(null)
    if (!archivo) return
    if (!TIPOS_LOGO.includes(archivo.type)) {
      setAviso({ tipo: 'error', texto: 'Sube tu logo en PNG o JPG.' })
      return
    }
    if (archivo.size > TAMANO_MAXIMO_LOGO) {
      setAviso({ tipo: 'error', texto: 'El logo puede pesar hasta 2 MB.' })
      return
    }
    const lector = new FileReader()
    lector.onload = () => guardarLogo(String(lector.result), 'SUBIDO')
    lector.onerror = () => setAviso({ tipo: 'error', texto: 'No pudimos leer el archivo. Prueba con otro.' })
    lector.readAsDataURL(archivo)
  }

  async function pedirPropuestas() {
    setAviso(null)
    setProponiendo(true)
    setPropuestas([])
    try {
      const respuesta = await proponerLogos(kit.id)
      setPropuestas(respuesta.opciones ?? [])
    } catch (error) {
      setAviso({ tipo: 'error', texto: error?.message || 'No se pudieron crear los logos. Inténtalo de nuevo.' })
    } finally {
      setProponiendo(false)
    }
  }

  async function guardarLogo(imagen, origen) {
    setGuardando(true)
    try {
      alActualizar(await guardarLogoImagen(kit.id, imagen, origen))
      setPropuestas([])
      setAviso({ tipo: 'exito', texto: origen === 'IA' ? 'Logo elegido.' : 'Tu logo se guardó.' })
    } catch (error) {
      setAviso({ tipo: 'error', texto: error?.message || 'No se pudo guardar el logo.' })
    } finally {
      setGuardando(false)
    }
  }

  async function descargarManual() {
    setAviso(null)
    try {
      const blob = await descargarManualMarca(kit.id)
      descargar(URL.createObjectURL(blob), `manual-de-marca-${nombreArchivo(kit.nombreNegocio)}.pdf`, true)
    } catch (error) {
      setAviso({ tipo: 'error', texto: error?.message || 'No se pudo descargar el manual.' })
    }
  }

  function descargarLogo() {
    if (!kit.logoImagen) return
    const extension = kit.logoImagen.startsWith('data:image/png') ? 'png' : 'jpg'
    descargar(kit.logoImagen, `logo-${nombreArchivo(kit.nombreNegocio)}.${extension}`, false)
  }

  return {
    aviso,
    guardando,
    proponiendo,
    propuestas,
    esloganPropio,
    setEsloganPropio,
    usarEslogan,
    subirLogo,
    pedirPropuestas,
    elegirPropuesta: (imagen) => guardarLogo(imagen, 'IA'),
    descargarManual,
    descargarLogo,
  }
}

function nombreArchivo(nombre) {
  return (nombre || 'mi-marca')
    .toLowerCase()
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/(^-|-$)/g, '')
}

function descargar(url, nombre, liberar) {
  const enlace = document.createElement('a')
  enlace.href = url
  enlace.download = nombre
  enlace.click()
  if (liberar) setTimeout(() => URL.revokeObjectURL(url), 1000)
}
