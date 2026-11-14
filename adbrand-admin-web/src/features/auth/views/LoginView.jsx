// View: pantalla de inicio de sesión de la web de administración.
import InputContrasena from '../../../shared/components/InputContrasena'
import { useSesion } from '../../../app/context/useSesion'
import { useLogin } from '../viewmodels/useLogin'
import './LoginView.css'

export default function LoginView() {
  const { aviso } = useSesion()
  const { correo, contrasena, errores, error, enviando, cambiarCorreo, cambiarContrasena, enviar } = useLogin()

  return (
    <main className="login">
      <section className="login-tarjeta">
        <p className="login-marca">
          AdBrand<b>.AI</b> <small>Admin</small>
        </p>
        <h1>Inicia sesión</h1>
        <p className="login-ayuda">Usa la cuenta que te creó el superadministrador.</p>

        {aviso && !error && <div className="login-aviso login-aviso-info">{aviso}</div>}
        {error && (
          <div className="login-aviso login-aviso-error" role="alert">
            {error}
          </div>
        )}

        <form className="login-formulario" onSubmit={enviar} noValidate>
          <label className={errores.correo ? 'login-campo login-campo-error' : 'login-campo'}>
            <span>Correo</span>
            <input
              type="email"
              autoComplete="username"
              value={correo}
              maxLength={150}
              onChange={(e) => cambiarCorreo(e.target.value)}
              placeholder="Ej.: nombre@adbrand.pe"
            />
            {errores.correo && <small>{errores.correo}</small>}
          </label>

          <label className={errores.contrasena ? 'login-campo login-campo-error' : 'login-campo'}>
            <span>Contraseña</span>
            <InputContrasena
              autoComplete="current-password"
              value={contrasena}
              maxLength={128}
              onChange={(e) => cambiarContrasena(e.target.value)}
            />
            {errores.contrasena && <small>{errores.contrasena}</small>}
          </label>

          <button type="submit" className="login-boton" disabled={enviando}>
            {enviando ? 'Ingresando…' : 'Ingresar'}
          </button>
        </form>
      </section>
    </main>
  )
}
