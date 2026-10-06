// View: formulario para solicitar el Kit de Marca. La lógica está en useSolicitarKitMarca.
import { useSolicitarKitMarca } from '../viewmodels/useSolicitarKitMarca'
import './SolicitarKitMarcaView.css'

function Campo({ etiqueta, error, children, ayuda }) {
  return (
    <label className={error ? 'campo campo-con-error' : 'campo'}>
      <span>{etiqueta}</span>
      {children}
      {error && <small className="campo-error">{error}</small>}
      {ayuda && !error && <small className="campo-ayuda">{ayuda}</small>}
    </label>
  )
}

function SelectEstilo({ valor, error, onChange, opciones }) {
  return (
    <Campo etiqueta="Estilo visual" error={error}>
      <select value={valor} onChange={(e) => onChange(e.target.value)}>
        <option value="">Selecciona un estilo</option>
        {opciones.map((op) => (
          <option key={op.valor} value={op.valor}>
            {op.nombre}
          </option>
        ))}
      </select>
      {error && <small className="campo-error">{error}</small>}
    </Campo>
  )
}

export default function SolicitarKitMarcaView({ alGenerar }) {
  const { formulario, errores, aviso, generando, estilos, cambiarCampo, generar, reiniciar } = useSolicitarKitMarca()

  function alEnviar(evento) {
    evento.preventDefault()
    generar()
  }

  return (
    <main className="pagina pagina-kit">
      <header className="pagina-cabecera">
        <h1>Solicitar Kit de Marca</h1>
        <p>Completa la información y la IA generará tu Kit de Marca básico: logo, tipografías, paleta y voz.</p>
      </header>

      {aviso && <div className={`aviso aviso-${aviso.tipo}`}>{aviso.texto}</div>}

      <section className="tarjeta">
        <h2>Datos de tu marca</h2>

        <form className="formulario" onSubmit={alEnviar} noValidate>
          <Campo etiqueta="Nombre del negocio" error={errores.nombreNegocio}>
            <input
              value={formulario.nombreNegocio}
              placeholder="Cafetería El Aroma"
              onChange={(e) => cambiarCampo('nombreNegocio', e.target.value)}
            />
          </Campo>

          <Campo etiqueta="Sector / Rubro" error={errores.sector}>
            <input
              value={formulario.sector}
              placeholder="Cafetería, panadería, ropa, tecnología..."
              onChange={(e) => cambiarCampo('sector', e.target.value)}
            />
          </Campo>

          <Campo etiqueta="Paleta de colores sugerida (opcional)" ayuda="Ej: Verdes y tierra, o #2E7D32, #8FBC8F">
            <input
              value={formulario.paletaSugerida}
              placeholder="Verdes y tierra, o códigos hex..."
              onChange={(e) => cambiarCampo('paletaSugerida', e.target.value)}
            />
          </Campo>

          <Campo etiqueta="Valores / Eslogan (opcional)" ayuda="Ej: Café de origen, sostenible, cálido">
            <textarea
              rows="3"
              value={formulario.valoresEslogan}
              placeholder="Valores que representa tu marca, eslogan..."
              onChange={(e) => cambiarCampo('valoresEslogan', e.target.value)}
            />
          </Campo>

          <SelectEstilo
            valor={formulario.estiloVisual}
            error={errores.estiloVisual}
            onChange={(v) => cambiarCampo('estiloVisual', v)}
            opciones={estilos}
          />

          <div className="formulario-acciones">
            <button type="button" className="boton-secundario" onClick={reiniciar} disabled={generando}>
              Limpiar
            </button>
            <button type="submit" className="boton-ia" disabled={generando}>
              {generando ? '✦ Generando Kit de Marca...' : '✦ Generar Kit de Marca'}
            </button>
          </div>
        </form>
      </section>
    </main>
  )
}