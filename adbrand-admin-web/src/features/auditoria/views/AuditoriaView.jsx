// View: historial de acciones de los administradores con filtros (HU-04). La lógica está en useAuditoria.
import { formatearFecha, NOMBRES_ACCION, NOMBRES_ENTIDAD } from '../models/auditoriaApi'
import { useAuditoria } from '../viewmodels/useAuditoria'
import './AuditoriaView.css'

export default function AuditoriaView() {
  const {
    registros,
    administradores,
    filtros,
    hayFiltros,
    cargando,
    error,
    errorFiltros,
    cambiarFiltro,
    aplicarFiltros,
    limpiarFiltros,
  } = useAuditoria()

  function alEnviar(evento) {
    evento.preventDefault()
    aplicarFiltros()
  }

  return (
    <main className="pagina pagina-auditoria">
      <header className="pagina-cabecera">
        <h1>Auditoría</h1>
        <p>Registro de las acciones críticas que realiza cada administrador.</p>
      </header>

      {error && <div className="aviso aviso-error">{error}</div>}

      <section className="tarjeta">
        <form className="auditoria-filtros" onSubmit={alEnviar} noValidate>
          <label className="campo">
            <span>Administrador</span>
            <select value={filtros.adminId} onChange={(e) => cambiarFiltro('adminId', e.target.value)}>
              <option value="">Todos</option>
              {administradores.map((admin) => (
                <option key={admin.id} value={admin.id}>
                  {admin.nombres} {admin.apellidos}
                </option>
              ))}
            </select>
          </label>

          <label className={errorFiltros ? 'campo campo-con-error' : 'campo'}>
            <span>Desde</span>
            <input type="date" value={filtros.desde} onChange={(e) => cambiarFiltro('desde', e.target.value)} />
          </label>

          <label className={errorFiltros ? 'campo campo-con-error' : 'campo'}>
            <span>Hasta</span>
            <input type="date" value={filtros.hasta} onChange={(e) => cambiarFiltro('hasta', e.target.value)} />
          </label>

          <div className="auditoria-filtros-botones">
            <button type="submit" className="boton-primario" disabled={cargando}>
              Aplicar filtros
            </button>
            <button type="button" className="boton-secundario" onClick={limpiarFiltros} disabled={cargando}>
              Limpiar
            </button>
          </div>
        </form>

        {errorFiltros && <p className="campo-error auditoria-error-filtros">{errorFiltros}</p>}
      </section>

      <section className="tarjeta">
        <div className="auditoria-titulo">
          <h2>Historial de acciones</h2>
          {!cargando && !error && <span className="auditoria-contador">{registros.length}</span>}
        </div>

        {cargando && <p className="auditoria-vacia">Cargando historial...</p>}

        {!cargando && !error && registros.length === 0 && (
          <p className="auditoria-vacia">
            {hayFiltros ? 'Ninguna acción coincide con los filtros.' : 'Todavía no hay acciones registradas.'}
          </p>
        )}

        {!cargando && registros.length > 0 && (
          <table className="tabla-auditoria">
            <thead>
              <tr>
                <th>Fecha</th>
                <th>Administrador</th>
                <th>Acción</th>
                <th>Sobre</th>
                <th>Detalle</th>
              </tr>
            </thead>
            <tbody>
              {registros.map((registro) => (
                <tr key={registro.id}>
                  <td className="auditoria-fecha">{formatearFecha(registro.fecha)}</td>
                  <td>
                    <span className="auditoria-admin">{registro.admin.nombre}</span>
                    <small className="auditoria-correo">{registro.admin.correo}</small>
                  </td>
                  <td>
                    <span className="auditoria-accion">{NOMBRES_ACCION[registro.accion] ?? registro.accion}</span>
                  </td>
                  <td>
                    {NOMBRES_ENTIDAD[registro.entidad] ?? registro.entidad}
                    {registro.entidad_id != null && ` #${registro.entidad_id}`}
                  </td>
                  <td>{registro.detalle ?? '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </main>
  )
}
