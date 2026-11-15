// View: tutorial guiado para la empresa nueva. Oscurece la pantalla menos la opción que explica
// (un "hueco" con sombra enorme alrededor) y muestra al lado una tarjeta con el texto y los botones.
// Teclado: → o Enter avanza, ← retrocede, Esc lo salta.
import { useEffect, useRef } from 'react'
import { useTutorial } from '../viewmodels/useTutorial'
import './TutorialGuiado.css'

const ANCHO_TARJETA = 340
const SEPARACION = 14

// Dónde va la tarjeta: a la derecha del hueco (menú lateral), si no debajo, si no encima. Siempre dentro de la pantalla.
function posicionTarjeta(hueco) {
  if (!hueco) return null
  const anchoVentana = window.innerWidth
  const altoVentana = window.innerHeight
  const ancho = Math.min(ANCHO_TARJETA, anchoVentana - 32)
  const ajustarX = (x) => Math.max(16, Math.min(x, anchoVentana - ancho - 16))
  const ajustarY = (y) => Math.max(16, Math.min(y, altoVentana - 300))

  if (hueco.left + hueco.width + SEPARACION + ancho + 16 <= anchoVentana) {
    const left = hueco.left + hueco.width + SEPARACION
    // Opción en la mitad de abajo: la tarjeta se alinea por abajo y crece hacia arriba, así nunca se sale.
    if (hueco.top + hueco.height / 2 > altoVentana / 2) {
      return { bottom: Math.max(16, altoVentana - hueco.top - hueco.height), left, width: ancho }
    }
    return { top: ajustarY(hueco.top), left, width: ancho }
  }
  if (hueco.top + hueco.height + SEPARACION + 300 <= altoVentana) {
    return { top: hueco.top + hueco.height + SEPARACION, left: ajustarX(hueco.left), width: ancho }
  }
  return { top: ajustarY(hueco.top - SEPARACION - 300), left: ajustarX(hueco.left), width: ancho }
}

// Lluvia de confeti al terminar (solo CSS; posiciones fijas, así se ve igual en cada render).
const COLORES_CONFETI = ['#4f46e5', '#f97316', '#b5179e', '#15803d', '#facc15', '#38bdf8']
const PIEZAS_CONFETI = Array.from({ length: 36 }, (_, i) => ({
  left: `${(i * 37) % 100}%`,
  color: COLORES_CONFETI[i % COLORES_CONFETI.length],
  retraso: `${(i % 9) * 0.08}s`,
  duracion: `${1.6 + ((i * 7) % 10) / 10}s`,
}))

function Confeti() {
  return (
    <div className="tutorial-confeti" aria-hidden="true">
      {PIEZAS_CONFETI.map((pieza, i) => (
        <span
          key={i}
          style={{ left: pieza.left, background: pieza.color, animationDelay: pieza.retraso, animationDuration: pieza.duracion }}
        />
      ))}
    </div>
  )
}

// Cuatro paneles oscuros alrededor del hueco (arriba, abajo, izquierda, derecha) y un borde de color en el hueco.
function Oscurecer({ hueco }) {
  const abajo = hueco.top + hueco.height
  const derecha = hueco.left + hueco.width
  return (
    <div aria-hidden="true">
      <div className="tutorial-sombra" style={{ top: 0, left: 0, right: 0, height: Math.max(0, hueco.top) }} />
      <div className="tutorial-sombra" style={{ top: abajo, left: 0, right: 0, bottom: 0 }} />
      <div className="tutorial-sombra" style={{ top: hueco.top, left: 0, width: Math.max(0, hueco.left), height: hueco.height }} />
      <div className="tutorial-sombra" style={{ top: hueco.top, left: derecha, right: 0, height: hueco.height }} />
      <div className="tutorial-hueco" style={hueco} />
    </div>
  )
}

