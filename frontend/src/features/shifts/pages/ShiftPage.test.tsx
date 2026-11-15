import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
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
  payments: null,
  receivedAmount: null,
  difference: null,
  items: [
    { productId: 'pan', productName: 'Pan francés', openingStock: '40.000', leftover: null, soldByCount: null, unitPrice: null },
    { productId: 'boc', productName: 'Bocadillo', openingStock: '10.000', leftover: null, soldByCount: null, unitPrice: null },
  ],
}

const closed: Shift = {
  ...open,
  status: 'CLOSED',
  closedAt: '2026-11-15T17:00:00Z',
  registeredSales: 12,
  countedSales: 17.5,
  expectedAmount: 29.5,
  payments: { cash: 20, yape: 5.5, plin: 0, transfer: 2 },
  receivedAmount: 27.5,
  difference: -2,
  items: [
    { productId: 'pan', productName: 'Pan francés', openingStock: '40.000', leftover: '5.000', soldByCount: '35.000', unitPrice: 0.5 },
  ],
}

const products = [
  { id: 'pan', code: 'PAN-1', name: 'Pan francés', salePrice: '0.50', stock: '40.000' },
  { id: 'boc', code: 'BOC-1', name: 'Bocadillo', salePrice: '2.00', stock: '10.000' },
  { id: 'emp', code: 'EMP-1', name: 'Empanada', salePrice: '3.00', stock: '60.000' },
]

const luis = { id: 'u1', fullName: 'Luis Soto', email: 'luis@correo.com', role: 'EMPLOYEE' as const, active: true }
const marta = { ...luis, id: 'u2', fullName: 'Marta Díaz', email: 'marta@correo.com' }

function fillPayments(cash = '20', yape = '5,50') {
  fireEvent.change(screen.getByLabelText('Efectivo'), { target: { value: cash } })
  fireEvent.change(screen.getByLabelText('Yape'), { target: { value: yape } })
  fireEvent.change(screen.getByLabelText('Plin'), { target: { value: '0' } })
  fireEvent.change(screen.getByLabelText('Transferencia'), { target: { value: '2' } })
}

