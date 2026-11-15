import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '@shared/api/http'
import { listReceiptProducts, registerReceipt } from '../api/inventory'
import { ReceiptForm } from './ReceiptForm'

vi.mock('../api/inventory', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../api/inventory')>()),
  listReceiptProducts: vi.fn(),
  registerReceipt: vi.fn(),
}))

const products = [
  { id: 'pan', code: 'PAN-1', name: 'Pan francés', stock: '10.000' },
  { id: 'boc', code: 'BOC-1', name: 'Bocadillo', stock: '2.500' },
]

describe('ReceiptForm', () => {
  beforeEach(() => {
    vi.mocked(listReceiptProducts).mockResolvedValue(products)
    vi.mocked(registerReceipt).mockResolvedValue(undefined)
  })

  it('lista los productos con su stock y el botón empieza deshabilitado', async () => {
    render(<ReceiptForm onRegistered={vi.fn()} />)

    expect(await screen.findByText('Pan francés')).toBeInTheDocument()
    expect(screen.getByText('Stock: 2.5')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Confirmar entrada' })).toBeDisabled()
  })

  it('registra solo los productos con cantidad y acepta la coma decimal', async () => {
    const onRegistered = vi.fn()
    render(<ReceiptForm onRegistered={onRegistered} />)
    fireEvent.change(await screen.findByLabelText(/Pan francés/), { target: { value: '40' } })
    fireEvent.change(screen.getByLabelText(/Bocadillo/), { target: { value: '12,5' } })

    fireEvent.click(screen.getByRole('button', { name: 'Confirmar entrada' }))

    await waitFor(() =>
      expect(registerReceipt).toHaveBeenCalledWith({
        items: [
          { productId: 'pan', quantity: '40' },
          { productId: 'boc', quantity: '12.5' },
        ],
      }),
    )
    await waitFor(() => expect(onRegistered).toHaveBeenCalledWith(2))
  })

  it('no envía una cantidad inválida y lo avisa', async () => {
    render(<ReceiptForm onRegistered={vi.fn()} />)
    fireEvent.change(await screen.findByLabelText(/Pan francés/), { target: { value: '0' } })

    fireEvent.click(screen.getByRole('button', { name: 'Confirmar entrada' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('mayor que 0')
    expect(registerReceipt).not.toHaveBeenCalled()
  })

  it('filtra la lista al buscar', async () => {
    render(<ReceiptForm onRegistered={vi.fn()} />)
    fireEvent.change(await screen.findByLabelText('Buscar producto'), { target: { value: 'boc' } })

    expect(screen.queryByText('Pan francés')).not.toBeInTheDocument()
    expect(screen.getByText('Bocadillo')).toBeInTheDocument()
  })

  it('muestra el error de la API y conserva lo escrito', async () => {
    vi.mocked(registerReceipt).mockRejectedValue(
      new ApiError({ title: 'Sin sede', status: 422, detail: 'Elige una sede concreta.' }),
    )
    const onRegistered = vi.fn()
    render(<ReceiptForm onRegistered={onRegistered} />)
    fireEvent.change(await screen.findByLabelText(/Pan francés/), { target: { value: '3' } })

    fireEvent.click(screen.getByRole('button', { name: 'Confirmar entrada' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Elige una sede concreta.')
    expect(screen.getByLabelText(/Pan francés/)).toHaveValue('3')
    expect(onRegistered).not.toHaveBeenCalled()
  })
})
