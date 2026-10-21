// View: panel del administrador técnico con los tokens de IA que gasta cada empresa (HU-17).
// La lógica (pedir los datos y refrescar cada 30 segundos) está en useConsumoTokens.
import { formatearFecha, formatearMes, formatearTokens } from '../models/tokensApi'
import { useConsumoTokens } from '../viewmodels/useConsumoTokens'
import './ConsumoTokensView.css'

export default function ConsumoTokensView() {
  const { consumo, cargando, error, actualizando, actualizar } = useConsumoTokens()

  return (
    <main className="pagina pagina-consumo">
      <header className="pagina-cabecera consumo-cabecera">
        <div>
          <h1>Consumo de IA</h1>
          <p>Tokens que gastó cada empresa en el mes. Se actualiza solo cada 30 segundos.</p>
        </div>
        <button type="button" className="boton-secundario" onClick={actualizar} disabled={actualizando}>
          {actualizando ? 'Actualizando…' : 'Actualizar'}
        </button>
      </header>

      {error && <div className="aviso aviso-error">{error}</div>}

      {cargando && <p className="consumo-vacio">Cargando consumo...</p>}

      {consumo && (
        <>
          <section className="tarjeta consumo-total">
            <span className="consumo-total-etiqueta">Total de {formatearMes(consumo.mes)}</span>
            <strong className="consumo-total-valor">{formatearTokens(consumo.total)}</strong>
            <span className="consumo-total-etiqueta">tokens</span>
          </section>

          <section className="tarjeta">
            <h2>Por empresa</h2>

            {consumo.empresas.length === 0 ? (
              <p className="consumo-vacio">Ninguna empresa ha usado la IA este mes.</p>
            ) : (
              <table className="tabla-consumo">
                <thead>
                  <tr>
                    <th>Empresa</th>
                    <th className="consumo-numero">Tokens</th>
                    <th className="consumo-numero">Generaciones</th>
                    <th>Última generación</th>
                  </tr>
                </thead>
                <tbody>
                  {consumo.empresas.map((fila) => (
                    <tr key={fila.usuario_id}>
                      <td className="consumo-empresa">{fila.empresa}</td>
                      <td className="consumo-numero">{formatearTokens(fila.tokens)}</td>
                      <td className="consumo-numero">{fila.generaciones}</td>
                      <td className="consumo-fecha">{formatearFecha(fila.ultima_generacion)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </section>
        </>
      )}
    </main>
  )
}
