// View: visualización del Kit de Marca generado con opción a descargar (HU-11).
import Aviso from '../../../shared/components/Aviso'
import LogoMarca from './LogoMarca'
import IdentidadMarca from './IdentidadMarca'
import './KitMarcaResultadoView.css'

function Seccion({ titulo, children, icono }) {
  return (
    <section className="kit-seccion">
      <div className="kit-seccion-cabecera">
        <span className="kit-icono">{icono}</span>
        <h3>{titulo}</h3>
      </div>
      <div className="kit-contenido">{children}</div>
    </section>
  )
}

function PaletaColores({ colores }) {
  if (!colores) return <p className="kit-vacio">No disponible</p>
  // Esperamos formato: "#2E7D32 - Verde bosque - Confianza\n#8FBC8F - Verde claro - Frescura"
  const items = colores.split('\n').filter(Boolean)
  return (
    <div className="kit-paleta">
      {items.map((item, i) => {
        const [hex, ...resto] = item.split(' - ')
        const nombre = resto.join(' - ') || hex
        return (
          <div key={i} className="kit-color" style={{ '--color-hex': hex.trim() }}>
            <div className="kit-color-muestra"></div>
            <div className="kit-color-info">
              <span className="kit-color-hex">{hex.trim()}</span>
              <span className="kit-color-nombre">{nombre.trim()}</span>
            </div>
          </div>
        )}
      )}
    </div>
  )
}

function Tipografia({ primaria, secundaria }) {
  return (
    <div className="kit-tipografias">
      <div className="kit-tipo">
        <strong>Primaria:</strong> {primaria || '—'}
      </div>
      <div className="kit-tipo">
        <strong>Secundaria:</strong> {secundaria || '—'}
      </div>
    </div>
  )
}

// HU-32: "alEditar" abre la edición manual de colores, tipografías y logotipo.
// HU-29: "alDisenarLogo" abre el editor interactivo del logo. HU-30: "alEditarColores" abre el editor de colores.
// HU-31: "alEditarIdentidad" abre el editor de tipografías y voz de marca.
export default function KitMarcaResultadoView({
  kit,
  aviso,
  generando,
  alAceptar,
  alPedirOtra,
  alVolver,
  alEditar,
  alDisenarLogo,
  alEditarColores,
  alEditarIdentidad,
  alActualizarKit,
}) {
  return (
    <main className="pagina pagina-kit-resultado">
      <header className="pagina-cabecera">
        <button type="button" className="boton-volver" onClick={alVolver}>← Volver</button>
        <div>
          <h1>Kit de Marca generado</h1>
          <p className="kit-meta">
            <strong>{kit.nombreNegocio}</strong> · {kit.sector}
            {kit.simulado && <span className="etiqueta-simulado">Simulado</span>}
          </p>
        </div>
      </header>

      {kit.simulado && (
        <div className="aviso aviso-info aviso-simulado">
          ⚠ Modo simulado: el contenido es de ejemplo. Conecta tu API de IA para resultados reales.
        </div>
      )}

      <Aviso mensaje={aviso} />

      {alActualizarKit && <IdentidadMarca kit={kit} alActualizar={alActualizarKit} />}

      <div className="kit-grid">
        <Seccion titulo="Concepto de Logo" icono="🎨">
          {/* El logo real (subido o hecho con IA) si ya hay; si no, solo las letras del concepto, sin fondo. */}
          {kit.logoImagen ? (
            <img className="kit-logo-imagen" src={kit.logoImagen} alt={`Logo de ${kit.nombreNegocio}`} />
          ) : (
            kit.logoDiseno && <LogoMarca diseno={kit.logoDiseno} tamano={120} soloLetras />
          )}
          <p className="kit-texto">{kit.logoConcepto || 'No disponible'}</p>
        </Seccion>

        <Seccion titulo="Tipografías" icono="🔤">
          <Tipografia primaria={kit.tipografiaPrimaria} secundaria={kit.tipografiaSecundaria} />
        </Seccion>

        <Seccion titulo="Paleta de Colores" icono="🎨">
          <PaletaColores colores={kit.paletaColores} />
        </Seccion>

        <Seccion titulo="Voz de Marca" icono="🗣️">
          <p className="kit-texto">{kit.vozMarca || 'No disponible'}</p>
        </Seccion>
      </div>

      <div className="kit-acciones">
        <button type="button" className="boton-primario" onClick={alAceptar} disabled={generando}>
          ✓ Aceptar kit
        </button>
        {alEditar && (
          <button type="button" className="boton-secundario" onClick={alEditar} disabled={generando}>
            ✏️ Editar
          </button>
        )}
        {alEditarColores && (
          <button type="button" className="boton-secundario" onClick={alEditarColores} disabled={generando}>
            🎨 Colores
          </button>
        )}
        {alEditarIdentidad && (
          <button type="button" className="boton-secundario" onClick={alEditarIdentidad} disabled={generando}>
            🔤 Identidad
          </button>
        )}
        {alDisenarLogo && (
          <button type="button" className="boton-secundario" onClick={alDisenarLogo} disabled={generando}>
            {kit.logoDiseno ? '🖌️ Editar logo' : '🖌️ Diseñar logo'}
          </button>
        )}
        <button type="button" className="boton-secundario" onClick={alPedirOtra} disabled={generando}>
          {generando ? '✦ Generando otra versión...' : '🔁 Pedir otra versión'}
        </button>
      </div>
    </main>
  )
}