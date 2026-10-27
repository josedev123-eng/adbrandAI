// View: historial de contenidos aprobados y rechazados con moderador y motivo (HU-16).
// La lógica (filtros, páginas y validación de fechas) está en useHistorialContenidos.
import { Fragment } from 'react'
import { formatearFechaHora, NOMBRES_ESTADO } from '../models/moderacionApi'
import { useHistorialContenidos } from '../viewmodels/useHistorialContenidos'
import './HistorialContenidosView.css'

export default function HistorialContenidosView() {
  const { filtros, historial, aviso, error, cargando, abiertoId, cambiarFiltro, filtrar, irAPagina, alternarTexto } =
    useHistorialContenidos()

  return (
    <main className="pagina pagina-historial-contenidos">
      <header className="pagina-cabecera">
        <h1>Historial de contenidos</h1>
        <p>Contenidos aprobados y rechazados, con quién lo decidió y por qué. Haz clic en una fila para ver el texto.</p>
      </header>

      <section className="tarjeta">
        <form className="historial-contenidos-filtros" onSubmit={filtrar} noValidate>
          <label className="campo">
            <span>Estado</span>
            <select value={filtros.estado} onChange={(e) => cambiarFiltro('estado', e.target.value)}>
              <option value="">Todos</option>
              <option value="APROBADO">Aprobado</option>
              <option value="RECHAZADO">Rechazado</option>
            </select>
          </label>
          <label className={aviso ? 'campo campo-con-error' : 'campo'}>
            <span>Desde</span>
            <input type="date" value={filtros.desde} onChange={(e) => cambiarFiltro('desde', e.target.value)} />
          </label>
          <label className={aviso ? 'campo campo-con-error' : 'campo'}>
            <span>Hasta</span>
            <input type="date" value={filtros.hasta} onChange={(e) => cambiarFiltro('hasta', e.target.value)} />
          </label>
          <button type="submit" className="boton-primario" disabled={cargando}>
            Filtrar
          </button>
        </form>

        {aviso && (
          <p className="historial-contenidos-aviso" role="alert">
            {aviso}
          </p>
        )}
      </section>

      {error && <div className="aviso aviso-error">{error}</div>}

      <section className="tarjeta">
        {cargando && <p className="historial-contenidos-vacio">Cargando historial...</p>}

        {!cargando && historial?.resultados.length === 0 && (
          <p className="historial-contenidos-vacio">No hay contenidos aprobados ni rechazados con esos filtros.</p>
        )}

        {!cargando && historial?.resultados.length > 0 && (
          <>
            <table className="tabla-historial-contenidos">
              <thead>
                <tr>
                  <th>Fecha</th>
                  <th>Empresa</th>
                  <th>Oferta</th>
                  <th>Estado</th>
                  <th>Moderador</th>
                  <th>Motivo</th>
                </tr>
              </thead>
              <tbody>
                {historial.resultados.map((fila) => (
                  <Fragment key={fila.id}>
                    <tr
                      className="historial-contenidos-fila"
                      onClick={() => alternarTexto(fila.id)}
                      aria-expanded={abiertoId === fila.id}
                    >
                      <td className="historial-contenidos-fecha">{formatearFechaHora(fila.fecha_decision)}</td>
                      <td>{fila.empresa}</td>
                      <td>{fila.oferta}</td>
                      <td>
                        <span className={`etiqueta-estado estado-${fila.estado.toLowerCase()}`}>
                          {NOMBRES_ESTADO[fila.estado] ?? fila.estado}
                        </span>
                      </td>
                      <td>{fila.moderador}</td>
                      <td>{fila.motivo || '—'}</td>
                    </tr>
                    {abiertoId === fila.id && (
                      <tr className="historial-contenidos-texto">
                        <td colSpan={6}>{fila.texto}</td>
                      </tr>
                    )}
                  </Fragment>
                ))}
              </tbody>
            </table>

            <nav className="historial-contenidos-paginas" aria-label="Páginas">
              <button
                type="button"
                className="boton-secundario"
                onClick={() => irAPagina(historial.pagina - 1)}
                disabled={historial.pagina <= 1}
              >
                Anterior
              </button>
              <span>
                Página {historial.pagina} de {historial.paginas} · {historial.total} contenidos
              </span>
              <button
                type="button"
                className="boton-secundario"
                onClick={() => irAPagina(historial.pagina + 1)}
                disabled={historial.pagina >= historial.paginas}
              >
                Siguiente
              </button>
            </nav>
          </>
        )}
      </section>
    </main>
  )
}
