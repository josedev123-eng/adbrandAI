// View: "Confirma tu correo" después de registrarse en la web. La lógica está en useVerificarCorreo.
import Aviso from '../../../shared/components/Aviso'
import { useVerificarCorreo } from '../viewmodels/useVerificarCorreo'
import CampoAuth from './CampoAuth'
import './Auth.css'

export default function VerificarCorreoView({ correo, desdeLogin = false, irALogin }) {
  const { codigo, error, mensaje, enviando, espera, cambiarCodigo, confirmar, reenviar } = useVerificarCorreo(correo, {
    pedirAlEntrar: desdeLogin,
  })

  function alEnviar(evento) {
    evento.preventDefault()
    confirmar()
  }

  return (
    <main className="pagina pagina-auth">
      <p className="auth-marca">
        AdBrand<b>.AI</b>
      </p>
      <header className="pagina-cabecera">
        <h1>Confirma tu correo</h1>
        <p>
          {desdeLogin ? 'Antes de entrar falta confirmar tu correo. ' : 'Tu cuenta está creada. '}
          Escribe el código de 6 dígitos que enviamos a <b>{correo}</b>.
        </p>
      </header>

      <Aviso mensaje={mensaje} />

      <form className="tarjeta formulario" onSubmit={alEnviar} noValidate>
        <CampoAuth etiqueta="Código" error={error} ayuda="Vence a los 15 minutos. Si no lo ves, revisa la carpeta de spam.">
          <input
            className="campo-codigo"
            value={codigo}
            inputMode="numeric"
            maxLength={6}
            placeholder="Ej.: 123456"
            autoComplete="one-time-code"
            autoFocus
            onChange={(e) => cambiarCodigo(e.target.value)}
          />
        </CampoAuth>

        <button className="boton-primario" type="submit" disabled={enviando}>
          {enviando ? 'Confirmando...' : 'Confirmar y entrar'}
        </button>
      </form>

      <div className="auth-enlaces">
        <button type="button" className="boton-enlace" onClick={reenviar} disabled={espera > 0}>
          {espera > 0 ? `Reenviar código en ${espera} s` : 'Reenviar código'}
        </button>
        <button type="button" className="boton-enlace" onClick={irALogin}>
          Volver a iniciar sesión
        </button>
      </div>
    </main>
  )
}
