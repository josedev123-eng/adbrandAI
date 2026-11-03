// View: formulario de inicio de sesión (HU-22). La lógica está en useLogin.
import { useLogin } from '../viewmodels/useLogin'
import CampoAuth from './CampoAuth'
import './Auth.css'

export default function LoginView({ aviso, irARegistro, irARecuperar }) {
  const { formulario, errores, mensaje, enviando, cambiarCampo, enviar } = useLogin(aviso)

  function alEnviar(evento) {
    evento.preventDefault()
    enviar()
  }

  return (
    <main className="pagina pagina-auth">
      <p className="auth-marca">
        AdBrand<b>.AI</b>
      </p>
      <header className="pagina-cabecera">
        <h1>Inicia sesión</h1>
        <p>Entra con el correo y la contraseña de tu empresa.</p>
      </header>

      {mensaje && <div className={`aviso aviso-${mensaje.tipo}`}>{mensaje.texto}</div>}

      <form className="tarjeta formulario" onSubmit={alEnviar} noValidate>
        <CampoAuth etiqueta="Correo" error={errores.correo}>
          <input
            type="email"
            value={formulario.correo}
            placeholder="rosa@panaderia.pe"
            autoComplete="email"
            onChange={(e) => cambiarCampo('correo', e.target.value)}
          />
        </CampoAuth>

        <CampoAuth etiqueta="Contraseña" error={errores.contrasena}>
          <input
            type="password"
            value={formulario.contrasena}
            autoComplete="current-password"
            onChange={(e) => cambiarCampo('contrasena', e.target.value)}
          />
        </CampoAuth>

        <button className="boton-primario" type="submit" disabled={enviando}>
          {enviando ? 'Entrando...' : 'Iniciar sesión'}
        </button>
      </form>

      <div className="auth-enlaces">
        {irARecuperar && (
          <button type="button" className="boton-enlace" onClick={irARecuperar}>
            ¿Olvidaste tu contraseña?
          </button>
        )}
        {irARegistro && (
          <button type="button" className="boton-enlace" onClick={irARegistro}>
            Crear una cuenta
          </button>
        )}
      </div>
    </main>
  )
}
