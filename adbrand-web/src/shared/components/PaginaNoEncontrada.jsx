// Página 404: la dirección no existe (un enlace viejo o mal escrito). Ofrece volver a un lugar conocido.
import './PaginaNoEncontrada.css'

export default function PaginaNoEncontrada({ alVolver, textoVolver = 'Ir al inicio' }) {
  return (
    <main className="pagina pagina-no-encontrada">
      <span className="no-encontrada-codigo" aria-hidden="true">
        404
      </span>
      <h1>No encontramos esta página</h1>
      <p>Puede que el enlace esté mal escrito o que la página ya no exista. No te preocupes, tus datos están a salvo.</p>
      <button type="button" className="boton-primario" onClick={alVolver}>
        {textoVolver}
      </button>
    </main>
  )
}
