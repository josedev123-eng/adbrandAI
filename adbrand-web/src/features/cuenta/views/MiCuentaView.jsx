// View: solo dibuja los datos de la empresa. La lógica está en useMiCuenta.
// Primero se ven los datos para leer; el botón "Editar" abre el formulario.
import Aviso from '../../../shared/components/Aviso'
import Cargando from '../../../shared/components/Cargando'
import { COLORES_AVATAR, colorDeAvatar } from '../../../shared/components/avatar'
import AvatarConImagen from './AvatarConImagen'
import { iniciales } from '../../../shared/components/opcionesMenuUsuario'
import { useMiCuenta } from '../viewmodels/useMiCuenta'
import CambiarContrasena from './CambiarContrasena'
import './MiCuentaView.css'

function Campo({ etiqueta, error, ayuda, children }) {
  return (
    <label className={error ? 'campo campo-con-error' : 'campo'}>
      <span>{etiqueta}</span>
      {children}
      {error ? <small className="campo-error">{error}</small> : ayuda && <small className="campo-ayuda">{ayuda}</small>}
    </label>
  )
}

function Dato({ etiqueta, valor }) {
  return (
    <div className="dato">
      <dt>{etiqueta}</dt>
      <dd className={valor ? undefined : 'dato-vacio'}>{valor || 'Sin completar'}</dd>
    </div>
  )
}

