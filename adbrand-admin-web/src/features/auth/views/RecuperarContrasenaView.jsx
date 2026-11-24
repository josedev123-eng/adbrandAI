// View: "¿Olvidaste tu contraseña?" del panel de administración. Usa el mismo diseño que el inicio de sesión.
import InputContrasena from '../../../shared/components/InputContrasena'
import { useRecuperarContrasena } from '../viewmodels/useRecuperarContrasena'
import './LoginView.css'

function Campo({ etiqueta, error, ayuda, children }) {
  return (
    <label className={error ? 'login-campo login-campo-error' : 'login-campo'}>
      <span>{etiqueta}</span>
      {children}
      {error ? <small>{error}</small> : ayuda && <small className="login-campo-ayuda">{ayuda}</small>}
    </label>
  )
}

export default function RecuperarContrasenaView({ irALogin, alTerminar }) {
  const { paso, datos, errores, mensaje, enviando, cambiar, pedirCodigo, cambiarContrasena, volverAPedir } =
    useRecuperarContrasena(alTerminar)

  return (
    <main className="login">
      <section className="login-tarjeta">
        <p className="login-marca">
          AdBrand<b>.AI</b> <small>Admin</small>
        </p>
        <h1>Recupera tu contraseña</h1>
        <p className="login-ayuda">
          {paso === 'correo'
            ? 'Escribe el correo de tu cuenta de administrador y te enviaremos un código de 6 números.'
            : `Escribe el código que enviamos a ${datos.correo.trim()} y tu contraseña nueva.`}
        </p>

        {mensaje && (
          <div className={mensaje.tipo === 'error' ? 'login-aviso login-aviso-error' : 'login-aviso login-aviso-info'} role="status">
            {mensaje.texto}
          </div>
        )}

        {paso === 'correo' ? (
          <form className="login-formulario" onSubmit={pedirCodigo} noValidate>
            <Campo etiqueta="Correo" error={errores.correo}>
              <input
                type="email"
                autoComplete="username"
                value={datos.correo}
                maxLength={150}
                placeholder="Ej.: nombre@adbrand.pe"
                onChange={(e) => cambiar('correo', e.target.value)}
              />
            </Campo>
            <button type="submit" className="login-boton" disabled={enviando}>
              {enviando ? 'Enviando…' : 'Enviarme el código'}
            </button>
          </form>
        ) : (
          <form className="login-formulario" onSubmit={cambiarContrasena} noValidate>
            <Campo etiqueta="Código de 6 números" error={errores.codigo}>
              <input
                inputMode="numeric"
                autoComplete="one-time-code"
                value={datos.codigo}
                placeholder="000000"
                onChange={(e) => cambiar('codigo', e.target.value)}
              />
            </Campo>
            <Campo
              etiqueta="Contraseña nueva"
              error={errores.nueva}
              ayuda="Mínimo 8 caracteres, con una mayúscula, una minúscula y un número."
            >
              <InputContrasena
                autoComplete="new-password"
                value={datos.nueva}
                maxLength={128}
                onChange={(e) => cambiar('nueva', e.target.value)}
              />
            </Campo>
            <Campo etiqueta="Confirmar contraseña nueva" error={errores.confirmar}>
              <InputContrasena
                autoComplete="new-password"
                value={datos.confirmar}
                maxLength={128}
                onChange={(e) => cambiar('confirmar', e.target.value)}
              />
            </Campo>
            <button type="submit" className="login-boton" disabled={enviando}>
              {enviando ? 'Guardando…' : 'Cambiar contraseña'}
            </button>
            <button type="button" className="login-enlace" onClick={volverAPedir}>
              ¿No te llegó? Pedir otro código
            </button>
          </form>
        )}

        <button type="button" className="login-enlace" onClick={irALogin}>
          ← Volver a iniciar sesión
        </button>
      </section>
    </main>
  )
}
