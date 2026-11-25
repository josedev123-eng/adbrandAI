// View: "Editar mi logo con IA". Una instrucción corta, la versión nueva al lado de la actual, y elegir si se usa.
import { MAXIMO_INSTRUCCION, useRetocarLogo } from '../viewmodels/useRetocarLogo'

export default function RetocarLogo({ kit, alUsar, guardando }) {
  const { instruccion, cambiarInstruccion, retocando, version, restantes, error, retocar, limpiar } = useRetocarLogo(kit.id)

  return (
    <div className="retocar-logo">
      <form
        className="retocar-logo-form"
        noValidate
        onSubmit={(evento) => {
          evento.preventDefault()
          retocar()
        }}
      >
        <label className={error ? 'campo campo-con-error' : 'campo'}>
          <span>¿Qué quieres mejorar de tu logo?</span>
          <div className="retocar-logo-fila">
            <input
              type="text"
              value={instruccion}
              maxLength={MAXIMO_INSTRUCCION}
              placeholder="Ej.: pon la taza más grande y el fondo blanco"
              onChange={(e) => cambiarInstruccion(e.target.value)}
              disabled={retocando}
            />
            <button type="submit" className="boton-ia" disabled={retocando || guardando}>
              {retocando ? 'Editando…' : '✦ Editar con IA'}
            </button>
          </div>
          {error && <small className="campo-error">{error}</small>}
          {!error && restantes != null && <small className="campo-ayuda">Te quedan {restantes} retoques este mes.</small>}
        </label>
      </form>

      {version && (
        <div className="retocar-logo-comparar" aria-label="Versión nueva del logo">
          <figure>
            <img src={kit.logoImagen} alt="Logo actual" />
            <figcaption>Actual</figcaption>
          </figure>
          <figure>
            <img src={version} alt="Versión nueva del logo" />
            <figcaption>Nueva</figcaption>
          </figure>
          <div className="retocar-logo-acciones">
            <button
              type="button"
              className="boton-primario"
              disabled={guardando}
              onClick={async () => {
                await alUsar(version)
                limpiar()
              }}
            >
              Usar esta versión
            </button>
            <button type="button" className="boton-secundario" onClick={limpiar} disabled={guardando}>
              Descartar
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
