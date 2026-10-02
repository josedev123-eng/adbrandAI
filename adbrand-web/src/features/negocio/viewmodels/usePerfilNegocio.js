// ViewModel: carga el perfil guardado, valida los campos y lo guarda (HU-09).
import { useEffect, useState } from 'react'
import { guardarPerfil, obtenerPerfil } from '../models/perfilNegocioApi'

const PERFIL_VACIO = { nombreComercial: '', rubro: '', publicoObjetivo: '', tono: '', descripcion: '' }
export const MAXIMO_DESCRIPCION = 500

function validar(perfil) {
  const errores = {}
  if (!perfil.nombreComercial.trim()) errores.nombreComercial = 'Ingresa el nombre comercial.'
  if (!perfil.rubro) errores.rubro = 'Selecciona el rubro.'
  if (!perfil.publicoObjetivo.trim()) errores.publicoObjetivo = 'Describe a tu público objetivo.'
  if (!perfil.tono) errores.tono = 'Selecciona el tono.'
  if (perfil.descripcion.length > MAXIMO_DESCRIPCION) errores.descripcion = `Máximo ${MAXIMO_DESCRIPCION} caracteres.`
  return errores
}

export function usePerfilNegocio() {
  const [perfil, setPerfil] = useState(PERFIL_VACIO)
  const [errores, setErrores] = useState({})
  const [mensaje, setMensaje] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [guardando, setGuardando] = useState(false)
  const [existe, setExiste] = useState(false)

  // Criterio 2: al entrar, si ya hay un perfil guardado se carga en el formulario.
  useEffect(() => {
    obtenerPerfil()
      .then((guardado) => {
        setPerfil({ ...guardado, descripcion: guardado.descripcion ?? '' })
        setExiste(true)
      })
      .catch((error) => {
        if (error.status !== 404) {
          setMensaje({ tipo: 'error', texto: 'No se pudo conectar con el servidor.' })
        }
      })
      .finally(() => setCargando(false))
  }, [])

  function cambiarCampo(campo, valor) {
    setPerfil((actual) => ({ ...actual, [campo]: valor }))
    setErrores((actuales) => ({ ...actuales, [campo]: undefined }))
  }

  // Criterio 1: si falta rubro, público objetivo o tono, no se envía.
  async function guardar() {
    setMensaje(null)
    const erroresLocales = validar(perfil)
    setErrores(erroresLocales)
    if (Object.keys(erroresLocales).length > 0) return

    setGuardando(true)
    try {
      const guardado = await guardarPerfil(perfil)
      setPerfil({ ...guardado, descripcion: guardado.descripcion ?? '' })
      setExiste(true)
      setMensaje({ tipo: 'exito', texto: 'Perfil guardado. La IA usará estos datos en tus anuncios.' })
    } catch (error) {
      if (error.status === 400) {
        setErrores(error.campos)
        setMensaje({ tipo: 'error', texto: error.message })
      } else {
        setMensaje({ tipo: 'error', texto: 'No se pudo conectar con el servidor.' })
      }
    } finally {
      setGuardando(false)
    }
  }

  return { perfil, errores, mensaje, cargando, guardando, existe, cambiarCampo, guardar }
}