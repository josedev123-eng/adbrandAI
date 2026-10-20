// View: solo dibuja los datos de la empresa. La lógica está en useMiCuenta.
import { useMiCuenta } from '../viewmodels/useMiCuenta'
import './MiCuentaView.css'

function Campo({ etiqueta, error, ayuda, children }) {
  return (
    <label className={error ? 'campo campo-con-error' : 'campo'}>
      <span>{etiqueta}</span>
      {children}
      {error ? <small className="campo-error">{error}</small> : ayuda && <small className="campo-ayuda">{ayuda}</small>}
    </label>
  )
}

export default function MiCuentaView() {
  const { datos, cuenta, errores, mensaje, cargando, guardando, cambiarCampo, guardar } = useMiCuenta()

  function alEnviar(evento) {
    evento.preventDefault()
    guardar()
  }

  if (cargando) {
    return <main className="pagina"><p className="cargando">Cargando tu cuenta...</p></main>
  }

  return (
    <main className="pagina">
      <header className="pagina-cabecera">
        <h1>Mi cuenta</h1>
        <p>Los datos de tu empresa. Solo tú puedes verlos y cambiarlos.</p>
      </header>

      {mensaje && <div className={`aviso aviso-${mensaje.tipo}`}>{mensaje.texto}</div>}

      {cuenta && (
        <section className="tarjeta cuenta-fija" aria-label="Tu cuenta">
          <div>
            <span>Empresa</span>
            <b>{cuenta.nombreEmpresa}</b>
          </div>
          <div>
            <span>Correo</span>
            <b>{cuenta.correo}</b>
          </div>
        </section>
      )}

      <form className="tarjeta formulario" onSubmit={alEnviar} noValidate>
        <div className="fila">
          <Campo etiqueta="RUC" error={errores.ruc} ayuda="11 dígitos, empieza en 10 o 20.">
            <input
              value={datos.ruc}
              inputMode="numeric"
              maxLength={11}
              placeholder="20601234567"
              onChange={(e) => cambiarCampo('ruc', e.target.value)}
            />
          </Campo>
          <Campo etiqueta="Teléfono (opcional)" error={errores.telefono} ayuda="Celular de 9 dígitos o fijo de 7.">
            <input
              value={datos.telefono}
              inputMode="tel"
              maxLength={9}
              placeholder="987654321"
              onChange={(e) => cambiarCampo('telefono', e.target.value)}
            />
          </Campo>
        </div>

        <Campo etiqueta="Razón social" error={errores.razonSocial}>
          <input
            value={datos.razonSocial}
            placeholder="Panadería Doña Rosa S.A.C."
            onChange={(e) => cambiarCampo('razonSocial', e.target.value)}
          />
        </Campo>

        <Campo etiqueta="Dirección" error={errores.direccion}>
          <input
            value={datos.direccion}
            placeholder="Av. Los Próceres 123, Surco, Lima"
            onChange={(e) => cambiarCampo('direccion', e.target.value)}
          />
        </Campo>

        <button className="boton-primario" type="submit" disabled={guardando}>
          {guardando ? 'Guardando...' : 'Guardar datos'}
        </button>
      </form>
    </main>
  )
}
