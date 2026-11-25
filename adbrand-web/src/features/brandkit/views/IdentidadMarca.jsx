// View: "Tu identidad" dentro del Kit de Marca: eslogan, logo (el tuyo o uno de la IA) y descargas para imprimir.
// Cuando ya elegiste eslogan o logo, se muestra solo lo elegido con un botón "Cambiar"; las opciones aparecen al
// tocarlo (con "Cancelar"), así la pantalla no te sigue ofreciendo cosas que ya decidiste.
import { useState } from 'react'
import Aviso from '../../../shared/components/Aviso'
import Cargando from '../../../shared/components/Cargando'
import { useIdentidadMarca } from '../viewmodels/useIdentidadMarca'
import RetocarLogo from './RetocarLogo'
import './IdentidadMarca.css'

export default function IdentidadMarca({ kit, alActualizar }) {
  const {
    aviso,
    guardando,
    proponiendo,
    propuestas,
    esloganPropio,
    setEsloganPropio,
    usarEslogan,
    subirLogo,
    pedirPropuestas,
    elegirPropuesta,
    descargarManual,
    descargarLogo,
    guardarLogo,
  } = useIdentidadMarca(kit, alActualizar)

  const opciones = kit.esloganOpciones ?? []
  // "Cambiar": se guarda qué había al abrir; si se elige otro, la sección se vuelve a cerrar sola.
  const [cambiandoEsloganDe, setCambiandoEsloganDe] = useState(null)
  const [cambiandoLogoDe, setCambiandoLogoDe] = useState(null)
  const eligiendoEslogan = !kit.eslogan || (cambiandoEsloganDe !== null && cambiandoEsloganDe === kit.eslogan)
  const eligiendoLogo = !kit.logoImagen || (cambiandoLogoDe !== null && cambiandoLogoDe === kit.logoImagen)

  return (
    <section className="tarjeta identidad" aria-labelledby="identidad-titulo">
      <header className="identidad-cabecera">
        <div>
          <h2 id="identidad-titulo">Tu identidad</h2>
          <p>Tu eslogan y tu logo salen en todos tus anuncios. Descárgalos para tus tarjetas, volantes o letrero.</p>
        </div>
        <button type="button" className="boton-primario" onClick={descargarManual}>
          ⬇ Manual de marca (PDF)
        </button>
      </header>

      <Aviso mensaje={aviso} />

      <div className="identidad-columnas">
        {/* Eslogan */}
        <div className="identidad-bloque">
          <div className="identidad-titulo-bloque">
            <h3>Eslogan</h3>
            {kit.eslogan && !eligiendoEslogan && (
              <button type="button" className="boton-enlace" onClick={() => setCambiandoEsloganDe(kit.eslogan)}>
                Cambiar eslogan
              </button>
            )}
            {kit.eslogan && eligiendoEslogan && (
              <button type="button" className="boton-enlace" onClick={() => setCambiandoEsloganDe(null)}>
                Cancelar
              </button>
            )}
          </div>
          {kit.eslogan ? <p className="identidad-eslogan">“{kit.eslogan}”</p> : <p className="kit-vacio">Aún no eliges uno.</p>}
          {eligiendoEslogan && (
            <>
              {opciones.length > 1 && (
                <ul className="identidad-opciones" aria-label="Eslóganes propuestos por la IA">
                  {opciones.map((opcion) => (
                    <li key={opcion}>
                      <button
                        type="button"
                        className={opcion === kit.eslogan ? 'identidad-opcion identidad-opcion-activa' : 'identidad-opcion'}
                        onClick={() => usarEslogan(opcion)}
                        disabled={guardando || opcion === kit.eslogan}
                        aria-pressed={opcion === kit.eslogan}
                      >
                        {opcion}
                      </button>
                    </li>
                  ))}
                </ul>
              )}
              <form
                className="identidad-propio"
                onSubmit={(e) => {
                  e.preventDefault()
                  usarEslogan(esloganPropio)
                }}
              >
                <label className="campo">
                  <span>¿Prefieres el tuyo?</span>
                  <input
                    value={esloganPropio}
                    maxLength={150}
                    placeholder="Ej.: La casa del buen pan"
                    onChange={(e) => setEsloganPropio(e.target.value)}
                  />
                </label>
                <button type="submit" className="boton-secundario" disabled={guardando || !esloganPropio.trim()}>
                  Usar este
                </button>
              </form>
            </>
          )}
        </div>

        {/* Logo */}
        <div className="identidad-bloque">
          <div className="identidad-titulo-bloque">
            <h3>Logo</h3>
            {kit.logoImagen && !eligiendoLogo && (
              <button type="button" className="boton-enlace" onClick={() => setCambiandoLogoDe(kit.logoImagen)}>
                Cambiar logo
              </button>
            )}
            {kit.logoImagen && eligiendoLogo && (
              <button type="button" className="boton-enlace" onClick={() => setCambiandoLogoDe(null)}>
                Cancelar
              </button>
            )}
          </div>
          {kit.logoImagen ? (
            <div className="identidad-logo-actual">
              <img src={kit.logoImagen} alt={`Logo de ${kit.nombreNegocio}`} />
              <button type="button" className="boton-secundario" onClick={descargarLogo}>
                ⬇ Descargar logo
              </button>
            </div>
          ) : (
            <p className="kit-vacio">Sube el logo que ya tienes o deja que la IA te proponga 3.</p>
          )}
          {/* Mejorar el logo elegido con instrucciones cortas, como los retoques de anuncios. */}
          {kit.logoImagen && !eligiendoLogo && (
            <RetocarLogo kit={kit} guardando={guardando} alUsar={(imagen) => guardarLogo(imagen, 'IA')} />
          )}
          {eligiendoLogo && (
            <>
              <div className="identidad-logo-acciones">
                <label className="boton-secundario identidad-subir">
                  Subir mi logo
                  <input
                    type="file"
                    accept="image/png,image/jpeg"
                    onChange={(e) => {
                      subirLogo(e.target.files?.[0])
                      e.target.value = ''
                    }}
                    disabled={guardando}
                  />
                </label>
                <button type="button" className="boton-ia" onClick={pedirPropuestas} disabled={proponiendo || guardando}>
                  {proponiendo ? '✦ Creando 3 logos…' : '✦ Proponme 3 logos'}
                </button>
              </div>
              <small className="campo-ayuda">PNG o JPG de hasta 2 MB. Mejor con fondo blanco o transparente.</small>

              {proponiendo && <Cargando texto="La IA está dibujando 3 logos para tu marca…" filas={2} />}
              {propuestas.length > 0 && (
                <ul className="identidad-propuestas" aria-label="Logos propuestos por la IA">
                  {propuestas.map((imagen, i) => (
                    <li key={i}>
                      <img src={imagen} alt={`Propuesta de logo ${i + 1}`} />
                      <button type="button" className="boton-primario" onClick={() => elegirPropuesta(imagen)} disabled={guardando}>
                        Elegir
                      </button>
                    </li>
                  ))}
                </ul>
              )}
            </>
          )}
        </div>
      </div>
    </section>
  )
}
