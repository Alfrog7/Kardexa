import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '@shared/api/http'
import { SessionProvider } from '@shared/auth/SessionProvider'
import { closeShift, getCurrentShift, listCountProducts, openShift, type Shift } from '../api/shifts'
import { ShiftPage } from './ShiftPage'

vi.mock('../api/shifts', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../api/shifts')>()),
  getCurrentShift: vi.fn(),
  openShift: vi.fn(),
  closeShift: vi.fn(),
  listCountProducts: vi.fn(),
}))

const open: Shift = {
  id: 't1',
  siteId: 's1',
  responsibleId: 'u1',
  responsible: 'Luis Soto',
  status: 'OPEN',
  openedAt: '2026-11-15T12:00:00Z',
  closedAt: null,
  registeredSales: null,
  countedSales: null,
  expectedAmount: null,
  note: null,
  items: [],
}

const closed: Shift = {
  ...open,
  status: 'CLOSED',
  closedAt: '2026-11-15T17:00:00Z',
  registeredSales: 12,
  countedSales: 17.5,
  expectedAmount: 29.5,
  items: [
    { productId: 'pan', productName: 'Pan francés', openingStock: '40.000', leftover: '5.000', soldByCount: '35.000', unitPrice: 0.5 },
  ],
}

const products = [
  { id: 'pan', code: 'PAN-1', name: 'Pan francés', salePrice: '0.50', stock: '40.000' },
  { id: 'boc', code: 'BOC-1', name: 'Bocadillo', salePrice: '2.00', stock: '10.000' },
]

const luis = { id: 'u1', fullName: 'Luis Soto', email: 'luis@correo.com', role: 'EMPLOYEE' as const, active: true }
const marta = { ...luis, id: 'u2', fullName: 'Marta Díaz', email: 'marta@correo.com' }

function renderPage(user = luis) {
  render(
    <SessionProvider initialSession={{ token: 't', user }}>
      <ShiftPage />
    </SessionProvider>,
  )
}

describe('ShiftPage', () => {
  beforeEach(() => {
    vi.mocked(getCurrentShift).mockResolvedValue(null)
    vi.mocked(listCountProducts).mockResolvedValue(products)
    vi.mocked(openShift).mockResolvedValue(open)
    vi.mocked(closeShift).mockResolvedValue(closed)
  })

  it('sin turno abierto ofrece marcar el ingreso y lo abre', async () => {
    renderPage()

    fireEvent.click(await screen.findByRole('button', { name: 'Marcar ingreso' }))

    expect(await screen.findByText('Turno abierto')).toBeInTheDocument()
    expect(openShift).toHaveBeenCalledTimes(1)
    expect(screen.getByText(/Luis Soto/)).toBeInTheDocument()
    expect(screen.getByLabelText(/Pan francés/)).toBeInTheDocument()
  })

  it('con el turno abierto pide contar cada producto antes de cerrar', async () => {
    vi.mocked(getCurrentShift).mockResolvedValue(open)
    renderPage()
    fireEvent.change(await screen.findByLabelText(/Pan francés/), { target: { value: '5' } })

    fireEvent.click(screen.getByRole('button', { name: 'Cerrar turno' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Revisa «Bocadillo»')
    expect(closeShift).not.toHaveBeenCalled()
  })

  it('cierra con el conteo (acepta 0 y coma decimal) y muestra lo que debe haber en caja', async () => {
    vi.mocked(getCurrentShift).mockResolvedValue(open)
    renderPage()
    fireEvent.change(await screen.findByLabelText(/Pan francés/), { target: { value: '5' } })
    fireEvent.change(screen.getByLabelText(/Bocadillo/), { target: { value: '0,5' } })

    fireEvent.click(screen.getByRole('button', { name: 'Cerrar turno' }))
    expect(closeShift).not.toHaveBeenCalled()
    fireEvent.click(await screen.findByRole('button', { name: 'Sí, cerrar turno' }))

    await waitFor(() =>
      expect(closeShift).toHaveBeenCalledWith('t1', [
        { productId: 'pan', leftover: '5' },
        { productId: 'boc', leftover: '0.5' },
      ]),
    )
    expect(await screen.findByText('Debes tener en caja')).toBeInTheDocument()
    expect(screen.getByText('S/ 29.50')).toBeInTheDocument()
    expect(screen.getByText('35 vendidos')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Marcar nuevo ingreso' })).toBeInTheDocument()
  })

  it('muestra el error de la API al cerrar y conserva el turno abierto', async () => {
    vi.mocked(getCurrentShift).mockResolvedValue(open)
    vi.mocked(closeShift).mockRejectedValue(
      new ApiError({ title: 'Sobrante mayor', status: 422, detail: 'El sobrante de «Pan francés» supera el stock.' }),
    )
    renderPage()
    fireEvent.change(await screen.findByLabelText(/Pan francés/), { target: { value: '5' } })
    fireEvent.change(screen.getByLabelText(/Bocadillo/), { target: { value: '0' } })
    fireEvent.click(screen.getByRole('button', { name: 'Cerrar turno' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Sí, cerrar turno' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('supera el stock')
    expect(screen.getByText('Turno abierto')).toBeInTheDocument()
  })

  it('«Todavía no» vuelve al conteo sin cerrar el turno', async () => {
    vi.mocked(getCurrentShift).mockResolvedValue(open)
    renderPage()
    fireEvent.change(await screen.findByLabelText(/Pan francés/), { target: { value: '5' } })
    fireEvent.change(screen.getByLabelText(/Bocadillo/), { target: { value: '0' } })
    fireEvent.click(screen.getByRole('button', { name: 'Cerrar turno' }))

    fireEvent.click(await screen.findByRole('button', { name: 'Todavía no' }))

    expect(closeShift).not.toHaveBeenCalled()
    expect(screen.queryByRole('button', { name: 'Sí, cerrar turno' })).not.toBeInTheDocument()
  })

  it('muestra lo vendido al contar y avisa si sobra más de lo registrado', async () => {
    vi.mocked(getCurrentShift).mockResolvedValue(open)
    renderPage()
    fireEvent.change(await screen.findByLabelText(/Pan francés/), { target: { value: '5' } })

    expect(screen.getByText(/Vendiste 35/)).toHaveTextContent('S/ 17.50')

    fireEvent.change(screen.getByLabelText(/Bocadillo/), { target: { value: '11' } })
    expect(screen.getByText('No puede ser más de lo registrado.')).toBeInTheDocument()
  })

  it('si el turno es de otra persona, una trabajadora lo ve pero no puede cerrarlo', async () => {
    vi.mocked(getCurrentShift).mockResolvedValue(open)
    renderPage(marta)

    expect(await screen.findByText(/Solo esa persona o un administrador pueden cerrarlo/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Cerrar turno' })).not.toBeInTheDocument()
  })

  it('avisa si ya hay un turno abierto en la sede al marcar el ingreso', async () => {
    vi.mocked(openShift).mockRejectedValue(
      new ApiError({ title: 'Turno ya abierto', status: 409, detail: 'Ya hay un turno abierto en esta sede.' }),
    )
    renderPage()

    fireEvent.click(await screen.findByRole('button', { name: 'Marcar ingreso' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Ya hay un turno abierto')
  })
})
