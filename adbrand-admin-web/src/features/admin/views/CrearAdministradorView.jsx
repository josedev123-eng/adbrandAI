// View: solo dibuja el formulario. La lógica está en useCrearAdministrador.
import InputContrasena from '../../../shared/components/InputContrasena'
import Aviso from '../../../shared/components/Aviso'
import { useCrearAdministrador } from '../viewmodels/useCrearAdministrador'
import './CrearAdministradorView.css'

function Campo({ etiqueta, error, children }) {
  return (
    <label className={error ? 'campo campo-con-error' : 'campo'}>
      <span>{etiqueta}</span>
      {children}
      {error && <small className="campo-error">{error}</small>}
    </label>
  )
}

export default function CrearAdministradorView() {
  const { formulario, roles, errores, mensaje, enviando, cambiarCampo, enviar } = useCrearAdministrador()

  function alEnviar(evento) {
    evento.preventDefault()
    enviar()
  }

  return (
    <main className="pagina">
      <header className="pagina-cabecera">
        <h1>Crear administrador</h1>
        <p>Solo el superadmin puede crear cuentas y asignar roles.</p>
      </header>

      <Aviso mensaje={mensaje} />

      <section className="tarjeta">
        <h2>Registrar administrador</h2>

        <form className="formulario" onSubmit={alEnviar} noValidate>
          <div className="fila">
            <Campo etiqueta="Nombres" error={errores.nombres}>
              <input
                value={formulario.nombres}
                maxLength={100}
                placeholder="Ej.: María"
                onChange={(e) => cambiarCampo('nombres', e.target.value)}
              />
            </Campo>
            <Campo etiqueta="Apellidos" error={errores.apellidos}>
              <input
                value={formulario.apellidos}
                maxLength={100}
                placeholder="Ej.: Torres"
                onChange={(e) => cambiarCampo('apellidos', e.target.value)}
              />
            </Campo>
          </div>

          <Campo etiqueta="Correo" error={errores.correo}>
            <input
              type="email"
              value={formulario.correo}
              maxLength={150}
              placeholder="Ej.: maria.torres@adbrand.pe"
              onChange={(e) => cambiarCampo('correo', e.target.value)}
            />
          </Campo>

          <Campo etiqueta="Contraseña temporal" error={errores.contrasena}>
            <InputContrasena
              value={formulario.contrasena}
              maxLength={128}
              placeholder="Mínimo 8 caracteres"
              onChange={(e) => cambiarCampo('contrasena', e.target.value)}
            />
            {!errores.contrasena && (
              <small className="campo-ayuda">
                Dásela en persona. Al entrar por primera vez, el sistema le pedirá cambiarla por una propia.
              </small>
            )}
          </Campo>

          <Campo etiqueta="Rol" error={errores.rol_id}>
            <select value={formulario.rol_id} onChange={(e) => cambiarCampo('rol_id', e.target.value)}>
              <option value="">Selecciona un rol</option>
              {roles.map((rol) => (
                <option key={rol.id} value={rol.id}>
                  {rol.nombre}
                </option>
              ))}
            </select>
          </Campo>

          <button className="boton-primario" type="submit" disabled={enviando}>
            {enviando ? 'Guardando...' : 'Crear cuenta'}
          </button>
        </form>
      </section>
    </main>
  )
}
