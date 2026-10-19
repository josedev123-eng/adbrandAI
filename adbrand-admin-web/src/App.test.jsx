// Login de administradores: sesión guardada, menú según el rol y cerrar sesión.
import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'
import { SesionProvider } from './app/context/SesionProvider'
import { obtenerSesion } from './features/auth/models/authApi'
import { EVENTO_SESION_VENCIDA } from './shared/services/apiClient'
import { guardarToken, leerToken } from './shared/services/sesionToken'

vi.mock('./features/auth/models/authApi', () => ({
  iniciarSesion: vi.fn(),
  obtenerSesion: vi.fn(),
}))
// Las pantallas reales llaman a Django; aquí basta saber cuál se muestra.
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
  return screen.queryAllByRole('button').map((b) => b.textContent).filter((t) => t !== 'Cerrar sesión')
}

describe('App (login y menú por rol)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
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

    expect(screen.getByText('Cargando tu sesión…')).toBeInTheDocument()
    expect(await screen.findByText('Pantalla de revisión')).toBeInTheDocument()
    expect(screen.getByText('Superadministrador')).toBeInTheDocument()
    expect(opcionesDelMenu()).toEqual([
      'Revisión de contenido',
      'Historial de contenidos',
      'Administradores',
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

    expect(await screen.findByText('Pantalla de revisión')).toBeInTheDocument()
    expect(opcionesDelMenu()).toEqual(['Revisión de contenido', 'Historial de contenidos'])
  })

  it('FINANZAS entra directo al panel financiero', async () => {
    guardarToken('token-guardado')
    obtenerSesion.mockResolvedValue(adminCon('FINANZAS'))
    mostrar()

    expect(await screen.findByText('Pantalla financiera')).toBeInTheDocument()
    expect(opcionesDelMenu()).toEqual(['Panel financiero'])
  })

  it('TECNICO entra directo al consumo de IA (HU 17)', async () => {
    guardarToken('token-guardado')
    obtenerSesion.mockResolvedValue(adminCon('TECNICO'))
    mostrar()

    expect(await screen.findByText('Pantalla de consumo de IA')).toBeInTheDocument()
    expect(opcionesDelMenu()).toEqual(['Consumo de IA', 'Historial de IA', 'Estado del servidor'])
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

    await screen.findByText('Pantalla de revisión')
    await userEvent.click(screen.getByRole('button', { name: 'Cerrar sesión' }))

    expect(screen.getByRole('heading', { name: 'Inicia sesión' })).toBeInTheDocument()
    expect(leerToken()).toBeNull()
  })

  it('si la sesión vence mientras trabaja, vuelve al login con un aviso', async () => {
    guardarToken('token-guardado')
    obtenerSesion.mockResolvedValue(adminCon('SUPERADMIN'))
    mostrar()

    await screen.findByText('Pantalla de revisión')
    act(() => window.dispatchEvent(new Event(EVENTO_SESION_VENCIDA)))

    expect(screen.getByRole('heading', { name: 'Inicia sesión' })).toBeInTheDocument()
    expect(screen.getByText('Tu sesión venció. Vuelve a iniciar sesión.')).toBeInTheDocument()
  })
})
