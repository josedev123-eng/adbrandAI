// ViewModel: estado del formulario, validaciones y envío (HU-11).
import { useState } from 'react'
import { generarKitMarca, ESTILOS_VISUALES } from '../models/brandkitApi'

const FORMULARIO_VACIO = {
  nombreNegocio: '',
  sector: '',
  paletaSugerida: '',
  valoresEslogan: '',
  estiloVisual: '',
}

function validar(formulario) {
  const errores = {}
  if (!formulario.nombreNegocio.trim()) errores.nombreNegocio = 'Ingresa el nombre del negocio.'
  if (!formulario.sector.trim()) errores.sector = 'Ingresa el sector/rubro.'
  if (!formulario.estiloVisual) errores.estiloVisual = 'Selecciona un estilo visual.'
  return errores
}

export function useSolicitarKitMarca() {
  const [formulario, setFormulario] = useState(FORMULARIO_VACIO)
  const [errores, setErrores] = useState({})
  const [kit, setKit] = useState(null)
  const [mostrarResultado, setMostrarResultado] = useState(false)
  const [aviso, setAviso] = useState(null)
  const [generando, setGenerando] = useState(false)

  function cambiarCampo(campo, valor) {
    setFormulario((actual) => ({ ...actual, [campo]: valor }))
    setErrores((actuales) => ({ ...actuales, [campo]: undefined }))
  }

  async function generar() {
    setAviso(null)
    const erroresLocales = validar(formulario)
    setErrores(erroresLocales)
    if (Object.keys(erroresLocales).length > 0) return

    setGenerando(true)
    try {
      const creado = await generarKitMarca({ ...formulario })
      setKit(creado)
      setMostrarResultado(true)
      setAviso({ tipo: 'exito', texto: '¡Kit de Marca generado correctamente!' })
    } catch (error) {
      if (error.status === 404 && error.codigo === 'PERFIL_NO_ENCONTRADO') {
        setAviso({ tipo: 'error', texto: 'Primero completa tu perfil de negocio.' })
      } else if (error.codigo === 'IA_NO_DISPONIBLE') {
        setAviso({ tipo: 'error', texto: error.message })
      } else if (error.status === 400 && error.detalles) {
        const delServidor = {}
        for (const [campo, mensajes] of Object.entries(error.detalles)) {
          delServidor[campo] = Array.isArray(mensajes) ? mensajes[0] : String(mensajes)
        }
        setErrores(delServidor)
        setAviso({ tipo: 'error', texto: 'Revisa los campos marcados.' })
      } else {
        setAviso({ tipo: 'error', texto: 'No se pudo generar el Kit de Marca.' })
      }
    } finally {
      setGenerando(false)
    }
  }

  function pedirOtraVersion() {
    return generar()
  }

  function aceptarKit() {
    setKit(null)
    setMostrarResultado(false)
    setAviso({ tipo: 'exito', texto: 'Kit de Marca aceptado. Ya puedes usarlo en tu negocio.' })
  }

  function volverAlFormulario() {
    setMostrarResultado(false)
    setAviso(null)
  }

  function reiniciar() {
    setFormulario(FORMULARIO_VACIO)
    setErrores({})
    setKit(null)
    setMostrarResultado(false)
    setAviso(null)
  }

  return {
    formulario,
    errores,
    kit,
    mostrarResultado,
    aviso,
    generando,
    estilos: ESTILOS_VISUALES,
    cambiarCampo,
    generar,
    pedirOtraVersion,
    aceptarKit,
    volverAlFormulario,
    reiniciar,
  }
}