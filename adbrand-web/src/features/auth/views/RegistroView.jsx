// View: formulario para registrar la empresa (HU-21). La lógica está en useRegistro.
import InputContrasena from '../../../shared/components/InputContrasena'
import Aviso from '../../../shared/components/Aviso'
import { useRegistro } from '../viewmodels/useRegistro'
import CampoAuth from './CampoAuth'
import './Auth.css'

export default function RegistroView({ irALogin, abrir, irAInicio }) {
  const { formulario, errores, mensaje, enviando, creada, cambiarCampo, enviar } = useRegistro()

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
        <CampoAuth etiqueta="Tu nombre" error={errores.nombre}>
          <input
            value={formulario.nombre}
            maxLength={100}
            placeholder="Ej.: Rosa Quispe"
            autoComplete="name"
            onChange={(e) => cambiarCampo('nombre', e.target.value)}
          />
        </CampoAuth>

        <CampoAuth etiqueta="Nombre de la empresa" error={errores.nombreEmpresa}>
          <input
            value={formulario.nombreEmpresa}
            maxLength={150}
            placeholder="Ej.: Panadería Doña Rosa"
            autoComplete="organization"
            onChange={(e) => cambiarCampo('nombreEmpresa', e.target.value)}
          />
        </CampoAuth>

        <CampoAuth etiqueta="Correo" error={errores.correo}>
          <input
            type="email"
            value={formulario.correo}
            maxLength={150}
            placeholder="Ej.: rosa@panaderia.pe"
            autoComplete="email"
            onChange={(e) => cambiarCampo('correo', e.target.value)}
          />
        </CampoAuth>

        <CampoAuth
          etiqueta="Contraseña"
          error={errores.contrasena}
          ayuda="Mínimo 8 caracteres, con una mayúscula, una minúscula y un número."
        >
          <InputContrasena
            value={formulario.contrasena}
            maxLength={72}
            autoComplete="new-password"
            onChange={(e) => cambiarCampo('contrasena', e.target.value)}
          />
        </CampoAuth>

        <CampoAuth etiqueta="Confirmar contraseña" error={errores.confirmarContrasena}>
          <InputContrasena
            value={formulario.confirmarContrasena}
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
