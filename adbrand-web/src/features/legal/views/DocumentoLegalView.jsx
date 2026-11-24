// View: Términos y condiciones o Política de privacidad. Los textos están en models/textosLegales.js.
import { useEffect } from 'react'
import { ACTUALIZADO, PRIVACIDAD, TERMINOS } from '../models/textosLegales'
import './DocumentoLegal.css'

export default function DocumentoLegalView({ tipo, alVolver, textoVolver = 'Volver' }) {
  const documento = tipo === 'privacidad' ? PRIVACIDAD : TERMINOS

  // Al abrir el documento se empieza a leer desde arriba.
  useEffect(() => {
    window.scrollTo?.(0, 0)
  }, [tipo])

  return (
    <main className="pagina pagina-legal">
      {alVolver && (
        <button type="button" className="boton-enlace legal-volver" onClick={alVolver}>
          ← {textoVolver}
        </button>
      )}
      <header className="pagina-cabecera">
        <h1>{documento.titulo}</h1>
        <p>{documento.resumen}</p>
        <small>Última actualización: {ACTUALIZADO}</small>
      </header>

      <article className="tarjeta legal-documento">
        {documento.secciones.map((seccion) => (
          <section key={seccion.titulo}>
            <h2>{seccion.titulo}</h2>
            {seccion.parrafos?.map((parrafo) => (
              <p key={parrafo}>{parrafo}</p>
            ))}
            {seccion.lista && (
              <ul>
                {seccion.lista.map((punto) => (
                  <li key={punto}>{punto}</li>
                ))}
              </ul>
            )}
          </section>
        ))}
      </article>
    </main>
  )
}
