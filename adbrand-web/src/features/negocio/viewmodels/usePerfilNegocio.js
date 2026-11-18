// ViewModel: carga el perfil guardado, valida los campos y lo guarda (HU-09).
// Con perfil guardado se muestra el resumen; "Editar" abre el formulario y al guardar vuelve al resumen.
import { useEffect, useState } from 'react'
import { useSesion } from '../../../app/context/useSesion'
import { guardarPerfil, obtenerPerfil } from '../models/perfilNegocioApi'
import { useCambiosSinGuardar } from '../../../shared/hooks/useCambiosSinGuardar'

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
  // El nombre que la empresa escribió al registrarse: se propone como nombre comercial para no pedirlo dos veces.
  const { usuario } = useSesion()
  const perfilInicial = { ...PERFIL_VACIO, nombreComercial: usuario?.nombreEmpresa ?? '' }
  const [perfil, setPerfil] = useState(PERFIL_VACIO)
  const [errores, setErrores] = useState({})
  const [mensaje, setMensaje] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [guardando, setGuardando] = useState(false)
  const [existe, setExiste] = useState(false)
  // Copia de lo que está guardado en la base, para el resumen. No cambia mientras se edita el formulario.
  const [guardado, setGuardado] = useState(null)
  // Sin perfil guardado se empieza directo en el formulario.
  const [editando, setEditando] = useState(false)
  // Aviso de cambios sin guardar: lo escrito en el formulario frente a lo guardado.
  useCambiosSinGuardar(perfil, {
    original: guardado ? { ...guardado, descripcion: guardado.descripcion ?? '' } : perfilInicial,
    activo: editando && !cargando,
  })

  // Criterio 2: al entrar, si ya hay un perfil guardado se carga en el formulario.
  useEffect(() => {
    obtenerPerfil()
      .then((perfilGuardado) => {
        setPerfil({ ...perfilGuardado, descripcion: perfilGuardado.descripcion ?? '' })
        setGuardado(perfilGuardado)
        setExiste(true)
      })
      .catch((error) => {
        if (error.status === 404) {
          setPerfil(perfilInicial)
          setEditando(true)
        } else {
          setMensaje({ tipo: 'error', texto: error?.message || 'No se pudo conectar con el servidor.' })
        }
      })
      .finally(() => setCargando(false))
    // Solo al abrir la pantalla: el nombre del registro no cambia mientras se está en ella.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  function empezarEdicion() {
    setMensaje(null)
    setEditando(true)
  }

  // Cancelar descarta lo escrito y vuelve al perfil guardado.
  function cancelarEdicion() {
    if (guardado) setPerfil({ ...guardado, descripcion: guardado.descripcion ?? '' })
    setErrores({})
    setMensaje(null)
    setEditando(false)
  }

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
      const perfilGuardado = await guardarPerfil(perfil)
      setPerfil({ ...perfilGuardado, descripcion: perfilGuardado.descripcion ?? '' })
      setGuardado(perfilGuardado)
      setExiste(true)
      setMensaje({ tipo: 'exito', texto: 'Perfil guardado. La IA usará estos datos en tus anuncios.' })
      setEditando(false)
    } catch (error) {
      if (error.status === 400) {
        setErrores(error.campos)
        setMensaje({ tipo: 'error', texto: error.message })
      } else {
        setMensaje({ tipo: 'error', texto: error?.message || 'No se pudo conectar con el servidor.' })
      }
    } finally {
      setGuardando(false)
    }
  }

  return {
    perfil,
    guardado,
    errores,
    mensaje,
    cargando,
    guardando,
    existe,
    editando,
    empezarEdicion,
    cancelarEdicion,
    cambiarCampo,
    guardar,
  }
}