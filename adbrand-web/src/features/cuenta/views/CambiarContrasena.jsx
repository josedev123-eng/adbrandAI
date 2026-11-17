// View: Mi cuenta → tarjeta para cambiar la contraseña. La lógica está en useCambiarContrasena.
import Aviso from '../../../shared/components/Aviso'
import InputContrasena from '../../../shared/components/InputContrasena'
import CampoAuth from '../../auth/views/CampoAuth'
import { useCambiarContrasena } from '../viewmodels/useCambiarContrasena'

export default function CambiarContrasena() {
  const { datos, errores, mensaje, guardando, cambiar, guardar } = useCambiarContrasena()

  return (
    <form className="tarjeta formulario" onSubmit={guardar} noValidate aria-labelledby="cuenta-clave-titulo">
      <h2 id="cuenta-clave-titulo" className="formulario-titulo">
        Cambiar contraseña
      </h2>
      <Aviso mensaje={mensaje} />
      <CampoAuth etiqueta="Contraseña actual" error={errores.actual}>
        <InputContrasena
          autoComplete="current-password"
          value={datos.actual}
          maxLength={72}
          onChange={(e) => cambiar('actual', e.target.value)}
        />
      </CampoAuth>
      <div className="fila">
        <CampoAuth
          etiqueta="Contraseña nueva"
          error={errores.nueva}
          ayuda="Mínimo 8 caracteres, con una mayúscula, una minúscula y un número."
        >
          <InputContrasena
            autoComplete="new-password"
            value={datos.nueva}
            maxLength={72}
            onChange={(e) => cambiar('nueva', e.target.value)}
          />
        </CampoAuth>
        <CampoAuth etiqueta="Confirmar contraseña nueva" error={errores.confirmar}>
          <InputContrasena
            autoComplete="new-password"
            value={datos.confirmar}
            maxLength={72}
            onChange={(e) => cambiar('confirmar', e.target.value)}
          />
        </CampoAuth>
      </div>
      <div className="formulario-acciones">
        <button className="boton-primario" type="submit" disabled={guardando}>
          {guardando ? 'Guardando...' : 'Cambiar contraseña'}
        </button>
      </div>
    </form>
  )
}
