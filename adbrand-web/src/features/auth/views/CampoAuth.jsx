// View: un campo de formulario con su etiqueta y su error, para las pantallas de cuenta (HU-21).
// "ayuda": las reglas del campo, visibles antes de que haya un error. "valido": el campo ya cumple (✓).
// "reglas": lista [{ texto, cumple }] que se marca mientras se escribe (por ejemplo, la contraseña).
// La ayuda, las reglas y el error van fuera de la <label> para que el nombre del campo siga siendo solo la etiqueta.
export default function CampoAuth({ etiqueta, error, ayuda, valido = false, reglas, children }) {
  return (
    <div className={error ? 'campo campo-con-error' : valido ? 'campo campo-valido' : 'campo'}>
      <label className="campo-control">
        <span>
          {etiqueta}
          {valido && !error && (
            <b className="campo-check" aria-hidden="true">
              {' '}
              ✓
            </b>
          )}
        </span>
        {children}
      </label>
      {reglas && (
        <ul className="campo-reglas" aria-label={`Reglas de ${etiqueta.toLowerCase()}`}>
          {reglas.map((regla) => (
            <li key={regla.texto} className={regla.cumple ? 'regla-cumple' : undefined}>
              {regla.cumple ? '✓' : '○'} {regla.texto}
            </li>
          ))}
        </ul>
      )}
      {error ? <small className="campo-error">{error}</small> : ayuda && <small className="campo-ayuda">{ayuda}</small>}
    </div>
  )
}
