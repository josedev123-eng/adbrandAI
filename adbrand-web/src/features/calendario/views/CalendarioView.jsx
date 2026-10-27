// View: calendario mensual de publicaciones. La lógica está en useCalendario (HU 33).
import { useState } from 'react'
import {
  diasDelMes,
  fechaDeDia,
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

function hoyIso() {
  const hoy = new Date()
  return `${hoy.getFullYear()}-${String(hoy.getMonth() + 1).padStart(2, '0')}-${String(hoy.getDate()).padStart(2, '0')}`
}

export default function CalendarioView() {
  const { mes, publicaciones, cargando, guardando, error, cambiarMes, programar } = useCalendario()
  const [formulario, setFormulario] = useState({
    titulo: '',
    redSocial: 'INSTAGRAM',
    fecha: hoyIso(),
    hora: '10:00',
  })
  const [aviso, setAviso] = useState(null)

  const totalDias = diasDelMes(mes)
  const huecos = primerDiaSemana(mes)
  const celdas = [...Array(huecos).fill(null), ...Array.from({ length: totalDias }, (_, indice) => indice + 1)]

  function cambiarCampo(campo, valor) {
    setFormulario((actual) => ({ ...actual, [campo]: valor }))
  }

  async function alProgramar(evento) {
    evento.preventDefault()
    setAviso(null)
    const guardado = await programar({
      ...formulario,
      titulo: formulario.titulo.trim(),
      hora: `${formulario.hora}:00`,
    })
    if (guardado) {
      setAviso('Publicación programada.')
      setFormulario((actual) => ({ ...actual, titulo: '' }))
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

        {error && <p className="aviso aviso-error">{error}</p>}
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
              return (
                <div key={fecha} className={esHoy ? 'calendario-celda calendario-hoy' : 'calendario-celda'}>
                  <span className="calendario-numero">{dia}</span>
                  <div className="calendario-publicaciones">
                    {delDia.map((publicacion) => (
                      <span
                        key={publicacion.id}
                        className={`calendario-chip calendario-chip-${publicacion.redSocial.toLowerCase()}`}
                        title={`${publicacion.titulo} · ${publicacion.hora}`}
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

        <label className="campo">
          <span>Hora</span>
          <input
            type="time"
            value={formulario.hora}
            onChange={(evento) => cambiarCampo('hora', evento.target.value)}
            required
          />
        </label>

        {aviso && <p className="aviso aviso-ok">{aviso}</p>}

        <div className="pago-acciones">
          <button type="submit" className="boton-primario" disabled={guardando}>
            {guardando ? 'Guardando…' : 'Programar publicación'}
          </button>
        </div>
      </form>
    </main>
  )
}
