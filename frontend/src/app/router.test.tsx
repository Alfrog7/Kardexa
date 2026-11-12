import { fireEvent, render, screen } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { type Session } from '@shared/auth/session'
import { SessionProvider } from '@shared/auth/SessionProvider'
import { appRoutes } from './router'

const adminSession: Session = {
  token: 'token-de-prueba',
  user: { id: '1', fullName: 'Ana Pérez', email: 'ana@correo.com', role: 'ADMIN', active: true },
}

function renderAt(path: string, session: Session | null = adminSession) {
  const router = createMemoryRouter(appRoutes, { initialEntries: [path] })
  render(
    <SessionProvider initialSession={session ?? undefined}>
      <RouterProvider router={router} />
    </SessionProvider>,
  )
}

describe('rutas del panel', () => {
  it('muestra el inicio dentro del layout con la navegación de módulos', () => {
    renderAt('/')

    expect(screen.getByRole('heading', { level: 1, name: 'Inicio' })).toBeInTheDocument()
    const nav = screen.getByRole('navigation', { name: 'Módulos' })
    for (const label of ['Inicio', 'Ventas', 'Productos', 'Categorías', 'Inventario', 'Compras', 'Proveedores']) {
      expect(nav).toHaveTextContent(label)
    }
  })

  it('/ventas muestra la pantalla de ventas con el buscador de productos', () => {
    renderAt('/ventas')

    expect(screen.getByRole('heading', { level: 1, name: 'Ventas' })).toBeInTheDocument()
    expect(screen.getByLabelText('Agregar producto')).toBeInTheDocument()
  })

  it('/inventario muestra la pantalla de inventario', () => {
    renderAt('/inventario')

    expect(screen.getByRole('heading', { level: 1, name: 'Inventario' })).toBeInTheDocument()
    expect(screen.getByLabelText('Producto')).toBeInTheDocument()
  })

  it('/compras es solo del administrador', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => Response.json({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 })))
    renderAt('/compras')
    expect(screen.getByRole('heading', { level: 1, name: 'Compras' })).toBeInTheDocument()
    expect(await screen.findByText('No hay compras en estas fechas.')).toBeInTheDocument()
    vi.unstubAllGlobals()
  })

  it('un empleado que entra a /compras vuelve al inicio y no ve el enlace', () => {
    renderAt('/compras', { ...adminSession, user: { ...adminSession.user, role: 'EMPLOYEE' } })

    expect(screen.getByRole('heading', { level: 1, name: 'Inicio' })).toBeInTheDocument()
    expect(screen.getByRole('navigation', { name: 'Módulos' })).not.toHaveTextContent('Compras')
  })

  it('/productos muestra la pantalla de productos', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => Response.json({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 })))
    renderAt('/productos')

    expect(screen.getByRole('heading', { level: 1, name: 'Productos' })).toBeInTheDocument()
    expect(await screen.findByText('No hay productos con estos filtros.')).toBeInTheDocument()
    vi.unstubAllGlobals()
  })

  it('marca como activo el módulo actual', () => {
    renderAt('/ventas')

    expect(screen.getByRole('link', { name: 'Ventas' })).toHaveAttribute('aria-current', 'page')
    expect(screen.getByRole('link', { name: 'Inicio' })).not.toHaveAttribute('aria-current')
  })

  it('sin sesión, cualquier ruta del panel lleva a /ingresar', () => {
    renderAt('/productos', null)

    expect(screen.getByRole('heading', { level: 1, name: 'Ingresa a tu negocio' })).toBeInTheDocument()
    expect(screen.queryByRole('navigation', { name: 'Módulos' })).not.toBeInTheDocument()
  })

  it('muestra el nombre del usuario y un botón para cerrar sesión', () => {
    renderAt('/')

    expect(screen.getByText('Ana Pérez')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Menú de la cuenta' }))
    expect(screen.getByRole('button', { name: 'Cerrar sesión' })).toBeInTheDocument()
  })

  it('muestra el inicio de sesión fuera del layout, con campos accesibles', () => {
    renderAt('/ingresar', null)

    expect(screen.queryByRole('navigation', { name: 'Módulos' })).not.toBeInTheDocument()
    expect(screen.getByLabelText('Correo')).toHaveAttribute('type', 'email')
    expect(screen.getByLabelText('Contraseña')).toHaveAttribute('type', 'password')
    expect(screen.getByRole('button', { name: 'Ingresar' })).toBeEnabled()
  })

  it('responde a rutas desconocidas con una página 404', () => {
    renderAt('/no-existe')

    expect(screen.getByRole('heading', { name: 'Página no encontrada' })).toBeInTheDocument()
  })
})
