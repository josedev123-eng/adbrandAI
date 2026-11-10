// View: informe consolidado de las campañas de la empresa (HU-37). La lógica está en useInformeCampanas.
// HU-38: botón para descargar el mismo informe en PDF (solo si hay campañas).
// HU-40: filtros por periodo y por campaña; el PDF sale con los mismos filtros.
import { formatearFecha, NOMBRES_ESTADO, NOMBRES_RED } from '../models/reportesApi'
import { useInformeCampanas } from '../viewmodels/useInformeCampanas'
import './InformeCampanasView.css'

function Total({ etiqueta, valor, clase }) {
  return (
    <div className={`informe-total ${clase ?? ''}`}>
      <span>{etiqueta}</span>
      <b>{valor}</b>
    </div>
  )
}

export default function InformeCampanasView() {
  const {
    informe,
    error,
    cargando,
    sinCampanas,
    descargando,
    descargarPdf,
    filtros,
    cambiarFiltro,
    aplicarFiltros,
    quitarFiltros,
    conFiltros,
    actualizando,
    campanas,
  } = useInformeCampanas()

  if (cargando) {
    return <main className="pagina"><p className="cargando">Cargando tu informe...</p></main>
  }

  function alAplicar(evento) {
    evento.preventDefault()
    aplicarFiltros()
  }

  return (
    <main className="pagina pagina-informe">
      <header className="pagina-cabecera">
        <h1>Informes</h1>
        <p>El registro de todas las campañas que generaste, para guardarlo o presentarlo.</p>
        {informe && !sinCampanas && (
          <div className="informe-descargas">
            <button type="button" className="boton-primario" onClick={descargarPdf} disabled={descargando}>
              {descargando ? 'Preparando PDF...' : '📄 Descargar PDF'}
            </button>
          </div>
        )}
      </header>

      <form className="tarjeta informe-filtros" onSubmit={alAplicar} aria-label="Filtros del informe">
        <label className="campo">
          <span>Desde</span>
          <input type="date" value={filtros.desde} onChange={(evento) => cambiarFiltro('desde', evento.target.value)} />
        </label>
        <label className="campo">
          <span>Hasta</span>
          <input type="date" value={filtros.hasta} onChange={(evento) => cambiarFiltro('hasta', evento.target.value)} />
        </label>
        <label className="campo">
          <span>Campaña</span>
          <select value={filtros.campanaId} onChange={(evento) => cambiarFiltro('campanaId', evento.target.value)}>
            <option value="">Todas las campañas</option>
            {campanas.map((campana) => (
              <option key={campana.id} value={campana.id}>
                {campana.nombre}
              </option>
            ))}
          </select>
        </label>
        <div className="informe-filtros-acciones">
          <button type="submit" className="boton-primario" disabled={actualizando}>
            {actualizando ? 'Aplicando...' : 'Aplicar filtros'}
          </button>
          {conFiltros && (
            <button type="button" className="boton-secundario" onClick={quitarFiltros} disabled={actualizando}>
              Quitar filtros
            </button>
          )}
        </div>
      </form>

      {error && <div className="aviso aviso-error">{error}</div>}

      {sinCampanas && (
        <div className="aviso aviso-info" role="status">
          {conFiltros
            ? 'No hay campañas con esos filtros. Prueba con otro periodo u otra campaña.'
            : 'Todavía no tienes campañas. Genera tu primer anuncio para ver aquí tu informe.'}
        </div>
      )}

      {informe && !sinCampanas && (
        <>
          <section className="tarjeta informe-resumen" aria-label="Resumen">
            <Total etiqueta="Empresa" valor={informe.empresa} clase="informe-total-empresa" />
            <Total etiqueta="Campañas" valor={informe.total} />
            <Total etiqueta="Aprobadas" valor={informe.aprobados} clase="estado-aprobado" />
            <Total etiqueta="En revisión" valor={informe.dudosos} clase="estado-dudoso" />
            <Total etiqueta="Rechazadas" valor={informe.rechazados} clase="estado-rechazado" />
          </section>

          <section className="tarjeta">
            <table className="tabla-informe">
              <thead>
                <tr>
                  <th>Fecha</th>
                  <th>Red social</th>
                  <th>Oferta</th>
                  <th>Estado</th>
                </tr>
              </thead>
              <tbody>
                {informe.campanas.map((campana) => (
                  <tr key={campana.id}>
                    <td className="informe-fecha">{formatearFecha(campana.fecha)}</td>
                    <td>{NOMBRES_RED[campana.redSocial] ?? campana.redSocial}</td>
                    <td>{campana.oferta}</td>
                    <td>
                      <span className={`etiqueta-estado estado-${campana.estado.toLowerCase()}`}>
                        {NOMBRES_ESTADO[campana.estado] ?? campana.estado}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </section>
        </>
      )}
    </main>
  )
}
