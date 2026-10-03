// View: visualización del Kit de Marca generado con opción a descargar (HU-11).
import { useState } from 'react'
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

export default function KitMarcaResultadoView({ kit, alVolver, alNuevaSolicitud }) {
  const [copiado, setCopiado] = useState(null)

  async function copiarJson() {
    try {
      await navigator.clipboard.writeText(JSON.stringify(kit, null, 2))
      setCopiado('json')
      setTimeout(() => setCopiado(null), 2000)
    } catch {
      alert('No se pudo copiar. Selecciona el texto manualmente.')
    }
  }

  async function descargarJson() {
    const blob = new Blob([JSON.stringify(kit, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `kit-marca-${kit.nombreNegocio?.replace(/\s+/g, '-') || kit.id}.json`
    a.click()
    URL.revokeObjectURL(url)
  }

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

      <div className="kit-grid">
        <Seccion titulo="Concepto de Logo" icono="🎨">
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
        <button type="button" className="boton-secundario" onClick={alVolver}>
          ← Volver al formulario
        </button>
        <button type="button" className="boton-secundario" onClick={alNuevaSolicitud}>
          Nueva solicitud
        </button>
        <button type="button" className="boton-secundario" onClick={copiarJson}>
          {copiado === 'json' ? '✓ Copiado' : '📋 Copiar JSON'}
        </button>
        <button type="button" className="boton-primario" onClick={descargarJson}>
          💾 Descargar JSON
        </button>
      </div>
    </main>
  )
}