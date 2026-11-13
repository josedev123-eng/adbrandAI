// View: pantalla de Inicio de la PYME con tarjetas resumen. La lógica está en useInicio.
// Cada tarjeta lleva a su pantalla con "alAbrir".
import { useInicio } from '../viewmodels/useInicio'
import './InicioView.css'

const REDES = { INSTAGRAM: 'Instagram', FACEBOOK: 'Facebook' }
const FORMATO_DIA = new Intl.DateTimeFormat('es-PE', { weekday: 'short', day: 'numeric', month: 'short' })

function formatearDia(fecha) {
  const [anio, mes, dia] = fecha.split('-').map(Number)
  return FORMATO_DIA.format(new Date(anio, mes - 1, dia))
}

function Tarjeta({ titulo, estado, accion, alAbrir, children }) {
  return (
    <section className="tarjeta inicio-tarjeta" aria-label={titulo}>
      <div className="inicio-tarjeta-cabecera">
        <h2>{titulo}</h2>
        {accion && (
          <button type="button" className="inicio-enlace" onClick={alAbrir}>
            {accion} →
          </button>
        )}
      </div>
      {estado.cargando && (
        <div className="inicio-cargando" aria-hidden="true">
          <span />
          <span />
        </div>
      )}
      {estado.error && <p className="inicio-vacio">No se pudo cargar. Intenta con "Actualizar".</p>}
      {!estado.cargando && !estado.error && children}
    </section>
  )
}

export default function InicioView({ empresa, alAbrir }) {
  const { tarjetas, recargar } = useInicio()
  const { perfil, anuncios, kit, calendario } = tarjetas
  const faltaPerfil = !perfil.cargando && !perfil.error && !perfil.datos

  return (
    <main className="pagina pagina-inicio">
      <header className="inicio-bienvenida">
        <div>
          <span className="inicio-kicker">TU PANEL</span>
          <h1>Hola, {empresa ?? 'bienvenido'}</h1>
          <p>Así va tu marca hoy. Crea un anuncio nuevo o revisa lo que tienes programado.</p>
        </div>
        <div className="inicio-bienvenida-acciones">
          <button type="button" className="boton-ia" onClick={() => alAbrir('anuncio')}>
            ✦ Crear anuncio
          </button>
          <button type="button" className="boton-secundario" onClick={recargar}>
            Actualizar
          </button>
        </div>
      </header>

      {faltaPerfil && (
        <div className="aviso aviso-con-accion inicio-aviso">
          <span>Completa el perfil de tu negocio para que la IA escriba con tu tono y tu rubro.</span>
          <button type="button" className="boton-secundario" onClick={() => alAbrir('perfil')}>
            Completar mi perfil
          </button>
        </div>
      )}

      <div className="inicio-tarjetas">
        <Tarjeta titulo="Tus anuncios" estado={anuncios} accion="Ver informes" alAbrir={() => alAbrir('informes')}>
          {anuncios.datos && (
            <>
              <div className="inicio-cifras">
                <div>
                  <strong>{anuncios.datos.total}</strong>
                  <span>creados</span>
                </div>
                <div>
                  <strong className="inicio-ok">{anuncios.datos.aprobados}</strong>
                  <span>aprobados</span>
                </div>
                <div>
                  <strong className="inicio-atencion">{anuncios.datos.enRevision}</strong>
                  <span>en revisión</span>
                </div>
              </div>
              {anuncios.datos.ultimo ? (
                <p className="inicio-nota">
                  Último: <b>{anuncios.datos.ultimo.oferta}</b> ({REDES[anuncios.datos.ultimo.redSocial] ?? anuncios.datos.ultimo.redSocial})
                </p>
              ) : (
                <p className="inicio-vacio">Todavía no creaste anuncios.</p>
              )}
            </>
          )}
        </Tarjeta>

        <Tarjeta titulo="Tu kit de marca" estado={kit} accion={kit.datos ? 'Editar kit' : 'Crear kit'} alAbrir={() => alAbrir('kit')}>
          {kit.datos ? (
            <>
              <div className="inicio-colores" aria-label="Colores de tu marca">
                {kit.datos.colores.map((color) => (
                  <span key={color.hex} className="inicio-color" style={{ backgroundColor: color.hex }} title={`${color.nombre ?? ''} ${color.hex}`} />
                ))}
              </div>
              <p className="inicio-nota">{kit.datos.nombre}</p>
            </>
          ) : (
            <p className="inicio-vacio">Aún no tienes kit de marca. La IA te propone logo, colores y tipografías.</p>
          )}
        </Tarjeta>

        <Tarjeta titulo="Próximas publicaciones" estado={calendario} accion="Ir al calendario" alAbrir={() => alAbrir('calendario')}>
          {calendario.datos?.length ? (
            <ul className="inicio-publicaciones">
              {calendario.datos.map((publicacion) => (
                <li key={publicacion.id}>
                  <span
                    className="inicio-punto"
                    style={{ backgroundColor: publicacion.campanaColor ?? 'var(--primary)' }}
                    aria-hidden="true"
                  />
                  <span className="inicio-publicacion-dia">{formatearDia(publicacion.fecha)}</span>
                  <span className="inicio-publicacion-titulo">{publicacion.titulo}</span>
                  <span className="inicio-publicacion-red">{REDES[publicacion.redSocial] ?? publicacion.redSocial}</span>
                </li>
              ))}
            </ul>
          ) : (
            <p className="inicio-vacio">No tienes publicaciones programadas. Programa la primera en el calendario.</p>
          )}
        </Tarjeta>

        <Tarjeta titulo="Tu plan" estado={{ cargando: false, error: false }} accion="Ver planes" alAbrir={() => alAbrir('planes')}>
          <p className="inicio-nota">Revisa tu plan, paga con tarjeta o Yape y descarga tus comprobantes.</p>
        </Tarjeta>
      </div>
    </main>
  )
}
