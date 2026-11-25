// View: formulario para solicitar el Kit de Marca. La lógica está en useSolicitarKitMarca.
import Aviso from '../../../shared/components/Aviso'
import { useSolicitarKitMarca } from '../viewmodels/useSolicitarKitMarca'
import KitMarcaResultadoView from './KitMarcaResultadoView'
import EditarKitMarcaView from './EditarKitMarcaView'
import EditarLogoView from './EditarLogoView'
import EditarColoresView from './EditarColoresView'
import EditarIdentidadView from './EditarIdentidadView'
import './SolicitarKitMarcaView.css'
import Cargando from '../../../shared/components/Cargando'

// Lo que la empresa quiere transmitir (identidad de marca, V70).
const SENTIMIENTOS = ['Confianza', 'Alegría', 'Cercanía', 'Elegancia', 'Calidez', 'Energía', 'Frescura', 'Tradición']

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
    actualizarKit,
    editandoLogo,
    empezarEdicionLogo,
    cancelarEdicionLogo,
    terminarEdicionLogo,
    editandoColores,
    empezarEdicionColores,
    cancelarEdicionColores,
    terminarEdicionColores,
    editandoIdentidad,
    empezarEdicionIdentidad,
    cancelarEdicionIdentidad,
    terminarEdicionIdentidad,
  } = useSolicitarKitMarca()

  function alEnviar(evento) {
    evento.preventDefault()
    generar()
  }

  // HU-31: cambiar tipografías y voz de marca del kit abierto.
  if (mostrarResultado && kit && editandoIdentidad) {
    return (
      <EditarIdentidadView kit={kit} alGuardar={terminarEdicionIdentidad} alCancelar={cancelarEdicionIdentidad} />
    )
  }

  // HU-30: cambiar los colores del kit abierto.
  if (mostrarResultado && kit && editandoColores) {
    return <EditarColoresView kit={kit} alGuardar={terminarEdicionColores} alCancelar={cancelarEdicionColores} />
  }

  // HU-29: diseñar el logo del kit abierto.
  if (mostrarResultado && kit && editandoLogo) {
    return <EditarLogoView kit={kit} alGuardar={terminarEdicionLogo} alCancelar={cancelarEdicionLogo} />
  }

  // HU-32: editar el kit abierto.
  if (mostrarResultado && kit && editando) {
    return <EditarKitMarcaView kit={kit} alGuardar={terminarEdicion} alCancelar={cancelarEdicion} />
  }

  if (mostrarResultado && kit) {
    return (
      <KitMarcaResultadoView
        kit={kit}
        alActualizarKit={actualizarKit}
        aviso={aviso}
        generando={generando}
        alAceptar={aceptarKit}
        alPedirOtra={pedirOtraVersion}
        alVolver={volverAlFormulario}
        alEditar={empezarEdicion}
        alDisenarLogo={empezarEdicionLogo}
        alEditarColores={empezarEdicionColores}
        alEditarIdentidad={empezarEdicionIdentidad}
      />
    )
  }

  return (
    <main className="pagina pagina-kit">
      <header className="pagina-cabecera">
        <h1>Solicitar Kit de Marca</h1>
        <p>Completa la información y la IA generará tu Kit de Marca básico: logo, tipografías, paleta y voz.</p>
      </header>

      <Aviso mensaje={aviso} />

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
        <h2 className="kit-datos-titulo">Datos de tu marca</h2>

        <form className="formulario" onSubmit={alEnviar} noValidate>
          <Campo etiqueta="Nombre del negocio" error={errores.nombreNegocio}>
            <input
              value={formulario.nombreNegocio}
              maxLength={120}
              placeholder="Ej.: Cafetería El Aroma"
              onChange={(e) => cambiarCampo('nombreNegocio', e.target.value)}
            />
          </Campo>

          <Campo etiqueta="Sector / Rubro" error={errores.sector}>
            <input
              value={formulario.sector}
              maxLength={80}
              placeholder="Cafetería, panadería, ropa, tecnología..."
              onChange={(e) => cambiarCampo('sector', e.target.value)}
            />
          </Campo>

          <Campo etiqueta="Paleta de colores sugerida (opcional)" ayuda="Ej: Verdes y tierra, o #2E7D32, #8FBC8F">
            <input
              value={formulario.paletaSugerida}
              maxLength={300}
              placeholder="Verdes y tierra, o códigos hex..."
              onChange={(e) => cambiarCampo('paletaSugerida', e.target.value)}
            />
          </Campo>

          <fieldset className="kit-sentimientos">
            <legend>¿Qué quieres que sienta tu cliente?</legend>
            <div className="kit-sentimientos-opciones">
              {SENTIMIENTOS.map((sentimiento) => {
                const elegidos = formulario.sentimiento ? formulario.sentimiento.split(', ') : []
                const activo = elegidos.includes(sentimiento)
                return (
                  <button
                    key={sentimiento}
                    type="button"
                    className={activo ? 'kit-chip kit-chip-activo' : 'kit-chip'}
                    aria-pressed={activo}
                    onClick={() =>
                      cambiarCampo(
                        'sentimiento',
                        (activo ? elegidos.filter((s) => s !== sentimiento) : [...elegidos, sentimiento].slice(-3)).join(', '),
                      )
                    }
                  >
                    {sentimiento}
                  </button>
                )
              })}
            </div>
            <small className="campo-ayuda">Elige hasta 3.</small>
          </fieldset>

          <Campo etiqueta="¿Qué te hace diferente? (opcional)" ayuda="Ej.: Pan recién horneado a las 6 am, con receta de la abuela">
            <input
              value={formulario.diferencial}
              maxLength={300}
              placeholder="Lo que tus clientes no encuentran en otro lado"
              onChange={(e) => cambiarCampo('diferencial', e.target.value)}
            />
          </Campo>

          <Campo etiqueta="¿Ya tienes eslogan? (opcional)" error={errores.eslogan} ayuda="Si lo dejas vacío, la IA te propone 3.">
            <input
              value={formulario.eslogan}
              maxLength={150}
              placeholder="Ej.: La casa del buen pan"
              onChange={(e) => cambiarCampo('eslogan', e.target.value)}
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
        {generando && <Cargando texto="La IA está creando tu logo, tus colores y tus tipografías…" filas={3} />}
      </section>
    </main>
  )
}