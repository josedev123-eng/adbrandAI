// View: "Tu plan y tu uso este mes" en Mi Cuenta. Barras de progreso como en ChatGPT o Claude, en cosas que el dueño entiende:
// la IA del plan (y cuántos anuncios más le alcanzan), los anuncios que creó y los retoques de imagen.
import { anunciosAprox, NUMERO } from '../models/formatoPlanes'
import { useUsoDelMes } from '../viewmodels/useUsoDelMes'
import './UsoDelMes.css'

function Barra({ titulo, valor, maximo, resumen, detalle }) {
  const porcentaje = maximo > 0 ? Math.min(100, Math.round((valor * 100) / maximo)) : 0
  const clase = porcentaje >= 100 ? 'uso-barra uso-barra-agotada' : porcentaje >= 80 ? 'uso-barra uso-barra-alerta' : 'uso-barra'
  return (
    <div className={clase}>
      <div className="uso-barra-titulo">
        <strong>{titulo}</strong>
        <span>{resumen}</span>
      </div>
      <div
        className="uso-barra-fondo"
        role="progressbar"
        aria-label={titulo}
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuenow={porcentaje}
      >
        <div className="uso-barra-relleno" style={{ width: `${porcentaje}%` }} />
      </div>
      {detalle && <p className="uso-barra-detalle">{detalle}</p>}
    </div>
  )
}

function nombrePlan(consumo) {
  if (consumo.origen === 'PLAN' && consumo.plan) return consumo.plan
  if (consumo.origen === 'GRATUITO') return 'Gratis'
  return 'Personalizado'
}

// "alVerPlanes": lleva a Planes para mejorar o cambiar el plan.
export default function UsoDelMes({ alVerPlanes }) {
  const uso = useUsoDelMes()
  if (!uso) return null
  const { consumo, anunciosCreados, retoquesUsados, limiteRetoques } = uso
  const restantes = Math.max(0, consumo.limite - consumo.tokensUsados)
  const anunciosRestantes = restantes > 0 ? anunciosAprox(restantes) : 0

  return (
    <section className="tarjeta uso-mes" aria-label="Tu plan y tu uso este mes">
      <div className="uso-mes-cabecera">
        <div>
          <h2>Tu plan y tu uso este mes</h2>
          <p className="uso-mes-plan">
            Plan <b>{nombrePlan(consumo)}</b> · {NUMERO.format(consumo.limite)} tokens de IA al mes
          </p>
        </div>
        {alVerPlanes && (
          <button type="button" className="boton-secundario" onClick={alVerPlanes}>
            Cambiar de plan
          </button>
        )}
      </div>
      <div className="uso-mes-filas">
        <Barra
          titulo="IA del plan"
          valor={consumo.tokensUsados}
          maximo={consumo.limite}
          resumen={`${consumo.porcentaje} % usado`}
          detalle={
            consumo.agotado
              ? 'Se acabó este mes. Se reinicia el día 1.'
              : `Te alcanza para ≈ ${NUMERO.format(anunciosRestantes)} anuncios más`
          }
        />
        <Barra
          titulo="Retoques de imagen"
          valor={retoquesUsados}
          maximo={limiteRetoques}
          resumen={`${NUMERO.format(retoquesUsados)} de ${NUMERO.format(limiteRetoques)}`}
        />
        <div className="uso-barra">
          <div className="uso-barra-titulo">
            <strong>Anuncios creados</strong>
            <span>
              <b>{NUMERO.format(anunciosCreados)}</b> este mes
            </span>
          </div>
        </div>
      </div>
      <small>
        {NUMERO.format(consumo.tokensUsados)} de {NUMERO.format(consumo.limite)} tokens de IA. Se reinicia el día 1 de cada mes.
      </small>
    </section>
  )
}