export default function TutorialGuiado({ empresa, alTerminar, alSaltar }) {
  const { paso, indice, total, esUltimo, hueco, confirmando, siguiente, anterior, pedirTerminar, continuar, terminar } =
    useTutorial({ alTerminar, alSaltar })
  const botonSiguiente = useRef(null)
  const posicion = posicionTarjeta(hueco)

  // El foco va al botón principal en cada paso (y al volver de la pregunta), para usarlo con teclado y lector de pantalla.
  useEffect(() => {
    botonSiguiente.current?.focus()
  }, [indice, confirmando])

  // Teclado: → continúa, ← vuelve, Esc pregunta si quiere terminar (o, si ya se está preguntando, sigue el recorrido).
  useEffect(() => {
    const alTeclear = (evento) => {
      if (evento.key === 'Escape') {
        if (confirmando) continuar()
        else pedirTerminar()
      } else if (confirmando) {
        return
      } else if (evento.key === 'ArrowRight') siguiente()
      else if (evento.key === 'ArrowLeft') anterior()
    }
    window.addEventListener('keydown', alTeclear)
    return () => window.removeEventListener('keydown', alTeclear)
  }, [confirmando, continuar, pedirTerminar, siguiente, anterior])

  const titulo = indice === 0 && empresa ? `¡Hola, ${empresa}! Qué bueno tenerte aquí` : paso.titulo
  const porcentaje = Math.round(((indice + 1) / total) * 100)

  return (
    <div className="tutorial">
      {/* Capa que bloquea los clics mientras dura el tutorial. */}
      <div className={hueco ? 'tutorial-bloqueo' : 'tutorial-bloqueo tutorial-bloqueo-oscuro'} />
      {hueco && <Oscurecer hueco={hueco} />}
      {paso.final && <Confeti />}

      {/* key: cada paso vuelve a entrar con su animación. */}
      <div
        key={indice}
        className={posicion ? 'tutorial-tarjeta' : 'tutorial-tarjeta tutorial-tarjeta-centro'}
        style={posicion ?? undefined}
        role="dialog"
        aria-modal="true"
        aria-labelledby="tutorial-titulo"
        aria-describedby="tutorial-texto"
      >
        <div className="tutorial-cabecera">
          <span
            className={indice === 0 ? 'tutorial-emoji tutorial-emoji-saludo' : 'tutorial-emoji'}
            style={{ background: paso.color }}
            aria-hidden="true"
          >
            {paso.emoji}
          </span>
          <span className="tutorial-progreso">
            Paso {indice + 1} de {total}
          </span>
          {!confirmando && (
            <button type="button" className="tutorial-cerrar" aria-label="Cerrar tutorial" onClick={pedirTerminar}>
              ✕
            </button>
          )}
        </div>
        <h2 id="tutorial-titulo">{titulo}</h2>
        <p id="tutorial-texto">{paso.texto}</p>
        <div
          className="tutorial-barra"
          role="progressbar"
          aria-label="Avance del recorrido"
          aria-valuenow={porcentaje}
          aria-valuemin={0}
          aria-valuemax={100}
        >
          <span style={{ width: `${porcentaje}%` }} />
        </div>
        {confirmando ? (
          // Pregunta antes de cerrar a mitad del recorrido: continuar o terminar.
          <div className="tutorial-confirmar" role="group" aria-labelledby="tutorial-confirmar-titulo">
            <p id="tutorial-confirmar-titulo">
              <strong>¿Terminar el recorrido aquí?</strong> 🤔 Te faltan {total - indice - 1} pasos. Puedes verlo
              completo cuando quieras desde <strong>Mi cuenta</strong>.
            </p>
            <div className="tutorial-acciones">
              <button type="button" className="boton-secundario" onClick={terminar}>
                Sí, terminar
              </button>
              <button ref={botonSiguiente} type="button" className="boton-primario" onClick={continuar}>
                Continuar el tutorial
              </button>
            </div>
          </div>
        ) : (
          <div className="tutorial-acciones">
            <button type="button" className="boton-enlace tutorial-terminar" onClick={pedirTerminar}>
              {esUltimo ? 'Quedarme aquí' : 'Terminar tutorial'}
            </button>
            {indice > 0 && (
              <button type="button" className="boton-secundario" onClick={anterior}>
                Atrás
              </button>
            )}
            <button ref={botonSiguiente} type="button" className="boton-primario" onClick={siguiente}>
              {esUltimo ? 'Ir a mi negocio 🚀' : indice === 0 ? '¡Vamos! →' : 'Continuar →'}
            </button>
          </div>
        )}
      </div>
    </div>
  )
}
