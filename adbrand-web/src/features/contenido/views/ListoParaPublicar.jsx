// View: "Listo para publicar". El texto con hashtags para copiar, compartir por WhatsApp y el afiche con QR.
import { useListoParaPublicar } from '../viewmodels/useListoParaPublicar'
import './ListoParaPublicar.css'

// "imagen", "nombre" y "eslogan" son para el afiche; "irAMiCuenta" lleva a poner el celular si falta.
export default function ListoParaPublicar({ anuncioId, imagen, nombre, eslogan, irAMiCuenta }) {
  const { publicacion, cargando, error, copiado, abrir, copiar, compartirWhatsapp, imprimirAfiche } =
    useListoParaPublicar(anuncioId)

  if (!publicacion) {
    return (
      <div className="publicar">
        <button type="button" className="boton-primario" onClick={abrir} disabled={cargando}>
          {cargando ? 'Preparando…' : '📣 Listo para publicar'}
        </button>
        {error && <p className="campo-error">{error}</p>}
      </div>
    )
  }

  return (
    <section className="publicar publicar-abierto" aria-label="Listo para publicar">
      <h3>Listo para publicar</h3>
      <p className="publicar-ayuda">Copia el texto y pégalo en Instagram o Facebook, o envíalo directo por WhatsApp.</p>
      <pre className="publicar-texto">{publicacion.textoParaPublicar}</pre>
      <ul className="publicar-hashtags" aria-label="Hashtags">
        {publicacion.hashtags.map((h) => (
          <li key={h}>{h}</li>
        ))}
      </ul>
      <div className="publicar-acciones">
        <button type="button" className="boton-primario" onClick={copiar}>
          {copiado ? '¡Copiado!' : 'Copiar texto con hashtags'}
        </button>
        <button type="button" className="boton-whatsapp" onClick={compartirWhatsapp}>
          Enviar por WhatsApp
        </button>
        {publicacion.tieneWhatsapp && (
          <button type="button" className="boton-secundario" onClick={() => imprimirAfiche({ imagen, nombre, eslogan })}>
            🖨 Afiche con QR para tu local
          </button>
        )}
      </div>
      {!publicacion.tieneWhatsapp && (
        <p className="publicar-nota">
          Agrega tu celular en{' '}
          {irAMiCuenta ? (
            <button type="button" className="boton-enlace" onClick={irAMiCuenta}>
              Mi cuenta
            </button>
          ) : (
            'Mi cuenta'
          )}{' '}
          y tus anuncios llevarán un enlace para que te pidan por WhatsApp, y podrás imprimir un afiche con QR.
        </p>
      )}
      {error && <p className="campo-error">{error}</p>}
    </section>
  )
}
