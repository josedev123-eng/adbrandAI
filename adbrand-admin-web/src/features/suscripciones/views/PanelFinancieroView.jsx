// View: panel financiero del dueño de la aplicación (HU-05). La lógica está en usePanelFinanciero.
import { usePanelFinanciero } from '../viewmodels/usePanelFinanciero'
import './PanelFinancieroView.css'

export default function PanelFinancieroView() {
  const { suscripcionesActivas, cargando, error, recargar } = usePanelFinanciero()

  return (
    <main className="pagina pagina-panel">
      <header className="pagina-cabecera panel-cabecera">
        <div>
          <h1>Panel financiero</h1>
          <p>Cuántas PYMEs usan el servicio y cómo va el negocio.</p>
        </div>
        <button type="button" className="boton-secundario" onClick={recargar} disabled={cargando}>
          Actualizar
        </button>
      </header>

      {error && <div className="aviso aviso-error">{error}</div>}

      <div className="panel-indicadores">
        <section className="tarjeta panel-indicador">
          <h2>Suscripciones activas</h2>
          <p className="panel-numero">
            {cargando && '…'}
            {!cargando && error && '—'}
            {!cargando && !error && suscripcionesActivas}
          </p>
          <p className="panel-nota">PYMEs con suscripción activa y vigente hoy.</p>
        </section>
      </div>
    </main>
  )
}
