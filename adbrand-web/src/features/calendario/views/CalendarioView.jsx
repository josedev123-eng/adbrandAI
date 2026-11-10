// View: calendario mensual de publicaciones. La lógica está en useCalendario (HU 33).
// HU 35: campañas con su color, filtro por campaña y formulario para crear campañas.
// HU 34: al programar se elige uno de los anuncios aprobados.
// HU 36: las publicaciones programadas se arrastran a otro día.
// HU 35: los días que dura cada campaña se pintan con su color y el primero lleva su nombre.
import { useState } from 'react'
import {
  campanasDelDia,
  diasDelMes,
  fechaDeDia,
  hoyIso,
  primerDiaSemana,
  etiquetaMes,
  useCalendario,
} from '../viewmodels/useCalendario'
import './CalendarioView.css'

const DIAS = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom']
const REDES = [
  { id: 'INSTAGRAM', nombre: 'Instagram' },
  { id: 'FACEBOOK', nombre: 'Facebook' },
]
const TIPOS = [
  { id: 'CAMPANA', nombre: 'Campaña' },
  { id: 'EVENTO', nombre: 'Evento especial' },
]

// "Oferta 2x1 en café" → "Instagram · Oferta 2x1 en café", corto para que entre en la lista.
function etiquetaAnuncio(anuncio) {
  const red = REDES.find((item) => item.id === anuncio.redSocial)?.nombre ?? anuncio.redSocial
  const oferta = anuncio.oferta.length > 60 ? `${anuncio.oferta.slice(0, 60)}…` : anuncio.oferta
  return `${red} · ${oferta}`
}

function campanaVacia() {
  return { nombre: '', color: '#4F46E5', tipo: 'CAMPANA', fechaInicio: hoyIso(), fechaFin: hoyIso() }
}

