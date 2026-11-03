// ViewModel: editor interactivo del logo (HU-29). La vista previa usa el mismo diseño que se va a guardar.
import { useState } from 'react'
import { guardarLogoKitMarca, leerPaleta, TIPOGRAFIAS_LOGO } from '../models/brandkitApi'

const COLOR_VALIDO = /^#[0-9A-Fa-f]{6}$/

// Las iniciales del negocio: "Panadería Doña Rosa" → "PDR".
function iniciales(nombre) {
  return (nombre ?? '')
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 3)
    .map((palabra) => palabra[0].toUpperCase())
    .join('')
}

// Si el kit todavía no tiene logo diseñado, se parte de sus iniciales, su primer color y su tipografía.
export function disenoInicial(kit) {
  if (kit.logoDiseno) return { ...kit.logoDiseno }
  const primerColor = leerPaleta(kit.paletaColores)[0]?.hex
  return {
    texto: iniciales(kit.nombreNegocio) || 'AB',
    forma: 'CIRCULO',
    colorFondo: COLOR_VALIDO.test(primerColor ?? '') ? primerColor.toUpperCase() : '#4F46E5',
    colorTexto: '#FFFFFF',
    tipografia: TIPOGRAFIAS_LOGO.includes(kit.tipografiaPrimaria) ? kit.tipografiaPrimaria : TIPOGRAFIAS_LOGO[0],
  }
}

// Las mismas reglas que LogoDiseno en Spring.
function validar(diseno) {
  const errores = {}
  const texto = diseno.texto.trim()
  if (!texto) errores.texto = 'Escribe el texto o las iniciales del logo.'
  else if (texto.length > 20) errores.texto = 'Máximo 20 caracteres.'
  if (!COLOR_VALIDO.test(diseno.colorFondo)) errores.colorFondo = 'Escribe el color como #RRGGBB.'
  if (!COLOR_VALIDO.test(diseno.colorTexto)) errores.colorTexto = 'Escribe el color como #RRGGBB.'
  return errores
}

export function useEditarLogo(kit, alGuardar) {
  const [diseno, setDiseno] = useState(() => disenoInicial(kit))
  const [errores, setErrores] = useState({})
  const [mensaje, setMensaje] = useState(null)
  const [guardando, setGuardando] = useState(false)

  function cambiar(campo, valor) {
    setDiseno((actual) => ({ ...actual, [campo]: valor }))
    setErrores((actuales) => ({ ...actuales, [campo]: undefined }))
  }

  // HU-29, criterio 1: la vista previa solo dibuja colores válidos; mientras se escribe, mantiene el anterior.
  const vistaPrevia = {
    ...diseno,
    texto: diseno.texto.trim() || ' ',
    colorFondo: COLOR_VALIDO.test(diseno.colorFondo) ? diseno.colorFondo : '#CCCCCC',
    colorTexto: COLOR_VALIDO.test(diseno.colorTexto) ? diseno.colorTexto : '#000000',
  }

  async function guardar() {
    setMensaje(null)
    const erroresLocales = validar(diseno)
    setErrores(erroresLocales)
    if (Object.keys(erroresLocales).length > 0) return

    setGuardando(true)
    try {
      const actualizado = await guardarLogoKitMarca(kit.id, { ...diseno, texto: diseno.texto.trim() })
      alGuardar?.(actualizado)
    } catch (error) {
      if (error.status === 400 && error.campos) {
        setErrores(error.campos)
      } else if (error.status === 404) {
        setMensaje({ tipo: 'error', texto: 'Este kit ya no existe.' })
      } else {
        setMensaje({ tipo: 'error', texto: 'No se pudo guardar el logo. Revisa tu conexión.' })
      }
    } finally {
      setGuardando(false)
    }
  }

  return { diseno, vistaPrevia, errores, mensaje, guardando, cambiar, guardar }
}
