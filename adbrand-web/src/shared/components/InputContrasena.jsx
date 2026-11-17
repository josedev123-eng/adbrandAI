// Campo de contraseña seguro, para todos los formularios.
// - No deja copiar, cortar ni arrastrar lo escrito (aunque esté visible).
// - No deja pegar (permitirPegar=false por defecto): la contraseña se escribe, y "Confirmar" obliga a escribirla dos veces.
//   Nota: OWASP y NIST recomiendan permitir pegar para no romper los gestores de contraseñas;
//   si el equipo lo decide, basta con pasar permitirPegar.
// - Tiene el botón del ojo (Mostrar/Ocultar) para revisar lo escrito sin equivocarse.
import { useState } from 'react'
import './InputContrasena.css'

export const AVISO_SIN_PEGAR = 'Por seguridad, escribe la contraseña: no se puede pegar.'

// Íconos del botón: ojo abierto (mostrar) y ojo tachado (ocultar).
function Ojo() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z" />
      <circle cx="12" cy="12" r="3" />
    </svg>
  )
}

function OjoTachado() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M10.6 5.1A10.4 10.4 0 0 1 12 5c6.5 0 10 7 10 7a17.6 17.6 0 0 1-2.7 3.6" />
      <path d="M6.6 6.6A17.4 17.4 0 0 0 2 12s3.5 7 10 7a9.7 9.7 0 0 0 5.4-1.6" />
      <path d="M9.9 9.9a3 3 0 0 0 4.2 4.2" />
      <path d="M3 3l18 18" />
    </svg>
  )
}

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
          {visible ? <OjoTachado /> : <Ojo />}
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
