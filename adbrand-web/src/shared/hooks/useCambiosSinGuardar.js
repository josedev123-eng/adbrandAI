// Aviso de "tienes cambios sin guardar": si el formulario cambió y el usuario intenta irse (menú, Atrás del navegador,
// recargar o cerrar la pestaña), se le pregunta antes de perder lo escrito.
// actual: lo que hay en el formulario; original: lo guardado (por defecto, lo que había al abrir); activo: si se está editando.
import { useEffect, useId, useState } from 'react'
import { marcarCambiosPendientes } from '../navegacion/navegacion'

export function useCambiosSinGuardar(actual, { original, activo = true } = {}) {
  const clave = useId()
  const [inicial] = useState(() => JSON.stringify(actual))
  const referencia = original === undefined ? inicial : JSON.stringify(original)
  const hayCambios = activo && JSON.stringify(actual) !== referencia

  useEffect(() => {
    marcarCambiosPendientes(clave, hayCambios)
    if (!hayCambios) return undefined
    // Recargar o cerrar la pestaña: el navegador muestra su propio aviso.
    const alSalirDelSitio = (evento) => {
      evento.preventDefault()
      evento.returnValue = ''
    }
    window.addEventListener('beforeunload', alSalirDelSitio)
    return () => window.removeEventListener('beforeunload', alSalirDelSitio)
  }, [clave, hayCambios])

  // Al guardar, cancelar o cerrar la pantalla ya no quedan cambios pendientes.
  useEffect(() => () => marcarCambiosPendientes(clave, false), [clave])

  return hayCambios
}
