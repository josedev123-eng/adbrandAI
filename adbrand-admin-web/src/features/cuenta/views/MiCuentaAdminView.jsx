// View: Mi cuenta del administrador. Sus datos (solo lectura) y el formulario para cambiar su contraseña.
import { useSesion } from '../../../app/context/useSesion'
import { NOMBRES_ROL } from '../../../app/routes/pantallasPorRol'
import Aviso from '../../../shared/components/Aviso'
import InputContrasena from '../../../shared/components/InputContrasena'
import { useMiCuentaAdmin } from '../viewmodels/useMiCuentaAdmin'
import './MiCuentaAdminView.css'

function Campo({ etiqueta, error, ayuda, children }) {
  return (
    <label className={error ? 'campo campo-con-error' : 'campo'}>
      <span>{etiqueta}</span>
      {children}
      {error ? <small className="campo-error">{error}</small> : ayuda && <small className="campo-ayuda">{ayuda}</small>}
    </label>
  )
}

// "obligatorio": primer inicio con la contraseña temporal que puso el superadmin. Solo se ve el formulario
// para cambiarla (y salir); el resto del panel se abre cuando la cambia.
export default function MiCuentaAdminView({ obligatorio = false, alSalir }) {
  const { admin } = useSesion()
  const { datos, errores, mensaje, guardando, cambiar, guardar } = useMiCuentaAdmin()

  return (
    <main className="pagina pagina-mi-cuenta-admin">
      <header className="pagina-cabecera">
        <h1>{obligatorio ? 'Crea tu contraseña' : 'Mi cuenta'}</h1>
        <p>
          {obligatorio
            ? `Hola, ${admin?.nombres ?? ''}. Entraste con la contraseña temporal que te dio el superadministrador. Por seguridad, elige una propia para continuar.`
            : 'Tus datos de administrador y tu contraseña.'}
        </p>
      </header>

      {!obligatorio && (
      <section className="tarjeta" aria-labelledby="mi-cuenta-datos">
        <h2 id="mi-cuenta-datos" className="mi-cuenta-titulo">
          Tus datos
        </h2>
        <dl className="mi-cuenta-datos">
          <div>
            <dt>Nombre</dt>
            <dd>
              {admin?.nombres} {admin?.apellidos}
            </dd>
          </div>
          <div>
            <dt>Correo</dt>
            <dd>{admin?.correo}</dd>
          </div>
          <div>
            <dt>Rol</dt>
            <dd>{NOMBRES_ROL[admin?.rol?.nombre] ?? admin?.rol?.nombre}</dd>
          </div>
        </dl>
        <p className="mi-cuenta-nota">Para cambiar tu nombre, correo o rol, pídeselo al superadministrador.</p>
      </section>
      )}

      <form className="tarjeta formulario" onSubmit={guardar} noValidate aria-labelledby="mi-cuenta-clave">
        <h2 id="mi-cuenta-clave" className="mi-cuenta-titulo">
          {obligatorio ? 'Tu contraseña nueva' : 'Cambiar contraseña'}
        </h2>
        <Aviso mensaje={mensaje} />
        <Campo etiqueta={obligatorio ? 'Contraseña temporal' : 'Contraseña actual'} error={errores.actual}>
          <InputContrasena
            autoComplete="current-password"
            value={datos.actual}
            maxLength={128}
            onChange={(e) => cambiar('actual', e.target.value)}
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
        <button type="submit" className="boton-primario" disabled={guardando}>
          {guardando ? 'Guardando…' : obligatorio ? 'Guardar y entrar al panel' : 'Cambiar contraseña'}
        </button>
        {obligatorio && alSalir && (
          <button type="button" className="boton-secundario" onClick={alSalir}>
            Cerrar sesión
          </button>
        )}
      </form>
    </main>
  )
}
