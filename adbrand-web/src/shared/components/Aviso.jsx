// Recuadro de aviso. Los de éxito (verdes) se muestran unos segundos y se desvanecen solos;
// los de error se quedan hasta que el usuario corrige o reintenta, para que pueda leerlos con calma.
// Recibe el objeto { tipo, texto } tal cual lo guardan los viewmodels.
import { useEffect, useState } from 'react'
import './Avisos.css'

export const SEGUNDOS_AVISO_EXITO = 4
const MILISEGUNDOS_DESVANECER = 500

export default function Aviso({ mensaje, segundos = SEGUNDOS_AVISO_EXITO, como: Etiqueta = 'div', className = '' }) {
  // Se guarda el mensaje (el objeto) que ya se está ocultando o se ocultó: si llega uno nuevo, se muestra.
  const [saliendo, setSaliendo] = useState(null)
  const [oculto, setOculto] = useState(null)
  const temporal = mensaje?.tipo === 'exito'

  useEffect(() => {
    if (!temporal) return undefined
    const desvanecer = setTimeout(() => setSaliendo(mensaje), segundos * 1000)
    const quitar = setTimeout(() => setOculto(mensaje), segundos * 1000 + MILISEGUNDOS_DESVANECER)
    return () => {
      clearTimeout(desvanecer)
      clearTimeout(quitar)
    }
  }, [mensaje, temporal, segundos])

  if (!mensaje || oculto === mensaje) return null

  const clases = ['aviso', `aviso-${mensaje.tipo}`, saliendo === mensaje ? 'aviso-saliendo' : '', className]
    .filter(Boolean)
    .join(' ')
  return (
    <Etiqueta className={clases} role={mensaje.tipo === 'error' ? 'alert' : 'status'}>
      {mensaje.texto}
    </Etiqueta>
  )
}
