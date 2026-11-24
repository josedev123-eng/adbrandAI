// Campo de contraseña seguro, para todos los formularios.
// - No deja copiar, cortar ni arrastrar lo escrito (aunque esté visible).
// - No deja pegar (permitirPegar=false por defecto): la contraseña se escribe, y "Confirmar" obliga a escribirla dos veces.
//   Nota: OWASP y NIST recomiendan permitir pegar para no romper los gestores de contraseñas;
//   si el equipo lo decide, basta con pasar permitirPegar.
// - Tiene el botón Mostrar/Ocultar para revisar lo escrito sin equivocarse.
import { useState } from 'react'
import './InputContrasena.css'

export const AVISO_SIN_PEGAR = 'Por seguridad, escribe la contraseña: no se puede pegar.'

export default function InputContrasena({ permitirPegar = false, onAviso, ...propiedades }) {
  const [visible, setVisible] = useState(false)
  const [aviso, setAviso] = useState(null)

  function bloquear(evento, mensaje) {
    evento.preventDefault()
    if (mensaje) {
      setAviso(mensaje)
      onAviso?.(mensaje)
    }
  }

  return (
    <span className="contrasena">
      <span className="contrasena-fila">
        <input
          {...propiedades}
          type={visible ? 'text' : 'password'}
          spellCheck={false}
          autoCapitalize="off"
          autoCorrect="off"
          onCopy={(e) => bloquear(e)}
          onCut={(e) => bloquear(e)}
          onDragStart={(e) => bloquear(e)}
          onDrop={(e) => bloquear(e, permitirPegar ? null : AVISO_SIN_PEGAR)}
          onPaste={(e) => (permitirPegar ? undefined : bloquear(e, AVISO_SIN_PEGAR))}
          onChange={(e) => {
            setAviso(null)
            propiedades.onChange?.(e)
          }}
        />
        <button
          type="button"
          className="contrasena-ver"
          onClick={() => setVisible((actual) => !actual)}
          aria-label={visible ? 'Ocultar contraseña' : 'Mostrar contraseña'}
          aria-pressed={visible}
          title={visible ? 'Ocultar contraseña' : 'Mostrar contraseña'}
        >
          {visible ? 'Ocultar' : 'Mostrar'}
        </button>
      </span>
      {aviso && (
        <small className="contrasena-aviso" role="status">
          {aviso}
        </small>
      )}
    </span>
  )
}
