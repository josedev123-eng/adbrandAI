// View: historial de consumo de tokens de IA por periodo para proyectar el gasto (HU-20).
// La lógica (validar el periodo y pedir los datos) está en useHistorialTokens.
import { formatearTokens, nombreDelMes } from '../models/tokensApi'
import { useHistorialTokens } from '../viewmodels/useHistorialTokens'
import './HistorialTokensView.css'

export default function HistorialTokensView() {
  const { periodo, historial, aviso, error, cargando, cambiarPeriodo, consultar } = useHistorialTokens()

  return (
    <main className="pagina pagina-historial">
      <header className="pagina-cabecera">
        <h1>Historial de IA</h1>
        <p>Tokens que se consumieron en cada mes del periodo, para proyectar el gasto de los próximos meses.</p>
      </header>

      <section className="tarjeta">
        <form className="historial-filtros" onSubmit={consultar} noValidate>
          <label className={aviso ? 'campo campo-con-error' : 'campo'}>
            <span>Desde</span>
            <input type="month" value={periodo.desde} onChange={(e) => cambiarPeriodo('desde', e.target.value)} />
          </label>
          <label className={aviso ? 'campo campo-con-error' : 'campo'}>
            <span>Hasta</span>
            <input type="month" value={periodo.hasta} onChange={(e) => cambiarPeriodo('hasta', e.target.value)} />
          </label>
          <button type="submit" className="boton-primario" disabled={cargando}>
            {cargando ? 'Consultando…' : 'Consultar'}
          </button>
        </form>

        {aviso && (
          <p className="historial-aviso" role="alert">
            {aviso}
          </p>
        )}
      </section>

      {error && <div className="aviso aviso-error">{error}</div>}

      {historial && (
        <section className="tarjeta">
          <h2>
            {nombreDelMes(historial.desde)} a {nombreDelMes(historial.hasta)}
          </h2>
          <table className="tabla-historial">
            <thead>
              <tr>
                <th>Mes</th>
                <th className="historial-numero">Tokens</th>
                <th className="historial-numero">Generaciones</th>
              </tr>
            </thead>
            <tbody>
              {historial.meses.map((fila) => (
                <tr key={fila.mes}>
                  <td className="historial-mes">{nombreDelMes(fila.mes)}</td>
                  <td className="historial-numero">{formatearTokens(fila.tokens)}</td>
                  <td className="historial-numero">{fila.generaciones}</td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr>
                <th>Total del periodo</th>
                <td className="historial-numero">{formatearTokens(historial.total)}</td>
                <td />
              </tr>
            </tfoot>
          </table>
        </section>
      )}
    </main>
  )
}
