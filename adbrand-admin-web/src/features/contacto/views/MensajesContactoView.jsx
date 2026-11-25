// View: los mensajes que las PYMEs (o visitantes) mandan desde "Contacto". Se responden por correo y se marcan
// como atendidos. La lógica está en useMensajesContacto.
import Cargando from '../../../shared/components/Cargando'
import SinResultados from '../../../shared/components/SinResultados'
import { useMensajesContacto } from '../viewmodels/useMensajesContacto'
import './MensajesContactoView.css'

const FECHA = new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium', timeStyle: 'short' })

export default function MensajesContactoView() {
  const { mensajes, soloPendientes, setSoloPendientes, cargando, error, guardando, cambiarAtendido } = useMensajesContacto()

  return (
    <main className="pagina pagina-contacto">
      <header className="pagina-cabecera">
        <h1>Mensajes de contacto</h1>
        <p>Lo que escriben desde el formulario de Contacto de la web. Responde por correo y márcalo como atendido.</p>
      </header>

      {error && (
        <div className="aviso aviso-error" role="alert">
          {error}
        </div>
      )}

      <label className="contacto-filtro">
        <input type="checkbox" checked={soloPendientes} onChange={(e) => setSoloPendientes(e.target.checked)} />
        Solo pendientes
      </label>

      {cargando && <Cargando texto="Cargando mensajes…" />}
      {!cargando && mensajes.length === 0 && (
        <SinResultados>{soloPendientes ? 'No hay mensajes pendientes. ¡Todo al día!' : 'Todavía no hay mensajes.'}</SinResultados>
      )}

      {!cargando && (
        <ul className="contacto-lista">
          {mensajes.map((m) => (
            <li key={m.id} className={m.atendido ? 'tarjeta contacto-mensaje contacto-atendido' : 'tarjeta contacto-mensaje'}>
              <div className="contacto-cabecera">
                <span className="contacto-motivo">{m.motivo}</span>
                <small>{FECHA.format(new Date(m.fecha_creacion))}</small>
              </div>
              <strong>{m.nombre}</strong>
              <a href={`mailto:${m.correo}?subject=${encodeURIComponent(`Re: ${m.motivo} - AdBrand.AI`)}`}>{m.correo}</a>
              {m.usuario_id ? <small>Empresa n.º {m.usuario_id}</small> : <small>Escribió sin iniciar sesión</small>}
              <p className="contacto-texto">{m.mensaje}</p>
              <div className="contacto-acciones">
                <a
                  className="boton-primario boton-pequeno"
                  href={`mailto:${m.correo}?subject=${encodeURIComponent(`Re: ${m.motivo} - AdBrand.AI`)}`}
                >
                  Responder por correo
                </a>
                <button
                  type="button"
                  className="boton-secundario boton-pequeno"
                  disabled={guardando === m.id}
                  onClick={() => cambiarAtendido(m)}
                >
                  {m.atendido ? 'Volver a pendiente' : '✓ Marcar como atendido'}
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </main>
  )
}
