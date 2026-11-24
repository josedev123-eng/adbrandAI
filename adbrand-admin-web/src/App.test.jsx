// Login de administradores: sesión guardada, menú según el rol y cerrar sesión.
import { act, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'
import { SesionProvider } from './app/context/SesionProvider'
import { obtenerSesion } from './features/auth/models/authApi'
import { listarDudosos } from './features/moderacion/models/moderacionApi'
import { EVENTO_SESION_VENCIDA } from './shared/services/apiClient'
import { guardarToken, leerToken } from './shared/services/sesionToken'

vi.mock('./features/moderacion/models/moderacionApi', () => ({ listarDudosos: vi.fn() }))
vi.mock('./features/auth/models/authApi', () => ({
  iniciarSesion: vi.fn(),
  obtenerSesion: vi.fn(),
}))
// Las pantallas reales llaman a Django; aquí basta saber cuál se muestra.
vi.mock('./features/inicio/views/InicioView', () => ({ default: () => <p>Pantalla de inicio</p> }))
vi.mock('./features/moderacion/views/BandejaRevisionView', () => ({ default: () => <p>Pantalla de revisión</p> }))
vi.mock('./features/moderacion/views/HistorialContenidosView', () => ({ default: () => <p>Pantalla de historial de contenidos</p> }))
vi.mock('./features/admin/views/CrearAdministradorView', () => ({ default: () => <p>Pantalla de crear</p> }))
vi.mock('./features/admin/views/AdministradoresView', () => ({ default: () => <p>Pantalla de lista</p> }))
vi.mock('./features/auditoria/views/AuditoriaView', () => ({ default: () => <p>Pantalla de auditoría</p> }))
vi.mock('./features/suscripciones/views/PanelFinancieroView', () => ({ default: () => <p>Pantalla financiera</p> }))
vi.mock('./features/tokens/views/ConsumoTokensView', () => ({ default: () => <p>Pantalla de consumo de IA</p> }))
vi.mock('./features/tokens/views/HistorialTokensView', () => ({ default: () => <p>Pantalla de historial de IA</p> }))
vi.mock('./features/servidor/views/EstadoServidorView', () => ({ default: () => <p>Pantalla de estado del servidor</p> }))

function adminCon(rol) {
  return { id: 7, nombres: 'Ana', apellidos: 'Quispe', correo: 'ana@adbrand.pe', rol: { id: 2, nombre: rol } }
}

function mostrar() {
  render(
    <SesionProvider>
      <App />
    </SesionProvider>,
  )
}

function opcionesDelMenu() {
  // Solo las opciones del menú de pantallas (sin el botón ☰, "Mi cuenta" ni "Cerrar sesión").
  const menu = screen.queryByRole('navigation', { name: 'Menú de administración' })
  return menu ? within(menu).queryAllByRole('button').map((b) => b.textContent) : []
}

describe('App (login y menú por rol)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    // Cada prueba empieza en la raíz: la pantalla sale de la URL.
    window.history.replaceState({}, '', '/')
    listarDudosos.mockResolvedValue([])
    localStorage.clear()
  })

  it('sin token muestra el login y no pregunta a Django', () => {
    mostrar()
    expect(screen.getByRole('heading', { name: 'Inicia sesión' })).toBeInTheDocument()
    expect(obtenerSesion).not.toHaveBeenCalled()
  })

  it('con token guardado recupera la sesión al recargar la página', async () => {
    guardarToken('token-guardado')
    obtenerSesion.mockResolvedValue(adminCon('SUPERADMIN'))
    mostrar()

    expect(screen.getByText('Revisando tu sesión…')).toBeInTheDocument()
    expect(await screen.findByText('Pantalla de inicio')).toBeInTheDocument()
    expect(screen.getByText('Superadministrador')).toBeInTheDocument()
    expect(opcionesDelMenu()).toEqual([
      'Inicio',
      'Revisión de contenido',
      'Historial de contenidos',
      'Empresas',
      'Crear administrador',
      'Lista de administradores',
      'Auditoría',
      'Panel financiero',
      'Consumo de IA',
      'Historial de IA',
      'Estado del servidor',
    ])
  })

  it('el MODERADOR solo ve la revisión y el historial de contenidos', async () => {
    guardarToken('token-guardado')
    obtenerSesion.mockResolvedValue(adminCon('MODERADOR'))
    mostrar()

    expect(await screen.findByText('Pantalla de inicio')).toBeInTheDocument()
    expect(opcionesDelMenu()).toEqual(['Inicio', 'Revisión de contenido', 'Historial de contenidos', 'Empresas'])
  })

  it('FINANZAS entra al Inicio y solo ve el panel financiero', async () => {
    guardarToken('token-guardado')
    obtenerSesion.mockResolvedValue(adminCon('FINANZAS'))
    mostrar()

    expect(await screen.findByText('Pantalla de inicio')).toBeInTheDocument()
    expect(opcionesDelMenu()).toEqual(['Inicio', 'Panel financiero'])
  })

  it('TECNICO entra al Inicio y ve el consumo de IA y el servidor (HU 17)', async () => {
    guardarToken('token-guardado')
    obtenerSesion.mockResolvedValue(adminCon('TECNICO'))
    mostrar()

    expect(await screen.findByText('Pantalla de inicio')).toBeInTheDocument()
    expect(opcionesDelMenu()).toEqual(['Inicio', 'Consumo de IA', 'Historial de IA', 'Estado del servidor'])
    expect(screen.queryByText(/Tu rol todavía no tiene pantallas/)).toBeNull()
  })

  it('si el token guardado ya no sirve, lo borra y muestra el login', async () => {
    guardarToken('token-vencido')
    obtenerSesion.mockRejectedValue(Object.assign(new Error('401'), { status: 401 }))
    mostrar()

    expect(await screen.findByRole('heading', { name: 'Inicia sesión' })).toBeInTheDocument()
    expect(leerToken()).toBeNull()
  })

  it('cerrar sesión borra el token y vuelve al login', async () => {
    guardarToken('token-guardado')
    obtenerSesion.mockResolvedValue(adminCon('SUPERADMIN'))
    mostrar()

    await screen.findByText('Pantalla de inicio')
    await userEvent.click(screen.getByRole('button', { name: 'Cerrar sesión' }))

    expect(screen.getByRole('heading', { name: 'Inicia sesión' })).toBeInTheDocument()
    expect(leerToken()).toBeNull()
  })

  it('si la sesión vence mientras trabaja, vuelve al login con un aviso', async () => {
    guardarToken('token-guardado')
    obtenerSesion.mockResolvedValue(adminCon('SUPERADMIN'))
    mostrar()

    await screen.findByText('Pantalla de inicio')
    act(() => window.dispatchEvent(new Event(EVENTO_SESION_VENCIDA)))

    expect(screen.getByRole('heading', { name: 'Inicia sesión' })).toBeInTheDocument()
    expect(screen.getByText('Tu sesión venció. Vuelve a iniciar sesión.')).toBeInTheDocument()
  })

  it('muestra cuántos contenidos esperan revisión junto a la opción del menú', async () => {
    listarDudosos.mockResolvedValue([{ id: 1 }, { id: 2 }, { id: 3 }])
    guardarToken('token-guardado')
    obtenerSesion.mockResolvedValue(adminCon('MODERADOR'))
    mostrar()

    expect(await screen.findByLabelText('3 pendientes')).toBeInTheDocument()
  })

  it('FINANZAS no ve la revisión, así que no se piden los pendientes', async () => {
    guardarToken('token-guardado')
    obtenerSesion.mockResolvedValue(adminCon('FINANZAS'))
    mostrar()

    await screen.findByText('Pantalla de inicio')
    expect(listarDudosos).not.toHaveBeenCalled()
  })

  it('cada pantalla tiene su dirección y una dirección desconocida muestra la página 404', async () => {
    guardarToken('token-valido')
    obtenerSesion.mockResolvedValue(adminCon('SUPERADMIN'))
    listarDudosos.mockResolvedValue([])
    render(
      <SesionProvider>
        <App />
      </SesionProvider>,
    )
    await screen.findByText('Pantalla de inicio')
    // La dirección cambia en un efecto después de pintar: se espera a que llegue (en el CI a veces tarda un instante).
    await waitFor(() => expect(window.location.pathname).toBe('/inicio'))

    await userEvent.click(screen.getByRole('button', { name: 'Auditoría' }))
    expect(window.location.pathname).toBe('/auditoria')
    expect(await screen.findByText('Pantalla de auditoría')).toBeInTheDocument()

    act(() => {
      window.history.pushState({}, '', '/no-existe')
      window.dispatchEvent(new PopStateEvent('popstate'))
    })
    expect(await screen.findByText('No encontramos esta página')).toBeInTheDocument()
  })

  it('el moderador no entra por URL a una pantalla de otro rol', async () => {
    window.history.replaceState({}, '', '/panel-financiero')
    guardarToken('token-valido')
    obtenerSesion.mockResolvedValue(adminCon('MODERADOR'))
    listarDudosos.mockResolvedValue([])
    render(
      <SesionProvider>
        <App />
      </SesionProvider>,
    )

    expect(await screen.findByText('No encontramos esta página')).toBeInTheDocument()
    expect(screen.queryByText('Pantalla financiera')).toBeNull()
  })
})
