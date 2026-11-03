// View: formulario para solicitar el Kit de Marca. La lógica está en useSolicitarKitMarca.
import { useSolicitarKitMarca } from '../viewmodels/useSolicitarKitMarca'
import KitMarcaResultadoView from './KitMarcaResultadoView'
import EditarKitMarcaView from './EditarKitMarcaView'
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

export default function SolicitarKitMarcaView() {
  const {
    formulario,
    errores,
    kit,
    mostrarResultado,
    aviso,
    generando,
    estilos,
    cambiarCampo,
    generar,
    pedirOtraVersion,
    aceptarKit,
    volverAlFormulario,
    reiniciar,
    kitsGuardados,
    editando,
    abrirKit,
    empezarEdicion,
    cancelarEdicion,
    terminarEdicion,
  } = useSolicitarKitMarca()

  function alEnviar(evento) {
    evento.preventDefault()
    generar()
  }

  // HU-32: editar el kit abierto.
  if (mostrarResultado && kit && editando) {
    return <EditarKitMarcaView kit={kit} alGuardar={terminarEdicion} alCancelar={cancelarEdicion} />
  }

  if (mostrarResultado && kit) {
    return (
      <KitMarcaResultadoView
        kit={kit}
        aviso={aviso}
        generando={generando}
        alAceptar={aceptarKit}
        alPedirOtra={pedirOtraVersion}
        alVolver={volverAlFormulario}
        alEditar={empezarEdicion}
      />
    )
  }

  return (
    <main className="pagina pagina-kit">
      <header className="pagina-cabecera">
        <h1>Solicitar Kit de Marca</h1>
        <p>Completa la información y la IA generará tu Kit de Marca básico: logo, tipografías, paleta y voz.</p>
      </header>

      {aviso && <div className={`aviso aviso-${aviso.tipo}`}>{aviso.texto}</div>}

      {kitsGuardados.length > 0 && (
        <section className="tarjeta kits-guardados" aria-label="Tus kits guardados">
          <h2>Tus kits guardados</h2>
          <ul>
            {kitsGuardados.map((guardado) => (
              <li key={guardado.id}>
                <button type="button" className="kit-guardado" onClick={() => abrirKit(guardado.id)}>
                  <span className="kit-guardado-colores">
                    {(guardado.paletaColores ?? '').split('\n').filter(Boolean).slice(0, 5).map((linea, i) => (
                      <span key={i} style={{ background: linea.split(' - ')[0].trim() }} />
                    ))}
                  </span>
                  <span className="kit-guardado-nombre">{guardado.nombreNegocio}</span>
                  <span className="kit-guardado-sector">{guardado.sector}</span>
                </button>
              </li>
            ))}
          </ul>
        </section>
      )}

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