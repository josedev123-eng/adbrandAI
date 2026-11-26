// ViewModel: modal "Confirma tu pago". Antes de cobrar, cada forma de pago llama a pedir({ metodo }) y espera
// la respuesta del dueño: true si confirma, false si cancela. Así nadie paga con un clic por error.
import { useCallback, useRef, useState } from 'react'

export function useConfirmarPago() {
  const [detalle, setDetalle] = useState(null)
  const resolver = useRef(null)

  const pedir = useCallback(
    (datos) =>
      new Promise((resolve) => {
        resolver.current = resolve
        setDetalle(datos)
      }),
    [],
  )

  function responder(confirmado) {
    resolver.current?.(confirmado)
    resolver.current = null
    setDetalle(null)
  }

  return {
    pedir,
    modal: {
      abierto: detalle !== null,
      metodo: detalle?.metodo,
      alConfirmar: () => responder(true),
      alCancelar: () => responder(false),
    },
  }
}
