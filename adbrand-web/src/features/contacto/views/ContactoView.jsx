// View: Contacto (soporte). Formulario corto: nombre, correo, motivo y mensaje. Se abre desde el pie de página.
import { CORREO_CONTACTO } from '../../../shared/config/contacto'
import { MAXIMO_MENSAJE, MOTIVOS } from '../models/contactoApi'
import { useContacto } from '../viewmodels/useContacto'
import './ContactoView.css'

export default function ContactoView({ alVolver, nombre, correo, irAAyuda }) {
  const { datos, errores, enviando, enviado, error, cambiar, enviar, escribirOtro } = useContacto({ nombre, correo })

  return (
    <main className="pagina pagina-contacto">
      {alVolver && (
        <button type="button" className="boton-enlace contacto-volver" onClick={alVolver}>
          ← Volver
        </button>
      )}
      <header className="pagina-cabecera">
        <h1>Contacto</h1>
        <p>
          ¿Tienes una duda o algo no funciona? Escríbenos y te respondemos a tu correo en menos de 24 horas hábiles.
          {irAAyuda && (
            <>
              {' '}
              Antes, quizá tu respuesta ya está en{' '}
              <button type="button" className="boton-enlace" onClick={irAAyuda}>
                Ayuda
              </button>
              .
            </>
          )}
        </p>
      </header>

      {enviado ? (
        <section className="tarjeta contacto-enviado" role="status">
          <h2>✓ ¡Mensaje enviado!</h2>
          <p>
            Gracias, {datos.nombre.trim().split(/\s+/)[0]}. Te responderemos a <b>{datos.correo.trim()}</b>.
          </p>
          <button type="button" className="boton-secundario" onClick={escribirOtro}>
            Escribir otro mensaje
          </button>
        </section>
      ) : (
        <form
          className="tarjeta contacto-formulario"
          noValidate
          onSubmit={(evento) => {
            evento.preventDefault()
            enviar()
          }}
        >
          <div className="contacto-fila">
            <label className={errores.nombre ? 'campo campo-con-error' : 'campo'}>
              <span>Nombre</span>
              <input
                type="text"
                value={datos.nombre}
                maxLength={100}
                autoComplete="name"
                onChange={(e) => cambiar('nombre', e.target.value)}
              />
              {errores.nombre && <small className="campo-error">{errores.nombre}</small>}
            </label>
            <label className={errores.correo ? 'campo campo-con-error' : 'campo'}>
              <span>Correo</span>
              <input
                type="email"
                value={datos.correo}
                maxLength={150}
                autoComplete="email"
                onChange={(e) => cambiar('correo', e.target.value)}
              />
              {errores.correo && <small className="campo-error">{errores.correo}</small>}
            </label>
          </div>

          <label className={errores.asunto ? 'campo campo-con-error' : 'campo'}>
            <span>Motivo</span>
            <select value={datos.asunto} onChange={(e) => cambiar('asunto', e.target.value)}>
              <option value="">Elige una opción…</option>
              {MOTIVOS.map((m) => (
                <option key={m.valor} value={m.valor}>
                  {m.nombre}
                </option>
              ))}
            </select>
            {errores.asunto && <small className="campo-error">{errores.asunto}</small>}
          </label>

          <label className={errores.mensaje ? 'campo campo-con-error' : 'campo'}>
            <span>Mensaje</span>
            <textarea
              rows="6"
              value={datos.mensaje}
              maxLength={MAXIMO_MENSAJE}
              placeholder="Cuéntanos qué pasó o qué necesitas."
              onChange={(e) => cambiar('mensaje', e.target.value)}
            />
            {errores.mensaje ? (
              <small className="campo-error">{errores.mensaje}</small>
            ) : (
              <small className="campo-ayuda">
                {datos.mensaje.length} / {MAXIMO_MENSAJE}
              </small>
            )}
          </label>

          {error && <p className="aviso aviso-error">{error}</p>}

          <div className="contacto-acciones">
            <button type="submit" className="boton-primario" disabled={enviando}>
              {enviando ? 'Enviando…' : 'Enviar'}
            </button>
            <small>
              O escríbenos a <a href={`mailto:${CORREO_CONTACTO}`}>{CORREO_CONTACTO}</a>
            </small>
          </div>
        </form>
      )}
    </main>
  )
}
