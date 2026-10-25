// View: panel del administrador técnico con el estado de cada servicio (HU-18).
// La lógica (verificar y refrescar cada 15 segundos) está en useEstadoServidor.
import { formatearHora } from '../models/servidorApi'
import { useEstadoServidor } from '../viewmodels/useEstadoServidor'
import './EstadoServidorView.css'

function TarjetaServicio({ servicio }) {
  const activo = servicio.estado === 'ACTIVO'
  return (
    <article className={`tarjeta servicio ${activo ? 'servicio-activo' : 'servicio-caido'}`}>
      <h2>{servicio.nombre}</h2>
      <span className="servicio-estado">
        <span className="servicio-luz" aria-hidden="true" />
        {activo ? 'Activo' : 'Caído'}
      </span>
      {servicio.tiempo_ms != null && <span className="servicio-tiempo">{servicio.tiempo_ms} ms</span>}
      {!activo && servicio.detalle && <p className="servicio-detalle">{servicio.detalle}</p>}
      {activo && servicio.detalle && <p className="servicio-nota">{servicio.detalle}</p>}
    </article>
  )
}

export default function EstadoServidorView() {
  const { estado, cargando, error, verificando, verificarAhora } = useEstadoServidor()
  const caidos = estado?.servicios.filter((servicio) => servicio.estado !== 'ACTIVO') ?? []

  return (
    <main className="pagina pagina-estado">
      <header className="pagina-cabecera estado-cabecera">
        <div>
          <h1>Estado del servidor</h1>
          <p>
            {estado
              ? `Última verificación: ${formatearHora(estado.verificado_en)}`
              : 'Se verifica solo cada 15 segundos.'}
          </p>
        </div>
        <button type="button" className="boton-secundario" onClick={verificarAhora} disabled={verificando}>
          {verificando ? 'Verificando…' : 'Verificar ahora'}
        </button>
      </header>

      {error && <div className="aviso aviso-error">{error}</div>}
      {cargando && <p className="estado-cargando">Verificando servicios...</p>}

      {caidos.length > 0 && (
        <div className="aviso aviso-error" role="alert">
          {caidos.length === 1 ? 'Hay un servicio caído: ' : 'Hay servicios caídos: '}
          {caidos.map((servicio) => servicio.nombre).join(', ')}.
        </div>
      )}

      {estado && (
        <section className="estado-servicios">
          {estado.servicios.map((servicio) => (
            <TarjetaServicio key={servicio.servicio} servicio={servicio} />
          ))}
        </section>
      )}
    </main>
  )
}
