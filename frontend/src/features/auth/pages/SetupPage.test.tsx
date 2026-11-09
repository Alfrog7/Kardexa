import { fireEvent, render, screen } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { appRoutes } from '@app/router'
import { setAccessToken } from '@shared/api/http'
import { SessionProvider } from '@shared/auth/SessionProvider'

const problem = { headers: { 'Content-Type': 'application/problem+json' } }

function renderSetup() {
  const router = createMemoryRouter(appRoutes, { initialEntries: ['/registro-inicial'] })
  render(
    <SessionProvider>
      <RouterProvider router={router} />
    </SessionProvider>,
  )
}

function fill(password: string) {
  fireEvent.change(screen.getByLabelText('Nombre completo'), { target: { value: 'Ana Pérez' } })
  fireEvent.change(screen.getByLabelText('Correo'), { target: { value: 'ana@correo.com' } })
  fireEvent.change(screen.getByLabelText('Contraseña'), { target: { value: password } })
  fireEvent.click(screen.getByRole('button', { name: 'Crear administrador' }))
}

afterEach(() => {
  vi.unstubAllGlobals()
  setAccessToken(undefined)
})

describe('registro inicial (HU-01)', () => {
  it('crea al administrador, abre su sesión y entra al panel', async () => {
    const admin = { id: '1', fullName: 'Ana Pérez', email: 'ana@correo.com', role: 'ADMIN', active: true }
    vi.stubGlobal(
      'fetch',
      vi.fn(async (url: string) => {
        if (url.endsWith('/auth/setup')) return Response.json(admin, { status: 201 })
        if (url.endsWith('/auth/login')) {
          return Response.json({ accessToken: 'abc.def.ghi', tokenType: 'Bearer', expiresIn: 3600, user: admin })
        }
        return Response.json({})
      }),
    )
    renderSetup()

    fill('secreta123')

    expect(await screen.findByRole('heading', { level: 1, name: 'Inicio' })).toBeInTheDocument()
    expect(screen.getByText('Ana Pérez')).toBeInTheDocument()
  })

  it('una contraseña corta muestra el error junto al campo', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () =>
        Response.json(
          {
            title: 'Datos inválidos',
            status: 400,
            detail: 'Revisa los campos marcados.',
            errors: [{ field: 'password', message: 'Debe tener entre 8 y 72 caracteres.' }],
          },
          { status: 400, ...problem },
        ),
      ),
    )
    renderSetup()

    fill('corta')

    expect(await screen.findByText('Debe tener entre 8 y 72 caracteres.')).toBeInTheDocument()
    expect(screen.getByLabelText('Contraseña')).toHaveAttribute('aria-invalid', 'true')
  })

  it('si ya hay un usuario muestra el mensaje y un enlace al ingreso', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () =>
        Response.json({ title: 'Registro inicial cerrado', status: 403, rule: 'RF-01' }, { status: 403, ...problem }),
      ),
    )
    renderSetup()

    fill('secreta123')

    expect(await screen.findByRole('heading', { name: 'El registro inicial ya se hizo' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ir a ingresar' })).toHaveAttribute('href', '/ingresar')
  })
})
