import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '@shared/api/http'
import { listReceiptProducts, listReceiptsOfDay, registerReceipt } from '../api/receipts'
import { ReceiptsPage } from './ReceiptsPage'

vi.mock('../api/receipts', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../api/receipts')>()),
  listReceiptProducts: vi.fn(),
  listReceiptsOfDay: vi.fn(),
  registerReceipt: vi.fn(),
}))

const products = [
  { id: 'pan', code: 'PAN-1', name: 'Pan francés', stock: '10.000' },
  { id: 'boc', code: 'BOC-1', name: 'Bocadillo', stock: '2.500' },
]

describe('ReceiptsPage', () => {
  beforeEach(() => {
    vi.mocked(listReceiptProducts).mockResolvedValue(products)
    vi.mocked(listReceiptsOfDay).mockResolvedValue([])
    vi.mocked(registerReceipt).mockResolvedValue({ id: 'e1', siteId: 's1', createdAt: '2026-11-15T10:00:00Z', items: [] })
  })

  it('lista los productos con su stock y el botón empieza deshabilitado', async () => {
    render(<ReceiptsPage />)

    expect(await screen.findByText('Pan francés')).toBeInTheDocument()
    expect(screen.getByText('Stock: 2.5')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Confirmar recepción' })).toBeDisabled()
    expect(screen.getByText('Todavía no se registró ninguna recepción hoy.')).toBeInTheDocument()
  })

  it('registra solo los productos con cantidad y acepta la coma decimal', async () => {
    render(<ReceiptsPage />)
    fireEvent.change(await screen.findByLabelText(/Pan francés/), { target: { value: '40' } })
    fireEvent.change(screen.getByLabelText(/Bocadillo/), { target: { value: '12,5' } })

    fireEvent.click(screen.getByRole('button', { name: 'Confirmar recepción' }))

    await waitFor(() =>
      expect(registerReceipt).toHaveBeenCalledWith({
        items: [
          { productId: 'pan', quantity: '40' },
          { productId: 'boc', quantity: '12.5' },
        ],
      }),
    )
    expect(await screen.findByText('Recepción registrada: 2 productos.')).toBeInTheDocument()
    expect(screen.getByLabelText(/Pan francés/)).toHaveValue('')
    expect(listReceiptsOfDay).toHaveBeenCalledTimes(2)
  })

  it('no envía una cantidad inválida y lo avisa', async () => {
    render(<ReceiptsPage />)
    fireEvent.change(await screen.findByLabelText(/Pan francés/), { target: { value: '0' } })

    fireEvent.click(screen.getByRole('button', { name: 'Confirmar recepción' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('mayor que 0')
    expect(registerReceipt).not.toHaveBeenCalled()
  })

  it('filtra la lista al buscar', async () => {
    render(<ReceiptsPage />)
    fireEvent.change(await screen.findByLabelText('Buscar producto'), { target: { value: 'boc' } })

    expect(screen.queryByText('Pan francés')).not.toBeInTheDocument()
    expect(screen.getByText('Bocadillo')).toBeInTheDocument()
  })

  it('muestra el error de la API y conserva lo escrito', async () => {
    vi.mocked(registerReceipt).mockRejectedValue(
      new ApiError({ title: 'Sin sede', status: 422, detail: 'Elige una sede concreta.' }),
    )
    render(<ReceiptsPage />)
    fireEvent.change(await screen.findByLabelText(/Pan francés/), { target: { value: '3' } })

    fireEvent.click(screen.getByRole('button', { name: 'Confirmar recepción' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Elige una sede concreta.')
    expect(screen.getByLabelText(/Pan francés/)).toHaveValue('3')
  })

  it('muestra lo recibido hoy', async () => {
    vi.mocked(listReceiptsOfDay).mockResolvedValue([
      { productName: 'Pan francés', movement: { id: 'm1', occurredAt: '2026-11-15T15:00:00Z', quantity: '40.000', user: 'Luis Soto' } },
    ])
    render(<ReceiptsPage />)

    expect(await screen.findByText(/Luis Soto/)).toBeInTheDocument()
    expect(screen.getByText('+40')).toBeInTheDocument()
  })
})
