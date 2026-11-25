// View: "Mis anuncios". El dueño ve el estado de cada anuncio y lo que decidió el moderador. Lógica: useMisAnuncios.
import Cargando from '../../../shared/components/Cargando'
import SinResultados from '../../../shared/components/SinResultados'
import { ESTADOS } from '../models/misAnunciosApi'
import { FILTROS, useMisAnuncios } from '../viewmodels/useMisAnuncios'
import ListoParaPublicar from '../../contenido/views/ListoParaPublicar'
import './MisAnunciosView.css'

const REDES = { INSTAGRAM: 'Instagram', FACEBOOK: 'Facebook', WHATSAPP: 'WhatsApp', TIKTOK: 'TikTok' }
const FECHA = new Intl.DateTimeFormat('es-PE', { day: 'numeric', month: 'short', year: 'numeric' })

function TarjetaAnuncio({ anuncio }) {
  const estado = ESTADOS[anuncio.estado] ?? { nombre: anuncio.estado, clase: '' }
  return (
    <article
      className={anuncio.avisoPendiente ? 'tarjeta mi-anuncio mi-anuncio-nuevo' : 'tarjeta mi-anuncio'}
      aria-label={`Anuncio: ${anuncio.oferta}`}
    >
      <div className="mi-anuncio-cabecera">
        <span className={`mi-anuncio-estado ${estado.clase}`}>{estado.nombre}</span>
        {anuncio.avisoPendiente && <span className="mi-anuncio-novedad">Nuevo</span>}
        <span className="mi-anuncio-fecha">
          {REDES[anuncio.redSocial] ?? anuncio.redSocial}
          {anuncio.fechaCreacion && ` · ${FECHA.format(new Date(anuncio.fechaCreacion))}`}
        </span>
      </div>
      <h2 className="mi-anuncio-oferta">{anuncio.oferta}</h2>
      <p className="mi-anuncio-texto">{anuncio.texto}</p>

      {anuncio.estado === 'RECHAZADO' && (
        <p className="mi-anuncio-motivo">
          <strong>{anuncio.moderado ? 'El moderador no lo aprobó.' : 'No pasó el filtro automático.'}</strong>{' '}
          {anuncio.motivo && `Motivo: ${anuncio.motivo}`} Puedes crear una versión nueva en «Crear anuncio».
        </p>
      )}
      {anuncio.estado === 'DUDOSO' && (
        <p className="mi-anuncio-motivo mi-anuncio-motivo-revision">
          Un moderador lo está revisando. Te avisaremos por correo cuando decida.
        </p>
      )}
      {anuncio.estado === 'APROBADO' && anuncio.moderado && (
        <p className="mi-anuncio-motivo mi-anuncio-motivo-aprobado">Un moderador lo revisó y lo aprobó: ya puedes publicarlo.</p>
      )}
      {anuncio.estado === 'APROBADO' && <ListoParaPublicar anuncioId={anuncio.id} />}
    </article>
  )
}

export default function MisAnunciosView({ alVerAvisos, irACrear }) {
  const { anuncios, cantidades, cargando, error, filtro, setFiltro } = useMisAnuncios(alVerAvisos)

  return (
    <main className="pagina pagina-mis-anuncios">
      <header className="pagina-cabecera">
        <h1>Mis anuncios</h1>
        <p>El estado de cada anuncio que creaste y, si un moderador lo revisó, qué decidió.</p>
      </header>

      {error && (
        <div className="aviso aviso-error" role="alert">
          {error}
        </div>
      )}
      {cargando && <Cargando texto="Cargando tus anuncios..." />}

      {!cargando && !error && (
        <>
          <div className="mis-anuncios-filtros" role="group" aria-label="Filtrar por estado">
            {FILTROS.map(({ valor, nombre }) => (
              <button
                key={valor}
                type="button"
                className={filtro === valor ? 'filtro filtro-activo' : 'filtro'}
                aria-pressed={filtro === valor}
                onClick={() => setFiltro(valor)}
              >
                {nombre} ({cantidades[valor]})
              </button>
            ))}
          </div>

          {anuncios.length === 0 ? (
            <SinResultados>
              {cantidades.TODOS === 0 ? 'Todavía no creaste anuncios.' : 'No hay anuncios con este estado.'}
              {cantidades.TODOS === 0 && irACrear && (
                <button type="button" className="boton-primario" onClick={irACrear}>
                  Crear mi primer anuncio
                </button>
              )}
            </SinResultados>
          ) : (
            <div className="mis-anuncios-lista">
              {anuncios.map((anuncio) => (
                <TarjetaAnuncio key={anuncio.id} anuncio={anuncio} />
              ))}
            </div>
          )}
        </>
      )}
    </main>
  )
}
