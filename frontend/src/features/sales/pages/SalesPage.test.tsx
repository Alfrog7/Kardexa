import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '@shared/api/http'
import { findProducts, registerSale, type Sale } from '../api/sales'
import { SalesPage } from './SalesPage'

vi.mock('../api/sales', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../api/sales')>()),
  registerSale: vi.fn(),
  findProducts: vi.fn(),
}))

const products = [
  { id: 'rice', code: 'ARR-1', name: 'Arroz', salePrice: '25.00', stock: '10.000' },
  { id: 'oil', code: 'ACE-1', name: 'Aceite', salePrice: '9.90', stock: '2.000' },
  { id: 'salt', code: 'SAL-1', name: 'Sal', salePrice: '1.50', stock: '0.000' },
]

const sale: Sale = {
  id: 'v1',
  number: 'V-000001',
  soldAt: '2026-11-12T15:00:00Z',
  sellerId: 'u1',
  seller: 'Ana Pérez',
  status: 'REGISTERED',
  voidReason: null,
  total: '59.90',
  items: [
    { productId: 'rice', productCode: 'ARR-1', productName: 'Arroz', quantity: '2.000', unitPrice: '25.00', subtotal: '50.00' },
    { productId: 'oil', productCode: 'ACE-1', productName: 'Aceite', quantity: '1.000', unitPrice: '9.90', subtotal: '9.90' },
  ],
}

async function addProduct(name: string) {
  fireEvent.change(screen.getByLabelText('Agregar producto'), { target: { value: name } })
  fireEvent.click(await screen.findByRole('button', { name }))
}

describe('SalesPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(findProducts).mockImplementation(async (q) =>
      products.filter((product) => product.name.toLowerCase().includes(q.toLowerCase())),
    )
  })

  it('registra una venta de dos productos con el total en vivo y muestra su número', async () => {
    vi.mocked(registerSale).mockResolvedValue(sale)
    render(<SalesPage />)

    await addProduct('Arroz')
    await addProduct('Aceite')
    fireEvent.change(screen.getByLabelText('Cantidad de Arroz'), { target: { value: '2' } })

    expect(screen.getByText('Total:').textContent).toMatch(/59\.90/)
    fireEvent.click(screen.getByRole('button', { name: 'Registrar venta' }))

    await waitFor(() =>
      expect(registerSale).toHaveBeenCalledWith({
        items: [
          { productId: 'rice', quantity: '2' },
          { productId: 'oil', quantity: '1' },
        ],
      }),
    )
    const dialog = within(await screen.findByRole('dialog', { name: 'Comprobante V-000001' }))
    expect(dialog.getByText('Ana Pérez')).toBeTruthy()
    expect(dialog.getByRole('button', { name: 'Imprimir' })).toBeTruthy()
    expect(screen.getByText(/Venta V-000001 registrada/)).toBeTruthy()
    expect(screen.queryByLabelText('Cantidad de Arroz')).toBeNull()
  })

  it('marca una cantidad mayor que el stock antes de enviar', async () => {
    render(<SalesPage />)

    await addProduct('Aceite')
    fireEvent.change(screen.getByLabelText('Cantidad de Aceite'), { target: { value: '3' } })
    fireEvent.click(screen.getByRole('button', { name: 'Registrar venta' }))

    expect(await screen.findByText('Solo hay 2 en stock.')).toBeTruthy()
    expect(registerSale).not.toHaveBeenCalled()
  })

  it('no deja agregar un producto sin stock ni enviar una venta vacía', async () => {
    render(<SalesPage />)

    fireEvent.change(screen.getByLabelText('Agregar producto'), { target: { value: 'Sal' } })
    expect(((await screen.findByRole('button', { name: 'Sal' })) as HTMLButtonElement).disabled).toBe(true)
    expect(screen.getByText(/sin stock/)).toBeTruthy()

    fireEvent.click(screen.getByRole('button', { name: 'Registrar venta' }))
    expect(await screen.findByText('Agrega al menos un producto.')).toBeTruthy()
  })

  it('muestra qué producto no alcanza si la API rechaza la venta (RN-01) y el aviso de concurrencia', async () => {
    vi.mocked(registerSale).mockRejectedValueOnce(
      new ApiError({ status: 422, title: 'Stock insuficiente', rule: 'RN-01', detail: 'Stock insuficiente para "Arroz": disponible 1, solicitado 2.' }),
    )
    vi.mocked(registerSale).mockRejectedValueOnce(new ApiError({ status: 409, title: 'Conflicto' }))
    render(<SalesPage />)

    await addProduct('Arroz')
    fireEvent.click(screen.getByRole('button', { name: 'Registrar venta' }))
    expect((await screen.findByRole('alert')).textContent).toContain('Stock insuficiente para "Arroz"')

    fireEvent.click(screen.getByRole('button', { name: 'Registrar venta' }))
    expect((await screen.findByRole('alert')).textContent).toContain('Otra venta cambió el stock')
    expect(screen.getByLabelText('Cantidad de Arroz')).toBeTruthy()
  })
})
