import { fireEvent, render, screen, within } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { appRoutes } from '@app/router'
import { setAccessToken } from '@shared/api/http'
import type { Session } from '@shared/auth/session'
import { SessionProvider } from '@shared/auth/SessionProvider'

const problem = { headers: { 'Content-Type': 'application/problem+json' } }

const admin = { id: '1', fullName: 'Ana Pérez', email: 'ana@correo.com', role: 'ADMIN', active: true } as const
const employee = { id: '2', fullName: 'Luis Gómez', email: 'luis@correo.com', role: 'EMPLOYEE', active: true } as const

const adminSession: Session = { token: 't-admin', user: admin }
const employeeSession: Session = { token: 't-empleado', user: employee }

function page(content: unknown[]) {
  return Response.json({ content, page: 0, size: 100, totalElements: content.length, totalPages: 1 })
}

function renderAt(path: string, session: Session) {
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

describe('usuarios (HU-03)', () => {
  it('como ADMIN veo los usuarios, creo un empleado y lo desactivo', async () => {
    const created = { id: '3', fullName: 'Marta Ríos', email: 'marta@correo.com', role: 'EMPLOYEE', active: true }
    const calls: { url: string; method?: string; body?: string }[] = []
    vi.stubGlobal(
      'fetch',
      vi.fn(async (url: string, init?: RequestInit) => {
        calls.push({ url, method: init?.method, body: init?.body as string | undefined })
        if (init?.method === 'POST') return Response.json(created, { status: 201 })
        if (init?.method === 'PATCH') return Response.json({ ...created, active: false })
        return page([admin, employee])
      }),
    )
    renderAt('/usuarios', adminSession)

    expect(await screen.findByRole('button', { name: 'Luis Gómez' })).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Nuevo usuario' }))
    const drawer = await screen.findByRole('dialog')
    fireEvent.change(within(drawer).getByLabelText('Nombre completo'), { target: { value: 'Marta Ríos' } })
    fireEvent.change(within(drawer).getByLabelText('Correo'), { target: { value: 'marta@correo.com' } })
    fireEvent.change(within(drawer).getByLabelText('Contraseña inicial'), { target: { value: 'clave12345' } })
    fireEvent.click(within(drawer).getByRole('button', { name: 'Crear usuario' }))

    const toggle = await screen.findByRole('switch', { name: 'Marta Ríos activo' })
    expect(JSON.parse(calls.find((c) => c.method === 'POST')!.body!)).toMatchObject({ role: 'EMPLOYEE' })

    fireEvent.click(toggle)
    await vi.waitFor(() => expect(screen.getByRole('switch', { name: 'Marta Ríos activo' })).not.toBeChecked())
    expect(calls.find((c) => c.method === 'PATCH')!.url).toContain('/users/3')
  })

  it('RN-11: al desactivar al último administrador se explica por qué', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async (_url: string, init?: RequestInit) =>
        init?.method === 'PATCH'
          ? Response.json(
              {
                title: 'Último administrador',
                status: 422,
                rule: 'RN-11',
                detail: 'Debe quedar al menos un administrador activo: el último no puede desactivarse ni cambiar de rol.',
              },
              { status: 422, ...problem },
            )
          : page([admin]),
      ),
    )
    renderAt('/usuarios', adminSession)

    fireEvent.click(await screen.findByRole('switch', { name: 'Ana Pérez activo' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Debe quedar al menos un administrador activo')
    expect(screen.getByRole('switch', { name: 'Ana Pérez activo' })).toBeChecked()
  })

  it('un EMPLEADO no ve el enlace de usuarios y la ruta lo devuelve al inicio', () => {
    renderAt('/usuarios', employeeSession)

    expect(screen.getByRole('heading', { level: 1, name: 'Inicio' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Usuarios' })).not.toBeInTheDocument()
  })

  it('el ADMIN ve el enlace de usuarios en la navegación', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => page([admin])))
    renderAt('/', adminSession)

    expect(screen.getByRole('link', { name: 'Usuarios' })).toBeInTheDocument()
  })
})

describe('cambio de contraseña propia (HU-04)', () => {
  function open() {
    fireEvent.click(screen.getByRole('button', { name: 'Cambiar contraseña' }))
    return screen.findByRole('dialog')
  }

  function fill(dialog: HTMLElement, current: string, next: string) {
    fireEvent.change(within(dialog).getByLabelText('Contraseña actual'), { target: { value: current } })
    fireEvent.change(within(dialog).getByLabelText('Contraseña nueva'), { target: { value: next } })
    fireEvent.click(within(dialog).getByRole('button', { name: 'Cambiar contraseña' }))
  }

  it('con la actual correcta cambia la contraseña y avisa', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => new Response(null, { status: 204 })))
    renderAt('/ventas', employeeSession)

    fill(await open(), 'secreta123', 'nuevaclave99')

    expect(await screen.findByText('Contraseña actualizada.')).toBeInTheDocument()
  })

  it('con la actual incorrecta muestra el error junto al campo', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () =>
        Response.json(
          { title: 'Contraseña actual incorrecta', status: 422, rule: 'RF-05', detail: 'La contraseña actual no es correcta.' },
          { status: 422, ...problem },
        ),
      ),
    )
    renderAt('/ventas', employeeSession)

    fill(await open(), 'equivocada', 'nuevaclave99')

    expect(await screen.findByText('La contraseña actual no es correcta.')).toBeInTheDocument()
    expect(screen.getByLabelText('Contraseña actual')).toHaveAttribute('aria-invalid', 'true')
  })
})
