// View: bandeja de contenidos marcados como dudosos. La lógica está en useBandejaRevision.
import { formatearFecha, NOMBRES_RED } from '../models/moderacionApi'
import { useBandejaRevision } from '../viewmodels/useBandejaRevision'
import './BandejaRevisionView.css'

export default function BandejaRevisionView() {
  const { contenidos, cargando, error, recargar } = useBandejaRevision()

  return (
    <main className="pagina pagina-bandeja">
      <header className="pagina-cabecera bandeja-cabecera">
        <div>
          <h1>Revisión de contenido</h1>
          <p>Contenidos que el filtro automático marcó como dudosos. No se publican hasta que alguien los revise.</p>
        </div>
        <button type="button" className="boton-secundario" onClick={recargar} disabled={cargando}>
          Actualizar
        </button>
      </header>

      {error && <div className="aviso aviso-error">{error}</div>}

      <section className="tarjeta bandeja">
        <div className="bandeja-titulo">
          <h2>Pendientes de revisión</h2>
          {!cargando && !error && <span className="contador">{contenidos.length}</span>}
        </div>

        {cargando && <p className="bandeja-vacia">Cargando contenidos...</p>}

        {!cargando && !error && contenidos.length === 0 && (
          <p className="bandeja-vacia">No hay contenidos pendientes. Todo lo generado pasó el filtro automático.</p>
        )}

        {!cargando && contenidos.length > 0 && (
          <ul className="bandeja-lista">
            {contenidos.map((contenido) => (
              <li key={contenido.id} className="bandeja-fila">
                <div className="fila-datos">
                  <span className="etiqueta-dudoso">Dudoso</span>
                  <span>{NOMBRES_RED[contenido.red_social] ?? contenido.red_social}</span>
                  <span>{formatearFecha(contenido.fecha_creacion)}</span>
                </div>
                <p className="fila-oferta">{contenido.oferta}</p>
                <p className="fila-extracto">{contenido.extracto}</p>
                <p className="fila-motivo">{contenido.motivo_revision}</p>
              </li>
            ))}
          </ul>
        )}
      </section>
    </main>
  )
}
