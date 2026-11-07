// View: panel del administrador técnico con los tokens de IA que gasta cada empresa (HU-17).
// La lógica (pedir los datos y refrescar cada 30 segundos) está en useConsumoTokens.
// HU-19: si una empresa llega al porcentaje de aviso de su límite mensual, aparece una alerta arriba de la tabla.
import { formatearFecha, formatearMes, formatearPorcentaje, formatearTokens } from '../models/tokensApi'
import { useConsumoTokens } from '../viewmodels/useConsumoTokens'
import './ConsumoTokensView.css'
import Cargando from '../../../shared/components/Cargando'
import SinResultados from '../../../shared/components/SinResultados'

function AlertasConsumo({ alertas }) {
  if (!alertas?.length) return null
  // Si alguna ya pasó el 100 % se pinta como error; si no, como advertencia.
  const excedida = alertas.some((alerta) => alerta.porcentaje >= 100)
  return (
    <div className={`consumo-alertas ${excedida ? 'consumo-alertas-excedida' : ''}`} role="alert">
      <strong>Consumo cerca del límite mensual</strong>
      <ul>
        {alertas.map((alerta) => (
          <li key={alerta.usuario_id} className={alerta.porcentaje >= 100 ? 'consumo-alerta-excedida' : undefined}>
            {alerta.empresa} consumió el {formatearPorcentaje(alerta.porcentaje)}% de su límite mensual (
            {formatearTokens(alerta.tokens)} de {formatearTokens(alerta.limite)} tokens)
          </li>
        ))}
      </ul>
    </div>
  )
}

// Gráfico de barras de tokens por empresa, de la que más gastó a la que menos.
// Es solo dibujo (la tabla de abajo tiene los mismos datos), por eso los textos van en atributos y no se repiten.
function GraficoEmpresas({ empresas }) {
  const ordenadas = [...empresas].sort((a, b) => b.tokens - a.tokens)
  const mayor = Math.max(1, ...ordenadas.map((fila) => fila.tokens))
  return (
    <div className="consumo-grafico" aria-hidden="true">
      {ordenadas.map((fila) => (
        <div
          key={fila.usuario_id}
          className={fila.alerta ? 'consumo-barra consumo-barra-alerta' : 'consumo-barra'}
          data-empresa={fila.empresa}
          data-tokens={formatearTokens(fila.tokens)}
        >
          <span className="consumo-barra-relleno" style={{ width: `${Math.max(Math.round((fila.tokens / mayor) * 100), 2)}%` }} />
        </div>
      ))}
    </div>
  )
}

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

      {cargando && <Cargando texto="Cargando consumo..." />}

      {consumo && (
        <>
          <section className="tarjeta consumo-total">
            <span className="consumo-total-etiqueta">Total de {formatearMes(consumo.mes)}</span>
            <strong className="consumo-total-valor">{formatearTokens(consumo.total)}</strong>
            <span className="consumo-total-etiqueta">tokens</span>
          </section>

          <AlertasConsumo alertas={consumo.alertas} />

          <section className="tarjeta">
            <h2>Por empresa</h2>

            {consumo.empresas.length === 0 ? (
              <SinResultados>Ninguna empresa ha usado la IA este mes.</SinResultados>
            ) : (
              <>
                <GraficoEmpresas empresas={consumo.empresas} />
                <table className="tabla-consumo">
                  <thead>
                    <tr>
                      <th>Empresa</th>
                      <th className="consumo-numero">Tokens</th>
                      <th className="consumo-numero">% del límite</th>
                      <th className="consumo-numero">Generaciones</th>
                      <th>Última generación</th>
                    </tr>
                  </thead>
                  <tbody>
                    {consumo.empresas.map((fila) => (
                      <tr key={fila.usuario_id} className={fila.alerta ? 'consumo-fila-alerta' : undefined}>
                        <td className="consumo-empresa">{fila.empresa}</td>
                        <td className="consumo-numero">{formatearTokens(fila.tokens)}</td>
                        <td className="consumo-numero">
                          {fila.porcentaje == null ? '—' : `${formatearPorcentaje(fila.porcentaje)}%`}
                        </td>
                        <td className="consumo-numero">{fila.generaciones}</td>
                        <td className="consumo-fecha">{formatearFecha(fila.ultima_generacion)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </>
            )}
          </section>
        </>
      )}
    </main>
  )
}
