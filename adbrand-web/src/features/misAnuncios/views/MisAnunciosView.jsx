// View: "Mis anuncios". El dueño ve el estado de cada anuncio y lo que decidió el moderador. Lógica: useMisAnuncios.
// HU 63: buscador por texto o fecha, de a 12 con "Ver más", "Cancelar en AdBrand" y, en los cancelados, "Eliminar".
import { useState } from 'react'
import Aviso from '../../../shared/components/Aviso'
import Cargando from '../../../shared/components/Cargando'
import SinResultados from '../../../shared/components/SinResultados'
import { ESTADOS } from '../models/misAnunciosApi'
import { FILTROS, useMisAnuncios } from '../viewmodels/useMisAnuncios'
import ListoParaPublicar from '../../contenido/views/ListoParaPublicar'
import './MisAnunciosView.css'

const REDES = { INSTAGRAM: 'Instagram', FACEBOOK: 'Facebook', WHATSAPP: 'WhatsApp', TIKTOK: 'TikTok' }
const FECHA = new Intl.DateTimeFormat('es-PE', { day: 'numeric', month: 'short', year: 'numeric' })

// Un botón que pide confirmar en la misma tarjeta (sin ventanas del navegador) antes de hacer algo.
function ConConfirmacion({ etiqueta, icono, pregunta, confirmar, haciendo, alConfirmar, procesando, nota }) {
  const [confirmando, setConfirmando] = useState(false)
  if (!confirmando) {
    return (
      <div className="mi-anuncio-accion">
        <button type="button" className="boton-quitar mi-anuncio-cancelar" onClick={() => setConfirmando(true)}>
          <span aria-hidden="true">{icono}</span> {etiqueta}
        </button>
        {nota && <small className="mi-anuncio-nota">{nota}</small>}
      </div>
    )
  }
  return (
    <div className="mi-anuncio-confirmar" role="alertdialog" aria-label={`Confirmar: ${etiqueta}`}>
      <span>{pregunta}</span>
      <button type="button" className="boton-quitar" onClick={alConfirmar} disabled={procesando}>
        {procesando ? haciendo : confirmar}
      </button>
      <button type="button" className="boton-secundario" onClick={() => setConfirmando(false)} disabled={procesando}>
        No
      </button>
    </div>
  )
}

function TarjetaAnuncio({ anuncio, irAPlanes, alCancelar, alReactivar, alEliminar, procesando }) {
  const estado = anuncio.cancelado
    ? { nombre: 'Cancelado', clase: 'estado-cancelado' }
    : (ESTADOS[anuncio.estado] ?? { nombre: anuncio.estado, clase: '' })
  return (
    <article
      className={anuncio.avisoPendiente ? 'tarjeta mi-anuncio mi-anuncio-nuevo' : 'tarjeta mi-anuncio'}
      aria-label={`Anuncio: ${anuncio.titular || anuncio.oferta}`}
    >
      <div className="mi-anuncio-cabecera">
        <span className={`mi-anuncio-estado ${estado.clase}`}>{estado.nombre}</span>
        {anuncio.avisoPendiente && <span className="mi-anuncio-novedad">Nuevo</span>}
        <span className="mi-anuncio-fecha">
          {REDES[anuncio.redSocial] ?? anuncio.redSocial}
          {anuncio.fechaCreacion && ` · ${FECHA.format(new Date(anuncio.fechaCreacion))}`}
        </span>
      </div>
      {/* El titular que escribió la IA; lo que pidió el dueño queda abajo, en chico. */}
      <h2 className="mi-anuncio-oferta">{anuncio.titular || anuncio.oferta}</h2>
      {anuncio.titular && anuncio.titular !== anuncio.oferta && <p className="mi-anuncio-pedido">Tu pedido: {anuncio.oferta}</p>}
      <p className="mi-anuncio-texto">{anuncio.texto}</p>

      {anuncio.cancelado ? (
        <div className="mi-anuncio-motivo mi-anuncio-cancelado">
          <span>Lo cancelaste en AdBrand: no se publica ni se puede programar.</span>
          <div className="mi-anuncio-acciones">
            <button type="button" className="boton-secundario" onClick={alReactivar} disabled={procesando}>
              Volver a activar
            </button>
            <ConConfirmacion
              etiqueta="Eliminar"
              icono="🗑"
              pregunta="¿Eliminar este anuncio de tu lista? Ya no lo verás más aquí."
              confirmar="Sí, eliminar"
              haciendo="Eliminando…"
              alConfirmar={alEliminar}
              procesando={procesando}
            />
          </div>
        </div>
      ) : (
        <>
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
          {anuncio.estado === 'APROBADO' && (
            <>
              <ListoParaPublicar anuncioId={anuncio.id} redSocial={anuncio.redSocial} irAPlanes={irAPlanes} />
              <ConConfirmacion
                etiqueta="Cancelar en AdBrand"
                icono="✕"
                nota="Bórralo si ya lo subiste."
                pregunta="¿Cancelar este anuncio en AdBrand? No se publicará y se quitará de tu calendario. Bórralo si ya lo subiste."
                confirmar="Sí, cancelar"
                haciendo="Cancelando…"
                alConfirmar={alCancelar}
                procesando={procesando}
              />
            </>
          )}
        </>
      )}
    </article>
  )
}

