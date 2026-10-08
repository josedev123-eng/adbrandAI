// View: tabla de suscripciones con filtro por estado (HU-08).
import { ESTADOS_CON_DEUDA, ESTADOS_SUSCRIPCION } from '../models/suscripcionesApi'
import { useSuscripciones } from '../viewmodels/useSuscripciones'
import './SuscripcionesView.css'

function BadgeEstado({ estado }) {
  const cls = `badge-estado ${estado.toLowerCase().replace('_', '-')}`
  return <span className={cls}>{ESTADOS_SUSCRIPCION[estado] ?? estado}</span>
}

export default function SuscripcionesView() {
  const { suscripciones, cargando, error, filtroEstado, cambiarFiltroEstado } = useSuscripciones()

  return (
    <main className="pagina pagina-suscripciones">
      <header className="pagina-cabecera">
        <h1>Suscripciones</h1>
        <p>Listado y estado de pagos de cada cliente.</p>
      </header>

      {error && <div className="aviso aviso-error">{error}</div>}

      <section className="tarjeta">
        <div className="filtros">
          <label>
            Filtrar por estado:
            <select value={filtroEstado} onChange={e => cambiarFiltroEstado(e.target.value)}>
              <option value="">Todos</option>
              {ESTADOS_CON_DEUDA.map(estado => (
                <option key={estado} value={estado}>{ESTADOS_SUSCRIPCION[estado]}</option>
              ))}
            </select>
          </label>
        </div>

        {cargando && <p className="lista-vacia">Cargando suscripciones...</p>}

        {!cargando && suscripciones.length === 0 && (
          <p className="lista-vacia">No hay suscripciones con deuda pendiente.</p>
        )}

        {!cargando && suscripciones.length > 0 && (
          <table className="tabla-suscripciones">
            <thead>
              <tr>
                <th>ID</th>
                <th>Cliente</th>
                <th>Plan</th>
                <th>Estado</th>
                <th>Vencimiento</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {suscripciones.map(s => (
                <tr key={s.id}>
                  <td>#{s.id}</td>
                  <td>{s.cliente || '—'}</td>
                  <td>{s.plan || '—'}</td>
                  <td><BadgeEstado estado={s.estado} /></td>
                  <td>{s.fecha_vencimiento ? new Date(s.fecha_vencimiento).toLocaleDateString('es-PE') : '—'}</td>
                  <td>
                    <button type="button" className="boton-secundario boton-pequeno" disabled>Ver detalle</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </main>
  )
}