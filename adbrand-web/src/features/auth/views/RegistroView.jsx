// View: formulario para registrar la empresa (HU-21). La lógica está en useRegistro.
import { useRegistro } from '../viewmodels/useRegistro'
import CampoAuth from './CampoAuth'
import './Auth.css'

export default function RegistroView({ irALogin }) {
  const { formulario, errores, mensaje, enviando, creada, cambiarCampo, enviar } = useRegistro()

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
        <h1>Crea la cuenta de tu empresa</h1>
        <p>Con tu cuenta, la IA guarda el perfil, los anuncios y el kit de marca solo para tu negocio.</p>
      </header>

      {mensaje && <div className={`aviso aviso-${mensaje.tipo}`}>{mensaje.texto}</div>}

      <form className="tarjeta formulario" onSubmit={alEnviar} noValidate>
        <CampoAuth etiqueta="Tu nombre" error={errores.nombre}>
          <input
            value={formulario.nombre}
            placeholder="Rosa Quispe"
            autoComplete="name"
            onChange={(e) => cambiarCampo('nombre', e.target.value)}
          />
        </CampoAuth>

        <CampoAuth etiqueta="Nombre de la empresa" error={errores.nombreEmpresa}>
          <input
            value={formulario.nombreEmpresa}
            placeholder="Panadería Doña Rosa"
            autoComplete="organization"
            onChange={(e) => cambiarCampo('nombreEmpresa', e.target.value)}
          />
        </CampoAuth>

        <CampoAuth etiqueta="Correo" error={errores.correo}>
          <input
            type="email"
            value={formulario.correo}
            placeholder="rosa@panaderia.pe"
            autoComplete="email"
            onChange={(e) => cambiarCampo('correo', e.target.value)}
          />
        </CampoAuth>

        <CampoAuth
          etiqueta="Contraseña"
          error={errores.contrasena}
          ayuda="Mínimo 8 caracteres, con una mayúscula, una minúscula y un número."
        >
          <input
            type="password"
            value={formulario.contrasena}
            autoComplete="new-password"
            onChange={(e) => cambiarCampo('contrasena', e.target.value)}
          />
        </CampoAuth>

        <CampoAuth etiqueta="Confirmar contraseña" error={errores.confirmarContrasena}>
          <input
            type="password"
            value={formulario.confirmarContrasena}
            autoComplete="new-password"
            onChange={(e) => cambiarCampo('confirmarContrasena', e.target.value)}
          />
        </CampoAuth>

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
