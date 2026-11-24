// ViewModel: carga los datos de la empresa, los valida y los guarda (HU-23).
// Primero se muestran solo para leer; con "Editar" se abre el formulario y al guardar vuelve a la vista.
import { useEffect, useState } from 'react'
import { guardarEmpresa, obtenerEmpresa } from '../models/cuentaApi'

const EMPRESA_VACIA = { ruc: '', razonSocial: '', telefono: '', direccion: '', colorAvatar: '' }
const RUC_VALIDO = /^(10|20)\d{9}$/
const TELEFONO_VALIDO = /^(9\d{8}|\d{7})$/

// Las mismas reglas que EmpresaRequest en Spring.
function validar(datos) {
  const errores = {}
  const ruc = datos.ruc.trim()
  const telefono = datos.telefono.trim()
  if (!ruc) errores.ruc = 'Ingresa el RUC.'
  else if (!RUC_VALIDO.test(ruc)) errores.ruc = 'El RUC tiene 11 dígitos y empieza en 10 o 20.'
  if (!datos.razonSocial.trim()) errores.razonSocial = 'Ingresa la razón social.'
  if (telefono && !TELEFONO_VALIDO.test(telefono)) {
    errores.telefono = 'Escribe un celular de 9 dígitos que empiece en 9, o un fijo de 7 dígitos.'
  }
  if (!datos.direccion.trim()) errores.direccion = 'Ingresa la dirección.'
  return errores
}

// La API devuelve null en los datos que todavía no se llenaron.
function aFormulario(empresa) {
  return {
    ruc: empresa.ruc ?? '',
    razonSocial: empresa.razonSocial ?? '',
    telefono: empresa.telefono ?? '',
    direccion: empresa.direccion ?? '',
    // '' = color automático según el nombre de la empresa.
    colorAvatar: empresa.colorAvatar ?? '',
  }
}

export function useMiCuenta(alGuardar) {
  const [datos, setDatos] = useState(EMPRESA_VACIA)
  const [cuenta, setCuenta] = useState(null)
  const [errores, setErrores] = useState({})
  const [mensaje, setMensaje] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [guardando, setGuardando] = useState(false)
  const [editando, setEditando] = useState(false)

  useEffect(() => {
    obtenerEmpresa()
      .then((empresa) => {
        setCuenta(empresa)
        setDatos(aFormulario(empresa))
        // Si la empresa todavía no tiene datos, se abre directo el formulario para llenarlos.
        if (!empresa?.ruc) setEditando(true)
      })
      .catch((error) => setMensaje({ tipo: 'error', texto: error?.message || 'No se pudo conectar con el servidor.' }))
      .finally(() => setCargando(false))
  }, [])

  function empezarEdicion() {
    setMensaje(null)
    setEditando(true)
  }

  // Cancelar descarta lo escrito y vuelve a los datos guardados.
  function cancelarEdicion() {
    if (cuenta) setDatos(aFormulario(cuenta))
    setErrores({})
    setMensaje(null)
    setEditando(false)
  }

  function cambiarCampo(campo, valor) {
    setDatos((actual) => ({ ...actual, [campo]: valor }))
    setErrores((actuales) => ({ ...actuales, [campo]: undefined }))
  }

  async function guardar() {
    setMensaje(null)
    const erroresLocales = validar(datos)
    setErrores(erroresLocales)
    if (Object.keys(erroresLocales).length > 0) return

    setGuardando(true)
    try {
      const empresa = await guardarEmpresa(datos)
      setCuenta(empresa)
      setDatos(aFormulario(empresa))
      setMensaje({ tipo: 'exito', texto: 'Datos guardados' })
      alGuardar?.(empresa)
      setEditando(false)
    } catch (error) {
      if (error.codigo === 'RUC_REPETIDO') {
        setErrores({ ruc: error.message })
      } else if (error.status === 400) {
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
    datos,
    cuenta,
    errores,
    mensaje,
    cargando,
    guardando,
    editando,
    empezarEdicion,
    cancelarEdicion,
    cambiarCampo,
    guardar,
  }
}
