import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '@shared/api/http'
import { SessionContext, type Role } from '@shared/auth/session'
import { formatMoney } from '@shared/lib/format'
import { getDashboard, type Dashboard } from '../api/dashboard'
import { DashboardPage } from './DashboardPage'

vi.mock('../api/dashboard', () => ({ getDashboard: vi.fn() }))

function dashboard(from: string, to: string, count = 3): Dashboard {
  return {
    from,
    to,
    summary: { total: count ? '22.75' : '0.00', count, averageTicket: count ? '7.58' : '0.00', criticalCount: 2 },
    salesByDay: [],
    topProducts: [],
    criticalStock: [],
  }
}

function renderAt(path: string, role: Role = 'ADMIN') {
  const router = createMemoryRouter([{ path: '/', element: <DashboardPage /> }], { initialEntries: [path] })
  const user = { id: '1', fullName: 'Ana Pérez', email: 'ana@correo.com', role, active: true }
  render(
    <SessionContext.Provider value={{ user, expired: false, start: vi.fn(), end: vi.fn() }}>
      <RouterProvider router={router} />
    </SessionContext.Provider>,
  )
  return router
}

/** `Intl` separa «S/» del monto con un espacio no separable. */
const text = (value: string) => value.replace(/\s/g, ' ')

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    // 2026-11-15 02:00 UTC todavía es 14 de noviembre en Lima.
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date('2026-11-15T02:00:00Z'))
    vi.mocked(getDashboard).mockImplementation(async (from, to) => dashboard(from, to))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('el ADMIN ve las cuatro tarjetas del rango de la URL con sus accesos', async () => {
    renderAt('/?desde=2026-11-01&hasta=2026-11-03')

    const cards = await screen.findByRole('list', { name: 'Indicadores' })
    expect(getDashboard).toHaveBeenCalledWith('2026-11-01', '2026-11-03')
    expect(text(cards.textContent ?? '')).toContain(`Total vendido${text(formatMoney('22.75'))}`)
    expect(cards).toHaveTextContent('Ventas3')
    expect(text(cards.textContent ?? '')).toContain(`Ticket promedio${text(formatMoney('7.58'))}`)
    expect(cards).toHaveTextContent('Stock crítico (hoy)2')
    expect(screen.getByRole('link', { name: 'Ver productos' })).toHaveAttribute('href', '/productos?critico=1')
    expect(screen.getByRole('link', { name: 'Ver historial' })).toHaveAttribute('href', '/ventas')
    expect(screen.getByText(/Del 1 de noviembre de 2026 al 3 de noviembre de 2026/)).toBeInTheDocument()
  })

  it('sin rango en la URL pide los últimos 30 días hasta hoy en Lima', async () => {
    renderAt('/')

    await screen.findByRole('list', { name: 'Indicadores' })
    expect(getDashboard).toHaveBeenCalledWith('2026-10-16', '2026-11-14')
    expect(screen.getByRole('button', { name: 'Últimos 30 días' })).toHaveAttribute('aria-pressed', 'true')
  })

  it('cambiar el rango vuelve a pedir los datos y lo guarda en la URL', async () => {
    const router = renderAt('/')
    await screen.findByRole('list', { name: 'Indicadores' })

    fireEvent.click(screen.getByRole('button', { name: 'Este mes' }))
    await waitFor(() => expect(getDashboard).toHaveBeenLastCalledWith('2026-11-01', '2026-11-14'))
    expect(router.state.location.search).toBe('?desde=2026-11-01&hasta=2026-11-14')

    fireEvent.change(screen.getByLabelText('Desde'), { target: { value: '2026-11-10' } })
    await waitFor(() => expect(getDashboard).toHaveBeenLastCalledWith('2026-11-10', '2026-11-14'))
    expect(router.state.location.search).toBe('?desde=2026-11-10&hasta=2026-11-14')
  })

  it('un rango invertido en la URL avisa sin llamar a la API', () => {
    renderAt('/?desde=2026-11-10&hasta=2026-11-01')

    expect(screen.getByRole('alert')).toHaveTextContent('La fecha inicial no puede ser posterior a la final.')
    expect(getDashboard).not.toHaveBeenCalled()
  })

  it('un rango sin ventas lo dice y muestra las tarjetas en 0', async () => {
    vi.mocked(getDashboard).mockImplementation(async (from, to) => dashboard(from, to, 0))
    renderAt('/?desde=2026-11-01&hasta=2026-11-01')

    expect(await screen.findByText(/No hubo ventas en este rango/)).toBeInTheDocument()
    expect(screen.getByRole('list', { name: 'Indicadores' })).toHaveTextContent('Ventas0')
  })

  it('si falla muestra el error y «Reintentar» vuelve a pedir', async () => {
    vi.mocked(getDashboard).mockRejectedValueOnce(
      new ApiError({ title: 'Bad Request', status: 400, detail: 'El rango puede tener como máximo 366 días.' }),
    )
    renderAt('/?desde=2025-01-01&hasta=2026-01-02')

    expect(await screen.findByRole('alert')).toHaveTextContent('El rango puede tener como máximo 366 días.')
    fireEvent.click(screen.getByRole('button', { name: 'Reintentar' }))
    expect(await screen.findByRole('list', { name: 'Indicadores' })).toBeInTheDocument()
    expect(getDashboard).toHaveBeenCalledTimes(2)
  })

  it('el EMPLEADO no pide indicadores y ve sus accesos rápidos', () => {
    renderAt('/', 'EMPLOYEE')

    const shortcuts = screen.getByRole('navigation', { name: 'Accesos rápidos' })
    expect(shortcuts).toHaveTextContent('Registrar venta')
    expect(shortcuts).toHaveTextContent('Productos')
    expect(shortcuts).toHaveTextContent('Inventario')
    expect(screen.queryByRole('list', { name: 'Indicadores' })).not.toBeInTheDocument()
    expect(getDashboard).not.toHaveBeenCalled()
  })
})
