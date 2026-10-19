// ViewModel: editar a mano colores, tipografías y logotipo del kit de marca (HU-32).
import { useEffect, useState } from 'react'
import { editarKitMarca, leerPaleta, listarVersionesKitMarca } from '../models/brandkitApi'

export const MAXIMO_COLORES = 8
const COLOR_VALIDO = /^#[0-9A-Fa-f]{6}$/

function formularioDesde(kit) {
  const colores = leerPaleta(kit.paletaColores)
  return {
    colores: colores.length > 0 ? colores : [{ hex: '#4F46E5', nombre: '' }],
    tipografiaPrimaria: kit.tipografiaPrimaria ?? '',
    tipografiaSecundaria: kit.tipografiaSecundaria ?? '',
    logoConcepto: kit.logoConcepto ?? '',
  }
}

// Las mismas reglas que EditarKitMarcaRequest en Spring. Los errores de color usan la clave "colores[i].hex".
function validar(formulario) {
  const errores = {}
  formulario.colores.forEach((color, i) => {
    if (!COLOR_VALIDO.test(color.hex.trim())) errores[`colores[${i}].hex`] = 'Escribe el color como #RRGGBB.'
  })
  if (!formulario.tipografiaPrimaria.trim()) errores.tipografiaPrimaria = 'Ingresa la tipografía principal.'
  if (!formulario.logoConcepto.trim()) errores.logoConcepto = 'Describe el concepto del logotipo.'
  return errores
}

// "alGuardar" recibe el kit actualizado que devuelve Spring.
export function useEditarKitMarca(kit, alGuardar) {
  const [formulario, setFormulario] = useState(() => formularioDesde(kit))
  const [errores, setErrores] = useState({})
  const [mensaje, setMensaje] = useState(null)
  const [guardando, setGuardando] = useState(false)
  const [versiones, setVersiones] = useState([])

  useEffect(() => {
    listarVersionesKitMarca(kit.id)
      .then(setVersiones)
      .catch(() => setVersiones([]))
  }, [kit.id])

  function cambiarCampo(campo, valor) {
    setFormulario((actual) => ({ ...actual, [campo]: valor }))
    setErrores((actuales) => ({ ...actuales, [campo]: undefined }))
  }

  function cambiarColor(indice, campo, valor) {
    setFormulario((actual) => ({
      ...actual,
      colores: actual.colores.map((color, i) => (i === indice ? { ...color, [campo]: valor } : color)),
    }))
    setErrores((actuales) => ({ ...actuales, [`colores[${indice}].hex`]: undefined }))
  }

  function agregarColor() {
    if (formulario.colores.length >= MAXIMO_COLORES) return
    setFormulario((actual) => ({ ...actual, colores: [...actual.colores, { hex: '#FFFFFF', nombre: '' }] }))
  }

  // Siempre queda al menos un color. Los errores de color se limpian porque cambian los índices.
  function quitarColor(indice) {
    if (formulario.colores.length <= 1) return
    setFormulario((actual) => ({ ...actual, colores: actual.colores.filter((_, i) => i !== indice) }))
    setErrores((actuales) =>
      Object.fromEntries(Object.entries(actuales).filter(([clave]) => !clave.startsWith('colores['))),
    )
  }

  async function guardar() {
    setMensaje(null)
    const erroresLocales = validar(formulario)
    setErrores(erroresLocales)
    if (Object.keys(erroresLocales).length > 0) return

    setGuardando(true)
    try {
      const actualizado = await editarKitMarca(kit.id, {
        colores: formulario.colores.map((color) => ({ hex: color.hex.trim(), nombre: color.nombre.trim() })),
        tipografiaPrimaria: formulario.tipografiaPrimaria,
        tipografiaSecundaria: formulario.tipografiaSecundaria,
        logoConcepto: formulario.logoConcepto,
      })
      alGuardar(actualizado)
    } catch (error) {
      if (error.status === 400) {
        setErrores(error.campos)
        setMensaje({ tipo: 'error', texto: error.message })
      } else if (error.status === 404) {
        setMensaje({ tipo: 'error', texto: 'Este kit ya no está disponible.' })
      } else {
        setMensaje({ tipo: 'error', texto: 'No se pudo conectar con el servidor.' })
      }
    } finally {
      setGuardando(false)
    }
  }

  return {
    formulario,
    errores,
    mensaje,
    guardando,
    versiones,
    cambiarCampo,
    cambiarColor,
    agregarColor,
    quitarColor,
    guardar,
  }
}
