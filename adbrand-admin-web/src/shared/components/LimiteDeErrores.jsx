// Si una pantalla se rompe por un error del código, en vez de quedar en blanco se muestra este aviso.
// React solo permite atrapar estos errores con un componente de clase (Error Boundary).
import { Component } from 'react'
import './Avisos.css'

export default class LimiteDeErrores extends Component {
  constructor(props) {
    super(props)
    this.state = { conError: false }
  }

  static getDerivedStateFromError() {
    return { conError: true }
  }

  componentDidCatch(error, info) {
    // Queda en la consola del navegador para quien esté revisando el problema.
    console.error('Error en la pantalla:', error, info?.componentStack)
  }

  render() {
    if (!this.state.conError) return this.props.children

    const { alVolverAlInicio } = this.props
    return (
      <main className="pagina">
        <section className="tarjeta pantalla-error" role="alert">
          <span className="pantalla-error-icono" aria-hidden="true">!</span>
          <h1>Algo salió mal en esta pantalla</h1>
          <p>Recarga la página para intentarlo de nuevo. Si se repite, revisa la consola del navegador.</p>
          <div className="pantalla-error-acciones">
            <button type="button" className="boton-primario" onClick={() => window.location.reload()}>
              Recargar
            </button>
            {alVolverAlInicio && (
              <button
                type="button"
                className="boton-secundario"
                onClick={() => {
                  this.setState({ conError: false })
                  alVolverAlInicio()
                }}
              >
                Volver al inicio
              </button>
            )}
          </div>
        </section>
      </main>
    )
  }
}
