// View: recuperar la contraseña en dos pasos (HU-24). La lógica está en useRecuperarContrasena.
import { useRecuperarContrasena } from '../viewmodels/useRecuperarContrasena'
import CampoAuth from './CampoAuth'
import './Auth.css'

export default function RecuperarContrasenaView({ irALogin, alTerminar }) {
  const {
    paso,
    correo,
    formulario,
    errores,
    mensaje,
    enviando,
    cambiarCorreo,
    cambiarCampo,
    pedirCodigo,
    cambiar,
    volverAlPaso1,
  } = useRecuperarContrasena(alTerminar)

  function alEnviar(evento) {
    evento.preventDefault()
    if (paso === 1) pedirCodigo()
    else cambiar()
  }

  return (
    <main className="pagina pagina-auth">
      <p className="auth-marca">
        AdBrand<b>.AI</b>
      </p>
      <header className="pagina-cabecera">
        <h1>Recupera tu contraseña</h1>
        <p>
          {paso === 1
            ? 'Te enviaremos un código de 6 dígitos a tu correo.'
            : `Escribe el código que enviamos a ${correo.trim()} y tu contraseña nueva.`}
        </p>
      </header>

      {mensaje && <div className={`aviso aviso-${mensaje.tipo}`}>{mensaje.texto}</div>}

      <form className="tarjeta formulario" onSubmit={alEnviar} noValidate>
        {paso === 1 && (
          <CampoAuth etiqueta="Correo" error={errores.correo}>
            <input
              type="email"
              value={correo}
              placeholder="rosa@panaderia.pe"
              autoComplete="email"
              onChange={(e) => cambiarCorreo(e.target.value)}
            />
          </CampoAuth>
        )}

        {paso === 2 && (
          <>
            <CampoAuth etiqueta="Código" error={errores.codigo} ayuda="Vence a los 15 minutos y sirve una sola vez.">
              <input
                className="campo-codigo"
                value={formulario.codigo}
                inputMode="numeric"
                maxLength={6}
                placeholder="123456"
                autoComplete="one-time-code"
                onChange={(e) => cambiarCampo('codigo', e.target.value)}
              />
            </CampoAuth>

            <CampoAuth
              etiqueta="Contraseña nueva"
              error={errores.contrasenaNueva}
              ayuda="Mínimo 8 caracteres, con una mayúscula, una minúscula y un número."
            >
              <input
                type="password"
                value={formulario.contrasenaNueva}
                autoComplete="new-password"
                onChange={(e) => cambiarCampo('contrasenaNueva', e.target.value)}
              />
            </CampoAuth>

            <CampoAuth etiqueta="Confirmar contraseña nueva" error={errores.confirmarContrasena}>
              <input
                type="password"
                value={formulario.confirmarContrasena}
                autoComplete="new-password"
                onChange={(e) => cambiarCampo('confirmarContrasena', e.target.value)}
              />
            </CampoAuth>
          </>
        )}

        <button className="boton-primario" type="submit" disabled={enviando}>
          {paso === 1 && (enviando ? 'Enviando...' : 'Enviar código')}
          {paso === 2 && (enviando ? 'Guardando...' : 'Cambiar contraseña')}
        </button>
      </form>

      <div className="auth-enlaces">
        {paso === 2 ? (
          <button type="button" className="boton-enlace" onClick={volverAlPaso1}>
            Pedir otro código
          </button>
        ) : (
          <span />
        )}
        <button type="button" className="boton-enlace" onClick={irALogin}>
          Volver a iniciar sesión
        </button>
      </div>
    </main>
  )
}