// "avatarImagen" y "alCambiarAvatar": el avatar con imagen (logo del kit o foto) que también se ve en el menú.
export default function MiCuentaView({ alGuardar, alVerTutorial, avatarImagen, alCambiarAvatar }) {
  const {
    datos,
    cuenta,
    errores,
    mensaje,
    cargando,
    guardando,
    editando,
    empezarEdicion,
    cancelarEdicion,
    cambiarCampo,
    guardar,
  } = useMiCuenta(alGuardar)

  function alEnviar(evento) {
    evento.preventDefault()
    guardar()
  }

  if (cargando) {
    return (
      <main className="pagina">
        <Cargando texto="Cargando tu cuenta..." />
      </main>
    )
  }

  return (
    <main className="pagina">
      <header className="pagina-cabecera">
        <h1>Mi Cuenta</h1>
        <p>Los datos de tu empresa. Solo tú puedes verlos y cambiarlos.</p>
      </header>

      <Aviso mensaje={mensaje} />

      {cuenta && (
        <section className="tarjeta cuenta-fija" aria-label="Tu cuenta">
          <div className="cuenta-empresa">
            {avatarImagen ? (
              <img className="cuenta-avatar cuenta-avatar-imagen" src={avatarImagen} alt="" />
            ) : (
              <span
                className="cuenta-avatar"
                aria-hidden="true"
                style={{ background: colorDeAvatar(cuenta.nombreEmpresa, editando ? datos.colorAvatar : cuenta.colorAvatar) }}
              >
                {iniciales(cuenta.nombreEmpresa)}
              </span>
            )}
            <span className="cuenta-empresa-texto">
              <span>Empresa</span>
              <b>{cuenta.nombreEmpresa}</b>
            </span>
          </div>
          <div>
            <span>Correo</span>
            <b>{cuenta.correo}</b>
          </div>
        </section>
      )}

      {cuenta && alCambiarAvatar && <AvatarConImagen imagen={avatarImagen} alCambiar={alCambiarAvatar} />}

      {!editando && cuenta && (
        <section className="tarjeta datos-lectura" aria-label="Datos de la empresa">
          <div className="datos-lectura-cabecera">
            <h2>Datos de la empresa</h2>
            <button type="button" className="boton-secundario" onClick={empezarEdicion}>
              ✎ Editar
            </button>
          </div>
          <dl className="datos-lista">
            <Dato etiqueta="RUC" valor={datos.ruc} />
            <Dato etiqueta="Teléfono" valor={datos.telefono} />
            <Dato etiqueta="Razón social" valor={datos.razonSocial} />
            <Dato etiqueta="Dirección" valor={datos.direccion} />
          </dl>
        </section>
      )}

      {editando && (
        <form className="tarjeta formulario" onSubmit={alEnviar} noValidate aria-label="Editar datos de la empresa">
          <h2 className="formulario-titulo">{cuenta?.ruc ? 'Editar datos de la empresa' : 'Completa los datos de tu empresa'}</h2>
          <div className="fila">
            <Campo etiqueta="RUC" error={errores.ruc} ayuda="11 dígitos, empieza en 10 o 20.">
              <input
                value={datos.ruc}
                inputMode="numeric"
                maxLength={11}
                placeholder="Ej.: 20601234567"
                onChange={(e) => cambiarCampo('ruc', e.target.value)}
              />
            </Campo>
            <Campo etiqueta="Teléfono (opcional)" error={errores.telefono} ayuda="Celular de 9 dígitos o fijo de 7.">
              <input
                value={datos.telefono}
                inputMode="tel"
                maxLength={9}
                placeholder="Ej.: 987654321"
                onChange={(e) => cambiarCampo('telefono', e.target.value)}
              />
            </Campo>
          </div>

          <Campo etiqueta="Razón social" error={errores.razonSocial}>
            <input
              value={datos.razonSocial}
              maxLength={200}
              placeholder="Ej.: Panadería Doña Rosa S.A.C."
              onChange={(e) => cambiarCampo('razonSocial', e.target.value)}
            />
          </Campo>

          <Campo etiqueta="Dirección" error={errores.direccion}>
            <input
              value={datos.direccion}
              maxLength={250}
              placeholder="Ej.: Av. Los Próceres 123, Surco, Lima"
              onChange={(e) => cambiarCampo('direccion', e.target.value)}
            />
          </Campo>

          <fieldset className="colores-avatar">
            <legend>Color de tu avatar</legend>
            <div className="colores-avatar-opciones">
              <label className={!datos.colorAvatar ? 'color-avatar color-avatar-activo' : 'color-avatar'}>
                <input
                  type="radio"
                  name="colorAvatar"
                  value=""
                  checked={!datos.colorAvatar}
                  onChange={() => cambiarCampo('colorAvatar', '')}
                />
                <span className="color-avatar-muestra color-avatar-auto" aria-hidden="true">A</span>
                Automático
              </label>
              {COLORES_AVATAR.map((opcion) => (
                <label
                  key={opcion.id}
                  className={datos.colorAvatar === opcion.id ? 'color-avatar color-avatar-activo' : 'color-avatar'}
                >
                  <input
                    type="radio"
                    name="colorAvatar"
                    value={opcion.id}
                    checked={datos.colorAvatar === opcion.id}
                    onChange={() => cambiarCampo('colorAvatar', opcion.id)}
                  />
                  <span className="color-avatar-muestra" style={{ background: opcion.color }} aria-hidden="true" />
                  {opcion.nombre}
                </label>
              ))}
            </div>
          </fieldset>

          <div className="formulario-acciones">
            {cuenta?.ruc && (
              <button type="button" className="boton-secundario" onClick={cancelarEdicion} disabled={guardando}>
                Cancelar
              </button>
            )}
            <button className="boton-primario" type="submit" disabled={guardando}>
              {guardando ? 'Guardando...' : 'Guardar cambios'}
            </button>
          </div>
        </form>
      )}

      <CambiarContrasena />

      {alVerTutorial && (
        <section className="tarjeta cuenta-tutorial" aria-labelledby="cuenta-tutorial-titulo">
          <div>
            <h2 id="cuenta-tutorial-titulo" className="formulario-titulo">
              Recorrido por AdBrand.AI
            </h2>
            <p>Vuelve a ver, paso a paso, dónde está cada cosa: perfil, kit de marca, anuncios, calendario y planes.</p>
          </div>
          <button type="button" className="boton-secundario" onClick={alVerTutorial}>
            Ver el tutorial otra vez
          </button>
        </section>
      )}
    </main>
  )
}