export default function CalendarioView() {
  const {
    mes,
    publicaciones,
    cargando,
    guardando,
    error,
    cambiarMes,
    programar,
    campanas,
    campanaFiltro,
    filtrarPorCampana,
    agregarCampana,
    erroresCampana,
    guardandoCampana,
    anuncios,
    errorProgramar,
    moverPublicacion,
    avisoArrastre,
  } = useCalendario()
  // HU 36: día sobre el que se está arrastrando una publicación, para resaltarlo.
  const [diaDestino, setDiaDestino] = useState(null)
  const [formulario, setFormulario] = useState({
    titulo: '',
    redSocial: 'INSTAGRAM',
    fecha: hoyIso(),
    hora: '10:00',
    campanaId: '',
    contenidoId: '',
  })
  const [aviso, setAviso] = useState(null)
  const [nuevaCampana, setNuevaCampana] = useState(campanaVacia())
  const [avisoCampana, setAvisoCampana] = useState(null)

  const totalDias = diasDelMes(mes)
  const huecos = primerDiaSemana(mes)
  const celdas = [...Array(huecos).fill(null), ...Array.from({ length: totalDias }, (_, indice) => indice + 1)]
  // Con el filtro puesto solo se pinta esa campaña; sin filtro, todas.
  const campanasVisibles = campanaFiltro
    ? campanas.filter((campana) => String(campana.id) === String(campanaFiltro))
    : campanas

  function cambiarCampo(campo, valor) {
    setFormulario((actual) => ({ ...actual, [campo]: valor }))
  }

  // HU 34: al elegir un anuncio se completan el título (su oferta) y la red social; el título se puede cambiar.
  function elegirAnuncio(contenidoId) {
    const anuncio = anuncios.find((item) => String(item.id) === contenidoId)
    setFormulario((actual) => ({
      ...actual,
      contenidoId,
      titulo: anuncio ? anuncio.oferta.slice(0, 150) : actual.titulo,
      redSocial: anuncio ? anuncio.redSocial : actual.redSocial,
    }))
  }

  // HU 36: arrastrar y soltar. Solo las publicaciones programadas se pueden mover.
  function alEmpezarArrastre(evento, publicacion) {
    evento.dataTransfer.setData('text/plain', String(publicacion.id))
    evento.dataTransfer.effectAllowed = 'move'
  }

  function alPasarSobreDia(evento, fecha) {
    evento.preventDefault()
    evento.dataTransfer.dropEffect = 'move'
    if (diaDestino !== fecha) setDiaDestino(fecha)
  }

  function alSoltarEnDia(evento, fecha) {
    evento.preventDefault()
    setDiaDestino(null)
    const id = Number(evento.dataTransfer.getData('text/plain'))
    if (id) moverPublicacion(id, fecha)
  }

  function cambiarCampoCampana(campo, valor) {
    setNuevaCampana((actual) => ({ ...actual, [campo]: valor }))
  }

  async function alProgramar(evento) {
    evento.preventDefault()
    setAviso(null)
    const guardado = await programar({
      ...formulario,
      titulo: formulario.titulo.trim(),
      hora: `${formulario.hora}:00`,
      campanaId: formulario.campanaId ? Number(formulario.campanaId) : null,
      contenidoId: formulario.contenidoId ? Number(formulario.contenidoId) : null,
    })
    if (guardado) {
      setAviso('Publicación programada.')
      setFormulario((actual) => ({ ...actual, titulo: '', contenidoId: '' }))
    }
  }

  async function alCrearCampana(evento) {
    evento.preventDefault()
    setAvisoCampana(null)
    const guardada = await agregarCampana(nuevaCampana)
    if (guardada) {
      setAvisoCampana('Campaña creada.')
      setNuevaCampana(campanaVacia())
    }
  }

  return (
    <main className="pagina pagina-calendario">
      <header className="pagina-cabecera">
        <h1>Calendario de publicaciones</h1>
        <p>Programa tus anuncios y míralos en el mes que corresponde.</p>
      </header>

      <section className="tarjeta calendario">
        <div className="calendario-barra">
          <button type="button" className="boton-secundario" onClick={() => cambiarMes(-1)} aria-label="Mes anterior">
            ←
          </button>
          <h2 className="calendario-mes">{etiquetaMes(mes)}</h2>
          <button type="button" className="boton-secundario" onClick={() => cambiarMes(1)} aria-label="Mes siguiente">
            →
          </button>
        </div>

        {campanas.length > 0 && (
          <div className="calendario-filtro">
            <label className="campo">
              <span>Ver campaña</span>
              <select value={campanaFiltro} onChange={(evento) => filtrarPorCampana(evento.target.value)}>
                <option value="">Todas las publicaciones</option>
                {campanas.map((campana) => (
                  <option key={campana.id} value={campana.id}>
                    {campana.nombre}
                  </option>
                ))}
              </select>
            </label>

            <ul className="calendario-leyenda" aria-label="Campañas">
              {campanas.map((campana) => (
                <li key={campana.id}>
                  <span className="calendario-leyenda-color" style={{ background: campana.color }} />
                  {campana.nombre}
                </li>
              ))}
            </ul>
          </div>
        )}

        {error && <p className="aviso aviso-error">{error}</p>}
        {avisoArrastre && (
          <p className="aviso calendario-aviso" role="alert">
            {avisoArrastre}
          </p>
        )}
        {cargando ? (
          <p className="cargando">Cargando calendario…</p>
        ) : (
          <div className="calendario-grid" role="grid" aria-label={`Publicaciones de ${etiquetaMes(mes)}`}>
            {DIAS.map((dia) => (
              <span key={dia} className="calendario-dia-semana" role="columnheader">
                {dia}
              </span>
            ))}

            {celdas.map((dia, indice) => {
              if (dia === null) {
                return <span key={`hueco-${indice}`} className="calendario-celda calendario-celda-vacia" />
              }
              const fecha = fechaDeDia(mes, dia)
              const delDia = publicaciones.filter((publicacion) => publicacion.fecha === fecha)
              const esHoy = fecha === hoyIso()
              // HU 35: el día toma el color de la primera campaña vigente; el nombre va en su primer día
              // (o en el día 1 del mes, si la campaña empezó el mes anterior).
              const vigentes = campanasDelDia(campanasVisibles, fecha)
              const nombresQueEmpiezan = vigentes
                .filter((campana) => campana.fechaInicio === fecha || dia === 1)
                .map((campana) => campana.nombre)
              const clasesCelda = ['calendario-celda']
              if (esHoy) clasesCelda.push('calendario-hoy')
              if (vigentes.length > 0) clasesCelda.push('calendario-dia-campana')
              if (diaDestino === fecha) clasesCelda.push('calendario-destino')
              return (
                <div
                  key={fecha}
                  className={clasesCelda.join(' ')}
                  style={vigentes.length > 0 ? { '--color-campana': vigentes[0].color } : undefined}
                  title={vigentes.length > 0 ? vigentes.map((campana) => campana.nombre).join(' · ') : undefined}
                  data-fecha={fecha}
                  onDragOver={(evento) => alPasarSobreDia(evento, fecha)}
                  onDragLeave={() => setDiaDestino((actual) => (actual === fecha ? null : actual))}
                  onDrop={(evento) => alSoltarEnDia(evento, fecha)}
                >
                  <span className="calendario-numero">{dia}</span>
                  {nombresQueEmpiezan.length > 0 && (
                    <span className="calendario-nombre-campana">{nombresQueEmpiezan.join(' · ')}</span>
                  )}
                  <div className="calendario-publicaciones">
                    {delDia.map((publicacion) => (
                      <span
                        key={publicacion.id}
                        draggable={publicacion.estado === 'PROGRAMADA'}
                        onDragStart={(evento) => alEmpezarArrastre(evento, publicacion)}
                        onDragEnd={() => setDiaDestino(null)}
                        className={
                          publicacion.campanaColor
                            ? 'calendario-chip calendario-chip-campana'
                            : `calendario-chip calendario-chip-${publicacion.redSocial.toLowerCase()}`
                        }
                        style={publicacion.campanaColor ? { '--color-campana': publicacion.campanaColor } : undefined}
                        title={[publicacion.titulo, publicacion.hora, publicacion.campanaNombre, publicacion.contenidoOferta]
                          .filter(Boolean)
                          .join(' · ')}
                      >
                        <strong>{publicacion.hora.slice(0, 5)}</strong> {publicacion.titulo}
                      </span>
                    ))}
                  </div>
                </div>
              )
            })}
          </div>
        )}
      </section>

      <form className="tarjeta formulario" onSubmit={alProgramar}>
        <h2>Programar una publicación</h2>

        <label className="campo">
          <span>Anuncio aprobado</span>
          <select value={formulario.contenidoId} onChange={(evento) => elegirAnuncio(evento.target.value)}>
            <option value="">
              {anuncios.length > 0 ? 'Elige un anuncio aprobado' : 'No tienes anuncios aprobados todavía'}
            </option>
            {anuncios.map((anuncio) => (
              <option key={anuncio.id} value={anuncio.id}>
                {etiquetaAnuncio(anuncio)}
              </option>
            ))}
          </select>
        </label>

        <label className="campo">
          <span>Título</span>
          <input
            type="text"
            maxLength={150}
            placeholder="Promoción de fin de semana"
            value={formulario.titulo}
            onChange={(evento) => cambiarCampo('titulo', evento.target.value)}
            required
          />
        </label>

        <div className="fila">
          <label className="campo">
            <span>Red social</span>
            <select
              value={formulario.redSocial}
              onChange={(evento) => cambiarCampo('redSocial', evento.target.value)}
            >
              {REDES.map((red) => (
                <option key={red.id} value={red.id}>
                  {red.nombre}
                </option>
              ))}
            </select>
          </label>

          <label className="campo">
            <span>Fecha</span>
            <input
              type="date"
              value={formulario.fecha}
              onChange={(evento) => cambiarCampo('fecha', evento.target.value)}
              required
            />
          </label>
        </div>

        <div className="fila">
          <label className="campo">
            <span>Hora</span>
            <input
              type="time"
              value={formulario.hora}
              onChange={(evento) => cambiarCampo('hora', evento.target.value)}
              required
            />
          </label>

          <label className="campo">
            <span>Campaña (opcional)</span>
            <select
              value={formulario.campanaId}
              onChange={(evento) => cambiarCampo('campanaId', evento.target.value)}
            >
              <option value="">Sin campaña</option>
              {campanas.map((campana) => (
                <option key={campana.id} value={campana.id}>
                  {campana.nombre}
                </option>
              ))}
            </select>
          </label>
        </div>

        {errorProgramar && <p className="aviso aviso-error">{errorProgramar}</p>}
        {aviso && <p className="aviso aviso-ok">{aviso}</p>}

        <div className="pago-acciones">
          <button type="submit" className="boton-primario" disabled={guardando}>
            {guardando ? 'Guardando…' : 'Programar publicación'}
          </button>
        </div>
      </form>

      <form className="tarjeta formulario" onSubmit={alCrearCampana} noValidate>
        <h2>Nueva campaña o evento</h2>

        <div className="fila">
          <label className="campo">
            <span>Nombre</span>
            <input
              type="text"
              maxLength={100}
              placeholder="Navidad 2026"
              value={nuevaCampana.nombre}
              onChange={(evento) => cambiarCampoCampana('nombre', evento.target.value)}
              aria-invalid={Boolean(erroresCampana.nombre)}
            />
            {erroresCampana.nombre && <small className="campo-error">{erroresCampana.nombre}</small>}
          </label>

          <label className="campo">
            <span>Tipo</span>
            <select value={nuevaCampana.tipo} onChange={(evento) => cambiarCampoCampana('tipo', evento.target.value)}>
              {TIPOS.map((tipo) => (
                <option key={tipo.id} value={tipo.id}>
                  {tipo.nombre}
                </option>
              ))}
            </select>
          </label>
        </div>

        <div className="fila">
          <label className="campo">
            <span>Desde</span>
            <input
              type="date"
              value={nuevaCampana.fechaInicio}
              onChange={(evento) => cambiarCampoCampana('fechaInicio', evento.target.value)}
              aria-invalid={Boolean(erroresCampana.fechaInicio)}
            />
            {erroresCampana.fechaInicio && <small className="campo-error">{erroresCampana.fechaInicio}</small>}
          </label>

          <label className="campo">
            <span>Hasta</span>
            <input
              type="date"
              value={nuevaCampana.fechaFin}
              onChange={(evento) => cambiarCampoCampana('fechaFin', evento.target.value)}
              aria-invalid={Boolean(erroresCampana.fechaFin)}
            />
            {erroresCampana.fechaFin && <small className="campo-error">{erroresCampana.fechaFin}</small>}
          </label>
        </div>

        <label className="campo campo-color">
          <span>Color</span>
          <span className="campo-color-fila">
            <input
              type="color"
              value={nuevaCampana.color}
              onChange={(evento) => cambiarCampoCampana('color', evento.target.value.toUpperCase())}
              aria-label="Elegir color"
            />
            <code>{nuevaCampana.color}</code>
          </span>
          {erroresCampana.color && <small className="campo-error">{erroresCampana.color}</small>}
        </label>

        {erroresCampana.general && <p className="aviso aviso-error">{erroresCampana.general}</p>}
        {avisoCampana && <p className="aviso aviso-ok">{avisoCampana}</p>}

        <div className="pago-acciones">
          <button type="submit" className="boton-primario" disabled={guardandoCampana}>
            {guardandoCampana ? 'Guardando…' : 'Crear campaña'}
          </button>
        </div>
      </form>
    </main>
  )
}
