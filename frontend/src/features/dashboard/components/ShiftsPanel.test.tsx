import { fireEvent, render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getShift, listShifts, type Dashboard, type ShiftRow } from '../api/dashboard'
import { ShiftsPanel } from './ShiftsPanel'

vi.mock('../api/dashboard', () => ({ listShifts: vi.fn(), getShift: vi.fn() }))

const dashboard: Dashboard = {
  from: '2026-11-15',
  to: '2026-11-15',
  summary: { total: '100.00', count: 8, averageTicket: '12.50', criticalCount: 0 },
  salesByDay: [],
  topProducts: [],
  criticalStock: [],
  payments: { cash: '60.00', yape: '30.00', plin: '0.00', transfer: '5.00', total: '95.00' },
  sites: [
    { siteId: 's1', name: 'Galeno', total: '70.00', count: 5, difference: '-5.00' },
    { siteId: 's2', name: 'Monserrate', total: '30.00', count: 3, difference: '0.00' },
  ],
}

const shift = (id: string, difference: number | null): ShiftRow => ({
  id,
  siteId: 's1',
  responsible: id === 'a' ? 'Luis Soto' : 'Marta Díaz',
  status: 'CLOSED',
  openedAt: '2026-11-15T13:00:00Z',
  closedAt: '2026-11-15T18:00:00Z',
  expectedAmount: 50,
  receivedAmount: 50 + (difference ?? 0),
  difference,
  payments: { cash: 40, yape: 5, plin: 0, transfer: 0 },
  note: null,
  items: [],
})

describe('ShiftsPanel (HU-24)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(listShifts).mockResolvedValue([shift('a', -5), shift('b', 0)])
  })

  it('muestra lo recibido por medio de pago y las ventas de cada sede', async () => {
    render(<ShiftsPanel dashboard={dashboard} />)

    expect(await screen.findByRole('list', { name: 'Ventas por sede' })).toHaveTextContent('Galeno')
    expect(screen.getByRole('list', { name: 'Ventas por sede' })).toHaveTextContent('Monserrate')
    expect(screen.getByText('Yape').nextSibling).toHaveTextContent('30.00')
    expect(screen.getByText('Total recibido').nextSibling).toHaveTextContent('95.00')
  })

  it('un turno con faltante se destaca y uno que cuadra no', async () => {
    render(<ShiftsPanel dashboard={dashboard} />)

    const list = await screen.findByRole('list', { name: 'Turnos' })
    const [missing, even] = Array.from(list.querySelectorAll('li.shifts__row'))
    expect(listShifts).toHaveBeenCalledWith('2026-11-15', '2026-11-15')
    expect(missing).toHaveClass('shifts__row--missing')
    expect(missing).toHaveTextContent('Falta')
    expect(even).not.toHaveClass('shifts__row--missing')
    expect(even).toHaveTextContent('Cuadra')
  })

  it('el detalle de un turno muestra lo vendido por producto', async () => {
    vi.mocked(getShift).mockResolvedValue({
      ...shift('a', -5),
      items: [{ productId: 'pan', productName: 'Pan francés', soldByCount: '35.000', unitPrice: 0.5 }],
    })
    render(<ShiftsPanel dashboard={dashboard} />)

    fireEvent.click((await screen.findAllByRole('button', { name: 'Ver detalle' }))[0]!)

    expect(await screen.findByRole('list', { name: 'Vendido por producto' })).toHaveTextContent('Pan francés')
    expect(getShift).toHaveBeenCalledWith('a')
  })

  it('sin turnos en el rango lo dice', async () => {
    vi.mocked(listShifts).mockResolvedValue([])
    render(<ShiftsPanel dashboard={dashboard} />)

    expect(await screen.findByText('No hubo turnos en este rango.')).toBeInTheDocument()
  })
})
