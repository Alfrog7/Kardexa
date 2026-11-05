import { render, screen } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { describe, expect, it } from 'vitest'
import { appRoutes } from './router'

function renderAt(path: string) {
  const router = createMemoryRouter(appRoutes, { initialEntries: [path] })
  render(<RouterProvider router={router} />)
}

describe('rutas del panel', () => {
  it('muestra el inicio dentro del layout con la navegación de módulos', () => {
    renderAt('/')

    expect(screen.getByRole('heading', { level: 1, name: 'Inicio' })).toBeInTheDocument()
    const nav = screen.getByRole('navigation', { name: 'Módulos' })
    for (const label of ['Inicio', 'Ventas', 'Productos', 'Inventario', 'Compras']) {
      expect(nav).toHaveTextContent(label)
    }
  })

  it.each([
    ['/ventas', 'Ventas', 'Fase 6'],
    ['/productos', 'Productos', 'Fase 4'],
    ['/inventario', 'Inventario', 'Fase 4'],
    ['/compras', 'Compras', 'Fase 5'],
  ])('%s muestra su pantalla y la fase en que se implementa', (path, title, phase) => {
    renderAt(path)

    expect(screen.getByRole('heading', { level: 1, name: title })).toBeInTheDocument()
    expect(screen.getByText(phase)).toBeInTheDocument()
  })

  it('marca como activo el módulo actual', () => {
    renderAt('/ventas')

    expect(screen.getByRole('link', { name: 'Ventas' })).toHaveAttribute('aria-current', 'page')
    expect(screen.getByRole('link', { name: 'Inicio' })).not.toHaveAttribute('aria-current')
  })

  it('muestra el inicio de sesión fuera del layout, con campos accesibles', () => {
    renderAt('/ingresar')

    expect(screen.queryByRole('navigation', { name: 'Módulos' })).not.toBeInTheDocument()
    expect(screen.getByLabelText('Correo')).toHaveAttribute('type', 'email')
    expect(screen.getByLabelText('Contraseña')).toHaveAttribute('type', 'password')
    expect(screen.getByRole('button', { name: 'Ingresar' })).toBeDisabled()
  })

  it('responde a rutas desconocidas con una página 404', () => {
    renderAt('/no-existe')

    expect(screen.getByRole('heading', { name: 'Página no encontrada' })).toBeInTheDocument()
  })
})