export default function MisAnunciosView({ alVerAvisos, irACrear, irAPlanes }) {
  const {
    anuncios,
    hayMas,
    restantes,
    verMas,
    cantidades,
    cargando,
    error,
    filtro,
    setFiltro,
    busqueda,
    setBusqueda,
    fecha,
    setFecha,
    limpiarBusqueda,
    mensaje,
    procesando,
    cancelar,
    reactivar,
    eliminar,
  } = useMisAnuncios(alVerAvisos)
  const buscando = Boolean(busqueda.trim() || fecha)

  return (
    <main className="pagina pagina-mis-anuncios">
      <header className="pagina-cabecera">
        <h1>Mis anuncios</h1>
        <p>El estado de cada anuncio que creaste y, si un moderador lo revisó, qué decidió.</p>
      </header>

      <Aviso mensaje={mensaje} />
      {error && (
        <div className="aviso aviso-error" role="alert">
          {error}
        </div>
      )}
      {cargando && <Cargando texto="Cargando tus anuncios..." />}

      {!cargando && !error && (
        <>
          <div className="mis-anuncios-buscador" role="search">
            <label className="campo">
              <span>Buscar</span>
              <input
                type="search"
                value={busqueda}
                maxLength={100}
                placeholder="Ej.: panetón, 2x1, Navidad"
                onChange={(e) => setBusqueda(e.target.value)}
              />
            </label>
            <label className="campo">
              <span>Fecha</span>
              <input type="date" value={fecha} onChange={(e) => setFecha(e.target.value)} />
            </label>
            {buscando && (
              <button type="button" className="boton-enlace" onClick={limpiarBusqueda}>
                Limpiar
              </button>
            )}
          </div>

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
              {buscando
                ? 'No encontramos anuncios con esa búsqueda.'
                : cantidades.TODOS === 0 && filtro === 'TODOS'
                  ? 'Todavía no creaste anuncios.'
                  : 'No hay anuncios con este estado.'}
              {!buscando && cantidades.TODOS === 0 && filtro === 'TODOS' && irACrear && (
                <button type="button" className="boton-primario" onClick={irACrear}>
                  Crear mi primer anuncio
                </button>
              )}
            </SinResultados>
          ) : (
            <div className="mis-anuncios-lista">
              {anuncios.map((anuncio) => (
                <TarjetaAnuncio
                  key={anuncio.id}
                  anuncio={anuncio}
                  irAPlanes={irAPlanes}
                  alCancelar={() => cancelar(anuncio.id)}
                  alReactivar={() => reactivar(anuncio.id)}
                  alEliminar={() => eliminar(anuncio.id)}
                  procesando={procesando === anuncio.id}
                />
              ))}
            </div>
          )}
          {hayMas && (
            <button type="button" className="boton-secundario mis-anuncios-ver-mas" onClick={verMas}>
              Ver más ({restantes})
            </button>
          )}
        </>
      )}
    </main>
  )
}
