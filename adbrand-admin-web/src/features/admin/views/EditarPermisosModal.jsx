// View: modal para elegir el nuevo rol de un administrador (HU-02). La lógica está en useEditarPermisos.
import './EditarPermisosModal.css'

export default function EditarPermisosModal({ admin, roles, rolId, error, guardando, alCambiarRol, alGuardar, alCerrar }) {
  function alEnviar(evento) {
    evento.preventDefault()
    alGuardar()
  }

  return (
    <div className="modal-overlay" onClick={alCerrar}>
      <form className="modal" onClick={(e) => e.stopPropagation()} onSubmit={alEnviar} noValidate>
        <div className="modal-cabecera">
          <h3>Editar permisos</h3>
          <button type="button" className="modal-cerrar" onClick={alCerrar} aria-label="Cerrar">×</button>
        </div>
        <div className="modal-cuerpo">
          <p>
            Cuenta de <strong>{admin.nombres} {admin.apellidos}</strong> ({admin.correo}).
          </p>
          <p className="permisos-rol-actual">Rol actual: {admin.rol?.nombre ?? 'Sin rol'}</p>

          <label className={error ? 'campo campo-con-error' : 'campo'}>
            <span>Nuevo rol</span>
            <select value={rolId} onChange={(e) => alCambiarRol(e.target.value)} disabled={guardando}>
              <option value="">Selecciona un rol</option>
              {roles.map((rol) => (
                <option key={rol.id} value={rol.id}>
                  {rol.nombre}
                </option>
              ))}
            </select>
            {error && <small className="campo-error">{error}</small>}
          </label>
        </div>
        <div className="modal-pie">
          <button type="button" className="boton-secundario" onClick={alCerrar} disabled={guardando}>
            Cancelar
          </button>
          <button type="submit" className="boton-primario" disabled={guardando}>
            {guardando ? 'Guardando...' : 'Guardar permisos'}
          </button>
        </div>
      </form>
    </div>
  )
}
