// View: lista de administradores con botón desactivar y formulario de creación (HU-01, HU-03).
import { useAdministradores } from '../viewmodels/useAdministradores'
import './AdministradoresView.css'

function Campo({ etiqueta, error, children }) {
  return (
    <label className={error ? 'campo campo-con-error' : 'campo'}>
      <span>{etiqueta}</span>
      {children}
      {error && <small className="campo-error">{error}</small>}
    </label>
  )
}

function BadgeEstado({ estado }) {
  const esActivo = estado === 'ACTIVO'
  return (
    <span className={`badge-estado ${esActivo ? 'activo' : 'inactivo'}`}>
      {esActivo ? '● Activo' : '○ Inactivo'}
    </span>
  )
}

export default function AdministradoresView() {
  const {
    formulario,
    roles,
    administradores,
    errores,
    mensaje,
    enviando,
    desactivandoId,
    mostrarConfirmacion,
    adminADesactivar,
    cambiarCampo,
    enviar,
    abrirConfirmacionDesactivar,
    cerrarConfirmacionDesactivar,
    confirmarDesactivar,
  } = useAdministradores()

  function alEnviar(evento) {
    evento.preventDefault()
    enviar()
  }

  return (
    <main className="pagina">
      <header className="pagina-cabecera">
        <h1>Administradores</h1>
        <p>Solo el superadmin puede crear cuentas y asignar roles.</p>
      </header>

      {mensaje && <div className={`aviso aviso-${mensaje.tipo}`}>{mensaje.texto}</div>}

      {/* Formulario de creación (HU-01) */}
      <section className="tarjeta">
        <h2>Registrar administrador</h2>

        <form className="formulario" onSubmit={alEnviar} noValidate>
          <div className="fila">
            <Campo etiqueta="Nombres" error={errores.nombres}>
              <input
                value={formulario.nombres}
                placeholder="María"
                onChange={(e) => cambiarCampo('nombres', e.target.value)}
              />
            </Campo>
            <Campo etiqueta="Apellidos" error={errores.apellidos}>
              <input
                value={formulario.apellidos}
                placeholder="Torres"
                onChange={(e) => cambiarCampo('apellidos', e.target.value)}
              />
            </Campo>
          </div>

          <Campo etiqueta="Correo" error={errores.correo}>
            <input
              type="email"
              value={formulario.correo}
              placeholder="maria.torres@adbrand.pe"
              onChange={(e) => cambiarCampo('correo', e.target.value)}
            />
          </Campo>

          <Campo etiqueta="Contraseña" error={errores.contrasena}>
            <input
              type="password"
              value={formulario.contrasena}
              placeholder="Mínimo 8 caracteres"
              onChange={(e) => cambiarCampo('contrasena', e.target.value)}
            />
          </Campo>

          <Campo etiqueta="Rol" error={errores.rol_id}>
            <select value={formulario.rol_id} onChange={(e) => cambiarCampo('rol_id', e.target.value)}>
              <option value="">Selecciona un rol</option>
              {roles.map((rol) => (
                <option key={rol.id} value={rol.id}>
                  {rol.nombre}
                </option>
              ))}
            </select>
          </Campo>

          <button className="boton-primario" type="submit" disabled={enviando}>
            {enviando ? 'Guardando...' : 'Crear cuenta'}
          </button>
        </form>
      </section>

      {/* Lista de administradores (HU-03) */}
      <section className="tarjeta">
        <h2>Administradores registrados</h2>

        {administradores.length === 0 && (
          <p className="lista-vacia">No hay administradores registrados.</p>
        )}

        <table className="tabla-administradores">
          <thead>
            <tr>
              <th>ID</th>
              <th>Nombre completo</th>
              <th>Correo</th>
              <th>Rol</th>
              <th>Estado</th>
              <th>Creado</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {administradores.map((admin) => (
              <tr key={admin.id}>
                <td>#{admin.id}</td>
                <td>{admin.nombres} {admin.apellidos}</td>
                <td>{admin.correo}</td>
                <td>{admin.rol?.nombre ?? '—'}</td>
                <td><BadgeEstado estado={admin.estado} /></td>
                <td>{new Date(admin.fecha_creacion).toLocaleDateString('es-PE')}</td>
                <td className="acciones">
                  {admin.estado === 'ACTIVO' && (
                    <button
                      type="button"
                      className="boton-peligro boton-pequeno"
                      onClick={() => abrirConfirmacionDesactivar(admin)}
                      disabled={desactivandoId === admin.id}
                    >
                      {desactivandoId === admin.id ? 'Desactivando...' : 'Desactivar cuenta'}
                    </button>
                  )}
                  {admin.estado === 'INACTIVO' && (
                    <span className="texto-inactivo">Cuenta desactivada</span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>

      {/* Modal de confirmación para desactivar (HU-03) */}
      {mostrarConfirmacion && adminADesactivar && (
        <div className="modal-overlay" onClick={cerrarConfirmacionDesactivar}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-cabecera">
              <h3>⚠ Confirmar desactivación</h3>
              <button type="button" className="modal-cerrar" onClick={cerrarConfirmacionDesactivar}>×</button>
            </div>
            <div className="modal-cuerpo">
              <p>¿Estás seguro de que deseas desactivar la cuenta de <strong>{adminADesactivar.nombres} {adminADesactivar.apellidos}</strong> ({adminADesactivar.correo})?</p>
              <p className="advertencia">El administrador no podrá iniciar sesión ni acceder al sistema hasta que sea reactivado.</p>
            </div>
            <div className="modal-pie">
              <button type="button" className="boton-secundario" onClick={cerrarConfirmacionDesactivar} disabled={desactivandoId === adminADesactivar.id}>
                Cancelar
              </button>
              <button type="button" className="boton-peligro" onClick={confirmarDesactivar} disabled={desactivandoId === adminADesactivar.id}>
                {desactivandoId === adminADesactivar.id ? 'Desactivando...' : 'Sí, desactivar'}
              </button>
            </div>
          </div>
        </div>
      )}
    </main>
  )
}