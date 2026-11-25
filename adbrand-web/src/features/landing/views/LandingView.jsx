// View: página de presentación de AdBrand.AI, lo primero que ve quien entra sin sesión.
// Explica qué es, cómo funciona y cuánto cuesta, y lleva a crear la cuenta o a iniciar sesión.
import Cargando from '../../../shared/components/Cargando'
import MarcaAdbrand from '../../../shared/components/MarcaAdbrand'
import PiePagina from '../../../shared/components/PiePagina'
import { formatearPrecio, formatearTokens, LIMITE_GRATUITO, usePlanesPublicos } from '../viewmodels/usePlanesPublicos'
import './LandingView.css'

const PASOS = [
  { titulo: 'Cuéntanos de tu negocio', texto: 'Rubro, productos, público y tus colores. Lo llenas una sola vez.' },
  { titulo: 'La IA crea tu anuncio', texto: 'Elige la red social y la oferta: recibes el texto y la imagen listos en segundos.' },
  { titulo: 'Revisa y programa', texto: 'Apruébalo, ponlo en tu calendario y mide cómo le va a cada campaña.' },
]

const FUNCIONES = [
  { icono: '✦', titulo: 'Anuncios con IA', texto: 'Textos e imágenes para Instagram y Facebook con el tono de tu marca.' },
  { icono: '◐', titulo: 'Kit de marca', texto: 'Paleta de colores, tipografías y estilo visual para que todo se vea de tu negocio.' },
  { icono: '▦', titulo: 'Calendario', texto: 'Organiza tus publicaciones por fecha y red social, sin hojas de cálculo.' },
  { icono: '↗', titulo: 'Informes', texto: 'Cuántos anuncios creaste, cuáles se aprobaron y qué campañas funcionan.' },
  { icono: '✓', titulo: 'Revisión de contenido', texto: 'Cada anuncio se revisa para que no publiques algo engañoso por error.' },
  { icono: '▢', titulo: 'Desde el celular', texto: 'Aprueba tus anuncios desde la app móvil, estés donde estés.' },
]

export default function LandingView({ irARegistro, irALogin, abrir }) {
  const { planes, cargando, error } = usePlanesPublicos()

  return (
    <div className="landing">
      <header className="landing-barra">
        <MarcaAdbrand />
        <nav className="landing-barra-acciones" aria-label="Cuenta">
          <button type="button" className="boton-secundario" onClick={irALogin}>
            Iniciar sesión
          </button>
          <button type="button" className="boton-primario" onClick={irARegistro}>
            Crear cuenta gratis
          </button>
        </nav>
      </header>

      <main>
        <section className="landing-hero" aria-labelledby="landing-titulo">
          <div className="landing-hero-texto">
            <p className="landing-etiqueta">Publicidad con IA para PYMEs del Perú</p>
            <h1 id="landing-titulo">
              Anuncios que se ven profesionales, <span>hechos en minutos</span>
            </h1>
            <p>
              AdBrand.AI crea los anuncios, el kit de marca y el calendario de publicaciones de tu negocio. Sin diseñador y
              sin agencia.
            </p>
            <div className="landing-hero-acciones">
              <button type="button" className="boton-primario landing-boton-grande" onClick={irARegistro}>
                Crear cuenta gratis
              </button>
              <button type="button" className="boton-enlace" onClick={irALogin}>
                Ya tengo cuenta
              </button>
            </div>
            <small>Plan gratuito para empezar. No pedimos tarjeta.</small>
          </div>

          {/* Ejemplo de anuncio, hecho solo con CSS (no descarga imágenes). */}
          <div className="landing-ejemplo" aria-hidden="true">
            <div className="landing-ejemplo-cabecera">
              <span className="landing-ejemplo-avatar">DR</span>
              <span>
                <strong>Panadería Doña Rosa</strong>
                <small>Instagram · Programado vie 10:00</small>
              </span>
            </div>
            <div className="landing-ejemplo-imagen">
              <span>2x1</span>
              <small>en pan de yema</small>
            </div>
            <p>
              ¡Este viernes el pan de yema viene doble! 🥐 Trae a quien quieras y llévate 2x1 recién salido del horno.
            </p>
            <span className="landing-ejemplo-estado">✓ Aprobado</span>
          </div>
        </section>

        <section className="landing-seccion" aria-labelledby="landing-pasos">
          <h2 id="landing-pasos">Cómo funciona</h2>
          <ol className="landing-pasos">
            {PASOS.map((paso, indice) => (
              <li key={paso.titulo} className="tarjeta">
                <span className="landing-paso-numero">{indice + 1}</span>
                <h3>{paso.titulo}</h3>
                <p>{paso.texto}</p>
              </li>
            ))}
          </ol>
        </section>

        <section className="landing-seccion" aria-labelledby="landing-funciones">
          <h2 id="landing-funciones">Todo lo que tu negocio necesita para publicar</h2>
          <ul className="landing-funciones">
            {FUNCIONES.map((funcion) => (
              <li key={funcion.titulo}>
                <span className="landing-funcion-icono" aria-hidden="true">
                  {funcion.icono}
                </span>
                <h3>{funcion.titulo}</h3>
                <p>{funcion.texto}</p>
              </li>
            ))}
          </ul>
        </section>

        <section className="landing-seccion" aria-labelledby="landing-planes">
          <h2 id="landing-planes">Planes</h2>
          <p className="landing-subtitulo">Empieza gratis y cambia de plan cuando tu negocio crezca. Precios en soles.</p>
          <ul className="landing-planes">
            <li className="tarjeta">
              <h3>Gratuito</h3>
              <p className="landing-precio">S/ 0</p>
              <p>{formatearTokens(LIMITE_GRATUITO)} tokens de IA al mes para probar la plataforma.</p>
            </li>
            {planes.map((plan) => (
              <li key={plan.id} className="tarjeta">
                <h3>{plan.nombre}</h3>
                <p className="landing-precio">
                  {formatearPrecio(plan.precio)} <small>/ mes</small>
                </p>
                {/* La descripción del plan ya menciona sus tokens; si no tiene, se muestran los tokens. */}
                <p>{plan.descripcion || `${formatearTokens(plan.limiteTokens)} tokens de IA al mes.`}</p>
              </li>
            ))}
          </ul>
          {cargando && <Cargando texto="Cargando planes…" filas={3} />}
          {error && <p className="landing-nota">Los planes de pago se ven al crear tu cuenta.</p>}
        </section>

        <section className="landing-cierre" aria-labelledby="landing-cierre">
          <h2 id="landing-cierre">Tu próximo anuncio está a unos minutos</h2>
          <button type="button" className="boton-primario landing-boton-grande" onClick={irARegistro}>
            Crear cuenta gratis
          </button>
        </section>
      </main>

      <PiePagina abrir={abrir} />
    </div>
  )
}
