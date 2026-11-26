// View: modal "¿Confirmas el pago?" (antes de cobrar): plan, precio, medio de pago y renovación mensual. Escape o "Cancelar" lo cierran sin cobrar.
import { useEffect, useRef } from 'react'
import './ModalConfirmarPago.css'

const SOLES = new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' })

export default function ModalConfirmarPago({ abierto, plan, metodo, alConfirmar, alCancelar }) {
  const confirmar = useRef(null)

  useEffect(() => {
    if (!abierto) return undefined
    confirmar.current?.focus()
    function alTeclear(evento) {
      if (evento.key === 'Escape') alCancelar()
    }
    window.addEventListener('keydown', alTeclear)
    return () => window.removeEventListener('keydown', alTeclear)
  }, [abierto, alCancelar])

  if (!abierto) return null
  const monto = SOLES.format(plan.precio)

  return (
    <div className="modal-pago-fondo" onClick={alCancelar}>
      <div
        className="modal-pago"
        role="dialog"
        aria-modal="true"
        aria-labelledby="modal-pago-titulo"
        onClick={(evento) => evento.stopPropagation()}
      >
        <h2 id="modal-pago-titulo">¿Confirmas el pago?</h2>
        <dl className="modal-pago-detalle">
          <div>
            <dt>Plan</dt>
            <dd>{plan.nombre}</dd>
          </div>
          <div>
            <dt>Precio</dt>
            <dd className="modal-pago-monto">{monto} / mes</dd>
          </div>
          {metodo && (
            <div>
              <dt>Medio de pago</dt>
              <dd>{metodo}</dd>
            </div>
          )}
          <div>
            <dt>Renovación</dt>
            <dd>Mensual ({monto} cada mes)</dd>
          </div>
        </dl>
        <p className="modal-pago-nota">Solo al confirmar se envía el pago y tu plan se activa de inmediato.</p>
        <div className="modal-pago-acciones">
          <button type="button" className="boton-secundario" onClick={alCancelar}>
            Cancelar
          </button>
          <button ref={confirmar} type="button" className="boton-primario" onClick={alConfirmar}>
            Sí, pagar
          </button>
        </div>
      </div>
    </div>
  )
}
