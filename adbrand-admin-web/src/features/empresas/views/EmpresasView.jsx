// View: las PYMEs registradas. El superadministrador o el moderador puede suspender una empresa que abusa
// (con motivo) y reactivarla. La lógica está en useEmpresas.
import Aviso from '../../../shared/components/Aviso'
import Cargando from '../../../shared/components/Cargando'
import SinResultados from '../../../shared/components/SinResultados'
import { MOTIVO_MAXIMO } from '../models/empresasApi'
import { useEmpresas } from '../viewmodels/useEmpresas'
import './EmpresasView.css'

const TOKENS = new Intl.NumberFormat('es-PE')
const FECHA = new Intl.DateTimeFormat('es-PE', { day: 'numeric', month: 'short', year: 'numeric' })

function Ventana({ titulo, children, alCerrar }) {
  return (
    <div className="empresas-fondo" onClick={alCerrar}>
      <div
        className="empresas-ventana"
        role="dialog"
        aria-modal="true"
        aria-label={titulo}
        onClick={(evento) => evento.stopPropagation()}
      >
        <h2>{titulo}</h2>
        {children}
      </div>
    </div>
  )
}

export default function EmpresasView() {
  const {
    empresas,
    cargando,
    error,
    mensaje,
    busqueda,
    setBusqueda,
    estado,
    setEstado,
    aSuspender,
    aReactivar,
    motivo,
    errorMotivo,
    procesando,
    abrirSuspender,
    abrirReactivar,
    cerrarVentanas,
    cambiarMotivo,
    confirmarSuspension,
    confirmarReactivacion,
  } = useEmpresas()

  return (
    <main className="pagina pagina-empresas">
      <header className="pagina-cabecera">
        <h1>Empresas</h1>
        <p>Las PYMEs registradas. Suspende la cuenta de una empresa que abusa, por ejemplo con contenido prohibido repetido.</p>
      </header>

      <Aviso mensaje={mensaje} />
      {error && (
        <div className="aviso aviso-error" role="alert">
          {error}
        </div>
      )}

      <section className="tarjeta">
        <div className="empresas-filtros">
          <label className="campo">
            <span>Buscar</span>
            <input
              type="search"
              value={busqueda}
              maxLength={100}
              placeholder="Ej.: nombre de la empresa o correo"
              onChange={(evento) => setBusqueda(evento.target.value)}
            />
          </label>
          <label className="campo">
            <span>Estado</span>
            <select value={estado} onChange={(evento) => setEstado(evento.target.value)}>
              <option value="">Todas</option>
              <option value="ACTIVO">Activas</option>
              <option value="INACTIVO">Suspendidas</option>
            </select>
          </label>
        </div>

        {cargando && <Cargando texto="Cargando empresas..." />}
        {!cargando && !error && empresas.length === 0 && <SinResultados>No hay empresas con esos filtros.</SinResultados>}
        {!cargando && empresas.length > 0 && (
          <div className="empresas-tabla-contenedor">
            <table className="empresas-tabla">
              <thead>
                <tr>
                  <th>Empresa</th>
                  <th>Plan</th>
                  <th className="empresas-numero">IA este mes</th>
                  <th className="empresas-numero">Rechazados</th>
                  <th>Estado</th>
                  <th>Acción</th>
                </tr>
              </thead>
              <tbody>
                {empresas.map((empresa) => (
                  <tr key={empresa.id} className={empresa.estado === 'INACTIVO' ? 'empresa-suspendida' : undefined}>
                    <td>
                      <strong>{empresa.nombre_empresa}</strong>
                      <small>{empresa.correo}</small>
                    </td>
                    <td>{empresa.plan ?? 'Gratuito'}</td>
                    <td className="empresas-numero">{TOKENS.format(empresa.tokens_mes)} tokens</td>
                    <td className="empresas-numero">
                      <span className={empresa.rechazados >= 3 ? 'empresas-rechazos-alto' : undefined}>
                        {empresa.rechazados} de {empresa.anuncios}
                      </span>
                    </td>
                    <td>
                      {empresa.estado === 'INACTIVO' ? (
                        <span className="empresas-estado empresas-estado-suspendida" title={empresa.motivo_suspension ?? ''}>
                          Suspendida
                          {empresa.fecha_suspension && ` · ${FECHA.format(new Date(empresa.fecha_suspension))}`}
                        </span>
                      ) : (
                        <span className="empresas-estado">Activa</span>
                      )}
                      {empresa.estado === 'INACTIVO' && empresa.motivo_suspension && (
                        <small className="empresas-motivo">{empresa.motivo_suspension}</small>
                      )}
                    </td>
                    <td>
                      {empresa.estado === 'INACTIVO' ? (
                        <button type="button" className="boton-secundario boton-pequeno" onClick={() => abrirReactivar(empresa)}>
                          Reactivar
                        </button>
                      ) : (
                        <button type="button" className="boton-peligro boton-pequeno" onClick={() => abrirSuspender(empresa)}>
                          Suspender
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {aSuspender && (
        <Ventana titulo={`Suspender a ${aSuspender.nombre_empresa}`} alCerrar={procesando ? undefined : cerrarVentanas}>
          <p>
            La empresa no podrá iniciar sesión y, si está conectada, su sesión se cerrará. Le enviaremos un correo con el
            motivo y quedará registrado en la auditoría.
          </p>
          <label className={errorMotivo ? 'campo campo-con-error' : 'campo'}>
            <span>Motivo de la suspensión</span>
            <textarea
              rows="3"
              value={motivo}
              maxLength={MOTIVO_MAXIMO}
              placeholder="Ej.: Publica contenido de tabaco una y otra vez, aunque se le rechazó."
              onChange={(evento) => cambiarMotivo(evento.target.value)}
            />
            {errorMotivo ? (
              <small className="campo-error">{errorMotivo}</small>
            ) : (
              <small className="campo-ayuda">
                {motivo.length} / {MOTIVO_MAXIMO}. La empresa verá este motivo.
              </small>
            )}
          </label>
          <div className="empresas-ventana-acciones">
            <button type="button" className="boton-secundario" onClick={cerrarVentanas} disabled={procesando}>
              Cancelar
            </button>
            <button type="button" className="boton-peligro" onClick={confirmarSuspension} disabled={procesando}>
              {procesando ? 'Suspendiendo...' : 'Suspender cuenta'}
            </button>
          </div>
        </Ventana>
      )}

      {aReactivar && (
        <Ventana titulo={`Reactivar a ${aReactivar.nombre_empresa}`} alCerrar={procesando ? undefined : cerrarVentanas}>
          <p>La empresa podrá volver a iniciar sesión y usar AdBrand.AI. Le avisaremos por correo.</p>
          <div className="empresas-ventana-acciones">
            <button type="button" className="boton-secundario" onClick={cerrarVentanas} disabled={procesando}>
              Cancelar
            </button>
            <button type="button" className="boton-primario" onClick={confirmarReactivacion} disabled={procesando}>
              {procesando ? 'Reactivando...' : 'Sí, reactivar'}
            </button>
          </div>
        </Ventana>
      )}
    </main>
  )
}
