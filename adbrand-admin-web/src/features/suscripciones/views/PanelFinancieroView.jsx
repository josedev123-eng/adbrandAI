// View: panel financiero del dueño de la aplicación (HU-05, HU-06, HU-07). La lógica está en usePanelFinanciero.
import { formatearMes, formatearSoles } from '../models/suscripcionesApi'
import { usePanelFinanciero } from '../viewmodels/usePanelFinanciero'
import './PanelFinancieroView.css'

export default function PanelFinancieroView() {
  const {
    suscripcionesActivas,
    ingresos,
    totalIngresos,
    cargando,
    error,
    recargar,
    rango,
    hayRango,
    errorRango,
    cambiarRango,
    aplicarRango,
    quitarRango,
  } = usePanelFinanciero()

  function alEnviar(evento) {
    evento.preventDefault()
    aplicarRango()
  }

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

        {/* HU-06: total cobrado en el periodo que muestran las barras */}
        <section className="tarjeta panel-indicador">
          <h2>Ingresos del periodo</h2>
          <p className="panel-numero">
            {cargando && '…'}
            {!cargando && error && '—'}
            {!cargando && !error && formatearSoles(totalIngresos)}
          </p>
          <p className="panel-nota">
            {hayRango ? 'Pagos recibidos en el rango elegido.' : 'Pagos recibidos en los últimos 6 meses.'}
          </p>
        </section>
      </div>

      {/* HU-06: ingresos generados por mes */}
      <section className="tarjeta">
        <h2>Ingresos por mes</h2>

        {/* HU-07: rango de meses para comparar periodos */}
        <form className="panel-rango" onSubmit={alEnviar} noValidate>
          <label className={errorRango ? 'campo campo-con-error' : 'campo'}>
            <span>Desde</span>
            <input
              type="month"
              placeholder="AAAA-MM"
              value={rango.desde}
              onChange={(e) => cambiarRango('desde', e.target.value)}
            />
          </label>
          <label className={errorRango ? 'campo campo-con-error' : 'campo'}>
            <span>Hasta</span>
            <input
              type="month"
              placeholder="AAAA-MM"
              value={rango.hasta}
              onChange={(e) => cambiarRango('hasta', e.target.value)}
            />
          </label>
          <div className="panel-rango-botones">
            <button type="submit" className="boton-primario" disabled={cargando}>
              Aplicar rango
            </button>
            <button type="button" className="boton-secundario" onClick={quitarRango} disabled={cargando || !hayRango}>
              Últimos 6 meses
            </button>
          </div>
        </form>

        {errorRango && <p className="campo-error panel-rango-error">{errorRango}</p>}

        {cargando && <p className="panel-vacio">Cargando ingresos...</p>}

        {!cargando && !error && (
          <ul className="panel-ingresos">
            {ingresos.map((ingreso) => (
              <li key={ingreso.mes} className="panel-ingreso">
                <span className="panel-ingreso-mes">{formatearMes(ingreso.mes)}</span>
                <span className="panel-ingreso-barra">
                  <span className="panel-ingreso-relleno" style={{ width: `${ingreso.porcentaje}%` }} />
                </span>
                <span className="panel-ingreso-monto">{formatearSoles(ingreso.total)}</span>
              </li>
            ))}
          </ul>
        )}
      </section>
    </main>
  )
}
