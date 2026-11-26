// View: formulario para registrar la empresa (HU-21). La lógica está en useRegistro.
import InputContrasena from '../../../shared/components/InputContrasena'
import Aviso from '../../../shared/components/Aviso'
import { useRegistro } from '../viewmodels/useRegistro'
import { reglasDeContrasena } from '../viewmodels/validaciones'
import CampoAuth from './CampoAuth'
import './Auth.css'

export default function RegistroView({ irALogin, abrir, irAInicio, irAVerificar }) {
  const { formulario, errores, validos, mensaje, enviando, creada, cambiarCampo, tocar, enviar } = useRegistro(irAVerificar)

  function alEnviar(evento) {
    evento.preventDefault()
    enviar()
  }

  return (
    <main className="pagina pagina-auth">
      {irAInicio && (
        <button type="button" className="boton-enlace auth-volver" onClick={irAInicio}>
          ← Volver al inicio
        </button>
      )}
      <p className="auth-marca">
        AdBrand<b>.AI</b>
      </p>
      <header className="pagina-cabecera">
        <h1>Crea la cuenta de tu empresa</h1>
        <p>Con tu cuenta, la IA guarda el perfil, los anuncios y el kit de marca solo para tu negocio.</p>
      </header>

      <Aviso mensaje={mensaje} />

      <form className="tarjeta formulario" onSubmit={alEnviar} noValidate>
        <CampoAuth etiqueta="Tu nombre" error={errores.nombre} valido={validos.nombre} ayuda="Solo letras y espacios.">
          <input
            value={formulario.nombre}
            onBlur={() => tocar('nombre')}
            maxLength={100}
            placeholder="Ej.: Rosa Quispe"
            autoComplete="name"
            onChange={(e) => cambiarCampo('nombre', e.target.value)}
          />
        </CampoAuth>

        <CampoAuth
          etiqueta="Nombre de la empresa"
          error={errores.nombreEmpresa}
          valido={validos.nombreEmpresa}
          ayuda="Letras, números y signos simples (& . , -)."
        >
          <input
            value={formulario.nombreEmpresa}
            onBlur={() => tocar('nombreEmpresa')}
            maxLength={150}
            placeholder="Ej.: Panadería Doña Rosa"
            autoComplete="organization"
            onChange={(e) => cambiarCampo('nombreEmpresa', e.target.value)}
          />
        </CampoAuth>

        <CampoAuth
          etiqueta="Correo"
          error={errores.correo}
          valido={validos.correo}
          ayuda="Te enviaremos un código para confirmarlo."
        >
          <input
            type="email"
            value={formulario.correo}
            onBlur={() => tocar('correo')}
            maxLength={150}
            placeholder="Ej.: rosa@panaderia.pe"
            autoComplete="email"
            onChange={(e) => cambiarCampo('correo', e.target.value)}
          />
        </CampoAuth>

        <CampoAuth
          etiqueta="Contraseña"
          error={errores.contrasena}
          valido={validos.contrasena}
          reglas={reglasDeContrasena(formulario.contrasena)}
        >
          <InputContrasena
            value={formulario.contrasena}
            onBlur={() => tocar('contrasena')}
            maxLength={72}
            autoComplete="new-password"
            onChange={(e) => cambiarCampo('contrasena', e.target.value)}
          />
        </CampoAuth>

        <CampoAuth
          etiqueta="Confirmar contraseña"
          error={errores.confirmarContrasena}
          valido={validos.confirmarContrasena}
          ayuda={validos.confirmarContrasena ? 'Las contraseñas coinciden.' : 'Escríbela otra vez.'}
        >
          <InputContrasena
            value={formulario.confirmarContrasena}
            onBlur={() => tocar('confirmarContrasena')}
            maxLength={72}
            autoComplete="new-password"
            onChange={(e) => cambiarCampo('confirmarContrasena', e.target.value)}
          />
        </CampoAuth>

        <div className={errores.aceptaTerminos ? 'auth-casilla auth-casilla-error' : 'auth-casilla'}>
          <input
            id="registro-acepta"
            type="checkbox"
            checked={formulario.aceptaTerminos}
            aria-invalid={Boolean(errores.aceptaTerminos)}
            aria-describedby={errores.aceptaTerminos ? 'registro-acepta-error' : undefined}
            onChange={(e) => cambiarCampo('aceptaTerminos', e.target.checked)}
          />
          <label htmlFor="registro-acepta">
            Acepto los{' '}
            <button type="button" className="boton-enlace" onClick={() => abrir?.('terminos')}>
              Términos y condiciones
            </button>{' '}
            y la{' '}
            <button type="button" className="boton-enlace" onClick={() => abrir?.('privacidad')}>
              Política de privacidad
            </button>
            .
          </label>
          {errores.aceptaTerminos && (
            <small id="registro-acepta-error" className="campo-error">
              {errores.aceptaTerminos}
            </small>
          )}
        </div>

        <button className="boton-primario" type="submit" disabled={enviando}>
          {enviando ? 'Creando cuenta...' : 'Crear cuenta'}
        </button>
      </form>

      {irALogin && (
        <div className="auth-enlaces">
          <span>{creada ? 'Tu cuenta está lista.' : '¿Ya tienes cuenta?'}</span>
          <button type="button" className="boton-enlace" onClick={irALogin}>
            Iniciar sesión
          </button>
        </div>
      )}
    </main>
  )
}
