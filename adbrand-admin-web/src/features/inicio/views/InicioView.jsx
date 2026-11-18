// View: pantalla de Inicio del administrador con tarjetas resumen. La lógica está en useInicio.
// Cada tarjeta lleva a su pantalla al hacer clic.
import { formatearHora } from '../../servidor/models/servidorApi'
import { formatearMes, formatearSoles } from '../../suscripciones/models/suscripcionesApi'
import { formatearTokens } from '../../tokens/models/tokensApi'
import { pantallaDeTarjeta, useInicio } from '../viewmodels/useInicio'
import './InicioView.css'

const TITULOS = {
  pendientes: 'Pendientes de revisión',
  suscripciones: 'Suscripciones activas',
  ingresos: 'Ingresos del mes',
  tokens: 'Tokens de IA del mes',
  servidor: 'Estado del servidor',
}

function plural(numero, singular, varios) {
  return numero === 1 ? singular : varios
}

// Qué número grande, qué nota y qué color lleva cada tarjeta.
function contenidoDe(clave, datos) {
  switch (clave) {
    case 'pendientes':
      return {
        valor: datos.total,
        nota: datos.total === 0 ? 'Todo al día: no hay contenidos dudosos.' : 'Contenidos dudosos esperando una decisión.',
        tono: datos.total > 0 ? 'atencion' : 'ok',
      }
    case 'suscripciones':
      return { valor: datos.total, nota: 'PYMEs con suscripción vigente hoy.', tono: 'normal' }
    case 'ingresos':
      return {
        valor: formatearSoles(datos.total),
        nota: datos.mes ? `Pagos recibidos en ${formatearMes(datos.mes)}.` : 'Todavía no hay pagos.',
        tono: 'normal',
      }
    case 'tokens':
      return {
        valor: formatearTokens(datos.total),
        nota:
          datos.alertas > 0
            ? `${datos.alertas} ${plural(datos.alertas, 'empresa está', 'empresas están')} cerca de su límite.`
            : `Usados por ${datos.empresas} ${plural(datos.empresas, 'empresa', 'empresas')}.`,
        tono: datos.alertas > 0 ? 'atencion' : 'normal',
      }
    case 'servidor': {
      const caidos = datos.caidos.length
      return {
        valor: caidos === 0 ? 'Todo activo' : `${caidos} ${plural(caidos, 'caído', 'caídos')}`,
        nota:
          caidos === 0
            ? `Revisado a las ${datos.verificadoEn ? formatearHora(datos.verificadoEn) : '—'}.`
            : `Revisa: ${datos.caidos.join(', ')}.`,
        tono: caidos === 0 ? 'ok' : 'peligro',
      }
    }
    default:
      return null
  }
}

function Tarjeta({ clave, estado, alAbrir }) {
  const contenido = estado.datos ? contenidoDe(clave, estado.datos) : null
  const tono = contenido?.tono ?? (estado.error ? 'peligro' : 'normal')
  let nota = 'Cargando…'
  if (estado.error) nota = 'No se pudo cargar. Revisa que Django esté encendido.'
  else if (contenido) nota = contenido.nota

  return (
    <button
      type="button"
      className={`tarjeta inicio-tarjeta inicio-${tono}`}
      onClick={() => alAbrir(pantallaDeTarjeta(clave))}
    >
      <span className="inicio-titulo">{TITULOS[clave]}</span>
      {estado.cargando && <span className="inicio-valor inicio-valor-cargando" aria-hidden="true" />}
      {estado.error && <span className="inicio-valor">—</span>}
      {contenido && <span className="inicio-valor">{contenido.valor}</span>}
      <span className="inicio-nota">{nota}</span>
      <span className="inicio-ir" aria-hidden="true">
        Ver detalle →
      </span>
    </button>
  )
}

export default function InicioView({ admin, claves, alAbrir }) {
  const { tarjetas, recargar } = useInicio(claves)

  return (
    <main className="pagina pagina-inicio">
      <header className="pagina-cabecera inicio-cabecera">
        <div>
          <h1>Hola, {admin.nombres}</h1>
          <p>Así va AdBrand.AI hoy. Haz clic en una tarjeta para ver el detalle.</p>
        </div>
        <button type="button" className="boton-secundario" onClick={recargar}>
          Actualizar
        </button>
      </header>

      <section className="inicio-tarjetas">
        {claves.map((clave) => (
          <Tarjeta key={clave} clave={clave} estado={tarjetas[clave]} alAbrir={alAbrir} />
        ))}
      </section>
    </main>
  )
}
