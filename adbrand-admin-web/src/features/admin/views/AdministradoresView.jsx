// View: lista de administradores con desactivar (HU-03) y editar permisos (HU-02).
// El formulario para crear cuentas está solo en CrearAdministradorView (HU-01).
import { useSesion } from '../../../app/context/useSesion'
import { useAdministradores } from '../viewmodels/useAdministradores'
import { useEditarPermisos } from '../viewmodels/useEditarPermisos'
import EditarPermisosModal from './EditarPermisosModal'
import './AdministradoresView.css'

function BadgeEstado({ estado }) {
  const esActivo = estado === 'ACTIVO'
  return (
    <span className={`badge-estado ${esActivo ? 'activo' : 'inactivo'}`}>
      {esActivo ? '● Activo' : '○ Inactivo'}
    </span>
  )
}

export default function AdministradoresView() {
  const { admin: adminConectado } = useSesion()
  const {
    roles,
    administradores,
    mensaje,
    desactivandoId,
    mostrarConfirmacion,
    adminADesactivar,
    abrirConfirmacionDesactivar,
    cerrarConfirmacionDesactivar,
    confirmarDesactivar,
    recargarAdministradores,
  } = useAdministradores()

  // HU-02: edición de permisos (rol) de un administrador.
  const {
    adminAEditar,
    rolId,
    errorPermisos,
    mensajePermisos,
    guardando,
    puedeEditar,
    abrirEdicion,
    cerrarEdicion,
    cambiarRol,
    guardarPermisos,
  } = useEditarPermisos(recargarAdministradores, adminConectado.id)

  return (
    <main className="pagina pagina-administradores">
      <header className="pagina-cabecera">
        <h1>Lista de administradores</h1>
        <p>Revisa las cuentas, cambia sus permisos o desactívalas. Para crear una cuenta ve a Administradores.</p>
      </header>

      {mensaje && <div className={`aviso aviso-${mensaje.tipo}`}>{mensaje.texto}</div>}
      {mensajePermisos && <div className={`aviso aviso-${mensajePermisos.tipo}`}>{mensajePermisos.texto}</div>}

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
                  {/* Editar permisos (HU-02) */}
                  <button
                    type="button"
                    className="boton-secundario boton-pequeno boton-editar-permisos"
                    onClick={() => abrirEdicion(admin)}
                    disabled={!puedeEditar(admin)}
                    title={puedeEditar(admin) ? undefined : 'No puedes modificar tus propios permisos.'}
                  >
                    Editar permisos
                  </button>
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

      {/* Modal para editar permisos (HU-02) */}
      {adminAEditar && (
        <EditarPermisosModal
          admin={adminAEditar}
          roles={roles}
          rolId={rolId}
          error={errorPermisos}
          guardando={guardando}
          alCambiarRol={cambiarRol}
          alGuardar={guardarPermisos}
          alCerrar={cerrarEdicion}
        />
      )}
    </main>
  )
}