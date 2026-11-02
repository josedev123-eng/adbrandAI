// View: informe consolidado de las campañas de la empresa (HU-37). La lógica está en useInformeCampanas.
// HU-38: botón para descargar el mismo informe en PDF (solo si hay campañas).
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
  const { informe, error, cargando, sinCampanas, descargando, descargarPdf } = useInformeCampanas()

  if (cargando) {
    return <main className="pagina"><p className="cargando">Cargando tu informe...</p></main>
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

      {error && <div className="aviso aviso-error">{error}</div>}

      {sinCampanas && (
        <div className="aviso aviso-info" role="status">
          Todavía no tienes campañas. Genera tu primer anuncio para ver aquí tu informe.
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