function renderPage(user = luis) {
  render(
    <MemoryRouter>
      <SessionProvider initialSession={{ token: 't', user }}>
        <ShiftPage />
      </SessionProvider>
    </MemoryRouter>,
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
    expect(screen.getByLabelText(/^Pan francés/)).toBeInTheDocument()
  })

  it('con el turno abierto pide contar cada producto antes de cerrar', async () => {
    vi.mocked(getCurrentShift).mockResolvedValue(open)
    renderPage()
    fireEvent.change(await screen.findByLabelText(/^Pan francés/), { target: { value: '5' } })

    fireEvent.click(screen.getByRole('button', { name: 'Continuar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Revisa «Bocadillo»')
    expect(closeShift).not.toHaveBeenCalled()
  })

  it('cierra con el conteo (acepta 0 y coma decimal) y muestra lo que debe haber en caja', async () => {
    vi.mocked(getCurrentShift).mockResolvedValue(open)
    renderPage()
    fireEvent.change(await screen.findByLabelText(/^Pan francés/), { target: { value: '5' } })
    fireEvent.change(screen.getByLabelText(/^Bocadillo/), { target: { value: '0,5' } })

    fireEvent.click(screen.getByRole('button', { name: 'Continuar' }))
    fillPayments()
    fireEvent.click(await screen.findByRole('button', { name: 'Cerrar turno' }))
    expect(closeShift).not.toHaveBeenCalled()
    fireEvent.click(await screen.findByRole('button', { name: 'Sí, cerrar turno' }))

    await waitFor(() =>
      expect(closeShift).toHaveBeenCalledWith(
        't1',
        [
          { productId: 'pan', leftover: '5' },
          { productId: 'boc', leftover: '0.5' },
        ],
        { cash: 20, yape: 5.5, plin: 0, transfer: 2 },
        '',
      ),
    )
    expect(await screen.findByText('Debes tener en caja')).toBeInTheDocument()
    expect(screen.getByText('S/ 29.50')).toBeInTheDocument()
    expect(screen.getByText('Total recibido')).toBeInTheDocument()
    expect(screen.getByRole('alert')).toHaveTextContent('Falta S/ 2.00')
    expect(screen.getByText('35 vendidos')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Marcar nuevo ingreso' })).toBeInTheDocument()
  })

  it('muestra el error de la API al cerrar y conserva el turno abierto', async () => {
    vi.mocked(getCurrentShift).mockResolvedValue(open)
    vi.mocked(closeShift).mockRejectedValue(
      new ApiError({ title: 'Sobrante mayor', status: 422, detail: 'El sobrante de «Pan francés» supera el stock.' }),
    )
    renderPage()
    fireEvent.change(await screen.findByLabelText(/^Pan francés/), { target: { value: '5' } })
    fireEvent.change(screen.getByLabelText(/^Bocadillo/), { target: { value: '0' } })
    fireEvent.click(screen.getByRole('button', { name: 'Continuar' }))
    fillPayments()
    fireEvent.click(await screen.findByRole('button', { name: 'Cerrar turno' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Sí, cerrar turno' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('supera el stock')
    expect(screen.getByText('Turno abierto')).toBeInTheDocument()
  })

  it('«Todavía no» vuelve al conteo sin cerrar el turno', async () => {
    vi.mocked(getCurrentShift).mockResolvedValue(open)
    renderPage()
    fireEvent.change(await screen.findByLabelText(/^Pan francés/), { target: { value: '5' } })
    fireEvent.change(screen.getByLabelText(/^Bocadillo/), { target: { value: '0' } })
    fireEvent.click(screen.getByRole('button', { name: 'Continuar' }))
    fillPayments()
    fireEvent.click(await screen.findByRole('button', { name: 'Cerrar turno' }))

    fireEvent.click(await screen.findByRole('button', { name: 'Todavía no' }))

    expect(closeShift).not.toHaveBeenCalled()
    expect(screen.queryByRole('button', { name: 'Sí, cerrar turno' })).not.toBeInTheDocument()
  })

  it('muestra lo vendido al contar y avisa si sobra más de lo registrado', async () => {
    vi.mocked(getCurrentShift).mockResolvedValue(open)
    renderPage()
    fireEvent.change(await screen.findByLabelText(/^Pan francés/), { target: { value: '5' } })

    expect(screen.getByText(/Vendiste 35/)).toHaveTextContent('S/ 17.50')

    fireEvent.change(screen.getByLabelText(/^Bocadillo/), { target: { value: '11' } })
    expect(screen.getByText('No puede ser más de lo registrado.')).toBeInTheDocument()
  })

  it('si el turno es de otra persona, una trabajadora lo ve pero no puede cerrarlo', async () => {
    vi.mocked(getCurrentShift).mockResolvedValue(open)
    renderPage(marta)

    expect(await screen.findByText(/Solo esa persona o un administrador pueden cerrarlo/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Continuar' })).not.toBeInTheDocument()
  })

  it('no pasa a cerrar si falta un monto: el error nombra el medio de pago', async () => {
    vi.mocked(getCurrentShift).mockResolvedValue(open)
    renderPage()
    fireEvent.change(await screen.findByLabelText(/^Pan francés/), { target: { value: '5' } })
    fireEvent.change(screen.getByLabelText(/^Bocadillo/), { target: { value: '0' } })
    fireEvent.click(screen.getByRole('button', { name: 'Continuar' }))
    fireEvent.change(await screen.findByLabelText('Efectivo'), { target: { value: '20' } })

    fireEvent.click(screen.getByRole('button', { name: 'Cerrar turno' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Revisa «Yape»')
    expect(screen.queryByRole('button', { name: 'Sí, cerrar turno' })).not.toBeInTheDocument()
  })

  it('avisa si ya hay un turno abierto en la sede al marcar el ingreso', async () => {
    vi.mocked(openShift).mockRejectedValue(
      new ApiError({ title: 'Turno ya abierto', status: 409, detail: 'Ya hay un turno abierto en esta sede.' }),
    )
    renderPage()

    fireEvent.click(await screen.findByRole('button', { name: 'Marcar ingreso' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Ya hay un turno abierto')
  })

  it('el conteo trae solo los productos del turno; uno que no salió en la lista se busca y se agrega', async () => {
    vi.mocked(getCurrentShift).mockResolvedValue(open)
    renderPage()
    await screen.findByLabelText(/^Pan francés/)
    expect(screen.queryByLabelText(/^Empanada/)).not.toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Buscar o agregar producto'), { target: { value: 'empa' } })
    fireEvent.click(screen.getByRole('button', { name: 'Agregar Empanada' }))

    expect(await screen.findByLabelText(/^Empanada/)).toBeInTheDocument()
    expect(screen.getByLabelText('Buscar o agregar producto')).toHaveValue('')
  })

  it('el buscador filtra la lista del turno y un producto que no se cuenta se quita', async () => {
    vi.mocked(getCurrentShift).mockResolvedValue(open)
    renderPage()
    await screen.findByLabelText(/^Pan francés/)

    fireEvent.change(screen.getByLabelText('Buscar o agregar producto'), { target: { value: 'boc' } })
    expect(screen.queryByLabelText(/^Pan francés/)).not.toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('Buscar o agregar producto'), { target: { value: '' } })
    fireEvent.click(screen.getByRole('button', { name: 'No contar Bocadillo' }))

    expect(screen.queryByLabelText(/^Bocadillo/)).not.toBeInTheDocument()
    fireEvent.change(screen.getByLabelText(/^Pan francés/), { target: { value: '5' } })
    fireEvent.click(screen.getByRole('button', { name: 'Continuar' }))
    expect(await screen.findByText(/Paso 2 de 2/)).toBeInTheDocument()
  })
})
