import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { appRoutes } from '@app/router'
import { setAccessToken } from '@shared/api/http'
import { type Session } from '@shared/auth/session'
import { SessionProvider } from '@shared/auth/SessionProvider'

const employee: Session = {
  token: 'token-empleado',
  user: { id: '2', fullName: 'Luis Gómez', email: 'luis@correo.com', role: 'EMPLOYEE', active: true },
}

function renderApp(path: string, session?: Session) {
  const router = createMemoryRouter(appRoutes, { initialEntries: [path] })
  render(
    <SessionProvider initialSession={session}>
      <RouterProvider router={router} />
    </SessionProvider>,
  )
}

afterEach(() => {
  vi.unstubAllGlobals()
  setAccessToken(undefined)
})

describe('ingreso y sesión (HU-02)', () => {
  it('con credenciales correctas entra al panel y envía el token en las peticiones', async () => {
    const fetchMock = vi.fn(async (url: string) =>
      url.endsWith('/auth/login')
        ? Response.json({
            accessToken: 'abc.def.ghi',
            tokenType: 'Bearer',
            expiresIn: 3600,
            user: { id: '1', fullName: 'Ana Pérez', email: 'ana@correo.com', role: 'ADMIN', active: true },
          })
        : Response.json({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }),
    )
    vi.stubGlobal('fetch', fetchMock)
    renderApp('/productos')

    fireEvent.change(screen.getByLabelText('Correo'), { target: { value: 'ana@correo.com' } })
    fireEvent.change(screen.getByLabelText('Contraseña'), { target: { value: 'secreta123' } })
    fireEvent.click(screen.getByRole('button', { name: 'Ingresar' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Productos' })).toBeInTheDocument()
    expect(screen.getByText('Ana Pérez')).toBeInTheDocument()
    await waitFor(() => {
      const calls = fetchMock.mock.calls as unknown as [string, RequestInit][]
      const productCall = calls.find(([url]) => url.includes('/products'))
      expect(productCall).toBeDefined()
      expect(new Headers(productCall![1].headers).get('Authorization')).toBe('Bearer abc.def.ghi')
    })
  })

  it('con credenciales incorrectas muestra el mensaje genérico', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () =>
        Response.json(
          { title: 'Credenciales inválidas', status: 401, detail: 'Correo o contraseña incorrectos.' },
          { status: 401, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    )
    renderApp('/ingresar')

    fireEvent.change(screen.getByLabelText('Correo'), { target: { value: 'ana@correo.com' } })
    fireEvent.change(screen.getByLabelText('Contraseña'), { target: { value: 'equivocada1' } })
    fireEvent.click(screen.getByRole('button', { name: 'Ingresar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Correo o contraseña incorrectos.')
  })

  it('un token vencido (401 con sesión) vuelve al ingreso con un aviso', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () =>
        Response.json(
          { title: 'No autenticado', status: 401 },
          { status: 401, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    )
    renderApp('/productos', employee)

    expect(await screen.findByText(/Tu sesión venció/)).toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 1, name: 'Ingresa a tu negocio' })).toBeInTheDocument()
  })

  it('al cerrar sesión se descarta el token y se vuelve al ingreso', async () => {
    renderApp('/ventas', employee)

    fireEvent.click(screen.getByRole('button', { name: 'Cerrar sesión' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Ingresa a tu negocio' })).toBeInTheDocument()
  })

  it('un EMPLEADO no ve las acciones de administración', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => Response.json({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 })),
    )
    renderApp('/productos', employee)

    expect(await screen.findByRole('heading', { level: 1, name: 'Productos' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Nuevo producto' })).not.toBeInTheDocument()
  })
})
