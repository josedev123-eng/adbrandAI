// View: cambiar el avatar en Mi Cuenta por el logo del kit de marca o por una foto. Sin imagen se usan las iniciales.
import { useRef } from 'react'
import { useAvatar } from '../viewmodels/useAvatar'

export default function AvatarConImagen({ imagen, alCambiar }) {
  const { guardando, error, usarLogoDelKit, subirFoto, quitar } = useAvatar(alCambiar)
  const selector = useRef(null)

  return (
    <section className="tarjeta avatar-imagen" aria-label="Tu avatar">
      <h2>Tu avatar</h2>
      <p className="avatar-imagen-ayuda">Es la imagen que ves en el menú, junto al nombre de tu empresa.</p>
      <div className="avatar-imagen-fila">
        {imagen ? (
          <img className="avatar-imagen-vista" src={imagen} alt="Tu avatar actual" />
        ) : (
          <span className="avatar-imagen-vista avatar-imagen-vacia">Sin imagen</span>
        )}
        <div className="avatar-imagen-acciones">
          <button type="button" className="boton-primario" onClick={usarLogoDelKit} disabled={guardando}>
            Usar el logo de mi kit
          </button>
          <button type="button" className="boton-secundario" onClick={() => selector.current?.click()} disabled={guardando}>
            Subir una foto
          </button>
          {imagen && (
            <button type="button" className="boton-enlace" onClick={quitar} disabled={guardando}>
              Quitar imagen
            </button>
          )}
          <input
            ref={selector}
            type="file"
            accept="image/png,image/jpeg"
            hidden
            aria-label="Elegir foto para el avatar"
            onChange={(e) => {
              subirFoto(e.target.files?.[0])
              e.target.value = ''
            }}
          />
        </div>
      </div>
      {guardando && <p className="avatar-imagen-ayuda">Guardando…</p>}
      {error && <p className="campo-error">{error}</p>}
    </section>
  )
}
