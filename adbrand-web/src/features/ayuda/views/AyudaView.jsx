// View: Ayuda / Preguntas frecuentes. Se abre desde el menú y desde el pie de página (también sin sesión).
import { useState } from 'react'
import { CORREO_CONTACTO } from '../../../shared/config/contacto'
import { filtrarPreguntas, TEMAS_AYUDA } from '../models/preguntasFrecuentes'
import './AyudaView.css'

export default function AyudaView({ alVolver }) {
  const [busqueda, setBusqueda] = useState('')
  const temas = filtrarPreguntas(TEMAS_AYUDA, busqueda)

  return (
    <main className="pagina pagina-ayuda">
      {alVolver && (
        <button type="button" className="boton-enlace ayuda-volver" onClick={alVolver}>
          ← Volver
        </button>
      )}
      <header className="pagina-cabecera">
        <h1>Ayuda</h1>
        <p>Respuestas rápidas a las dudas más comunes.</p>
      </header>

      <label className="campo ayuda-buscador">
        <span>Buscar</span>
        <input
          type="search"
          value={busqueda}
          placeholder="Ej.: tokens, contraseña, Yape…"
          onChange={(e) => setBusqueda(e.target.value)}
        />
      </label>

      {temas.length === 0 ? (
        <p className="ayuda-sin-resultados">
          No encontramos nada con "{busqueda.trim()}". Prueba con otra palabra o escríbenos.
        </p>
      ) : (
        temas.map((grupo) => (
          <section key={grupo.tema} className="tarjeta ayuda-tema" aria-labelledby={`tema-${grupo.tema}`}>
            <h2 id={`tema-${grupo.tema}`}>{grupo.tema}</h2>
            {grupo.preguntas.map(({ pregunta, respuesta }) => (
              <details key={pregunta} className="ayuda-pregunta" open={busqueda.trim() !== ''}>
                <summary>{pregunta}</summary>
                <p>{respuesta}</p>
              </details>
            ))}
          </section>
        ))
      )}

      <section className="tarjeta ayuda-contacto">
        <h2>¿No encontraste tu respuesta?</h2>
        <p>
          Escríbenos a <a href={`mailto:${CORREO_CONTACTO}`}>{CORREO_CONTACTO}</a> y te ayudamos.
        </p>
      </section>
    </main>
  )
}
