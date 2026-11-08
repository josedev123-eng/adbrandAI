// Pantalla de Inicio del administrador: tarjetas resumen según el rol.
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { pantallasDelRol } from '../../../app/routes/pantallasPorRol'
import { listarDudosos } from '../../moderacion/models/moderacionApi'
import { verificarEstado } from '../../servidor/models/servidorApi'
import { obtenerIngresosMensuales, obtenerSuscripcionesActivas } from '../../suscripciones/models/suscripcionesApi'
import { obtenerConsumoActual } from '../../tokens/models/tokensApi'
import { tarjetasDelRol } from '../viewmodels/useInicio'
import InicioView from './InicioView'

vi.mock('../../moderacion/models/moderacionApi', () => ({ listarDudosos: vi.fn() }))
vi.mock('../../servidor/models/servidorApi', async (importOriginal) => ({ ...(await importOriginal()), verificarEstado: vi.fn() }))
vi.mock('../../suscripciones/models/suscripcionesApi', async (importOriginal) => ({
  ...(await importOriginal()),
  obtenerSuscripcionesActivas: vi.fn(),
  obtenerIngresosMensuales: vi.fn(),
}))
vi.mock('../../tokens/models/tokensApi', async (importOriginal) => ({ ...(await importOriginal()), obtenerConsumoActual: vi.fn() }))

const ADMIN = { nombres: 'Ana', apellidos: 'Quispe' }

function mostrar(rol, alAbrir = vi.fn()) {
  render(<InicioView admin={ADMIN} claves={tarjetasDelRol(pantallasDelRol(rol))} alAbrir={alAbrir} />)
  return alAbrir
}

function tarjeta(titulo) {
  return screen.getByText(titulo).closest('button')
}

describe('InicioView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    listarDudosos.mockResolvedValue([{ id: 1 }, { id: 2 }])
    obtenerSuscripcionesActivas.mockResolvedValue({ total: 8 })
    obtenerIngresosMensuales.mockResolvedValue([
      { mes: '2026-10', total: '90.00' },
      { mes: '2026-11', total: '120.00' },
    ])
    obtenerConsumoActual.mockResolvedValue({ mes: '2026-11', total: 2500, empresas: [{}, {}], alertas: [] })
    verificarEstado.mockResolvedValue({
      verificado_en: '2026-11-10T13:00:00-05:00',
      servicios: [
        { servicio: 'BACKEND', nombre: 'Backend', estado: 'ACTIVO' },
        { servicio: 'IA', nombre: 'Servicio de IA', estado: 'CAIDO' },
      ],
    })
  })

  it('el SUPERADMIN ve las cinco tarjetas con sus datos', async () => {
    mostrar('SUPERADMIN')

    expect(await screen.findByText('S/ 120.00')).toBeInTheDocument()
    expect(tarjeta('Pendientes de revisión')).toHaveTextContent('2')
    expect(tarjeta('Suscripciones activas')).toHaveTextContent('8')
    expect(tarjeta('Ingresos del mes')).toHaveTextContent('noviembre de 2026')
    expect(await screen.findByText('Usados por 2 empresas.')).toBeInTheDocument()
    expect(await screen.findByText('1 caído')).toBeInTheDocument()
    expect(tarjeta('Estado del servidor')).toHaveTextContent('Revisa: Servicio de IA.')
  })

  it('el MODERADOR solo ve los pendientes y no se piden los demás datos', async () => {
    mostrar('MODERADOR')

    expect(await screen.findByText('Contenidos dudosos esperando una decisión.')).toBeInTheDocument()
    expect(screen.queryByText('Suscripciones activas')).toBeNull()
    expect(screen.queryByText('Estado del servidor')).toBeNull()
    expect(obtenerSuscripcionesActivas).not.toHaveBeenCalled()
    expect(verificarEstado).not.toHaveBeenCalled()
  })

  it('si una tarjeta falla, las demás se muestran igual', async () => {
    obtenerSuscripcionesActivas.mockRejectedValue(new Error('caído'))
    mostrar('FINANZAS')

    expect(await screen.findByText('S/ 120.00')).toBeInTheDocument()
    expect(await screen.findByText(/No se pudo cargar/)).toBeInTheDocument()
  })

  it('al hacer clic en una tarjeta abre su pantalla', async () => {
    const alAbrir = mostrar('TECNICO')

    await screen.findByText('Usados por 2 empresas.')
    await userEvent.click(tarjeta('Tokens de IA del mes'))

    expect(alAbrir).toHaveBeenCalledWith('consumo-tokens')
  })
})
