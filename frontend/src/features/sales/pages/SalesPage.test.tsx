import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '@shared/api/http'
import { SessionContext } from '@shared/auth/session'
import { exportSales, findProducts, listSales, listSellers, registerSale, voidSale, type Sale } from '../api/sales'
import { SalesPage } from './SalesPage'

vi.mock('../api/sales', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../api/sales')>()),
  registerSale: vi.fn(),
  findProducts: vi.fn(),
  listSales: vi.fn(),
  listSellers: vi.fn(),
  voidSale: vi.fn(),
  exportSales: vi.fn(),
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

function page(content: Sale[], totalPages = content.length ? 1 : 0) {
  return { content, page: 0, size: 20, totalElements: content.length, totalPages }
}

async function addProduct(name: string) {
  fireEvent.change(screen.getByLabelText('Agregar producto'), { target: { value: name } })
  fireEvent.click(await screen.findByRole('button', { name }))
}

describe('SalesPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(listSales).mockResolvedValue(page([]))
    vi.mocked(listSellers).mockResolvedValue([
      { id: 'u1', fullName: 'Ana Pérez' },
      { id: 'u2', fullName: 'Luis Rojas' },
    ])
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

    expect(screen.getByLabelText('Total de la venta')).toHaveTextContent(/59\.90/)
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

  it('lista las ventas del mes, filtra por fechas y vendedor, y abre el comprobante', async () => {
    const voided: Sale = { ...sale, id: 'v2', number: 'V-000002', status: 'VOIDED', voidReason: 'Error de cobro' }
    vi.mocked(listSales).mockResolvedValue(page([voided, sale]))
    render(<SalesPage />)

    const table = within(await screen.findByRole('table', { name: 'Ventas' }))
    expect(table.getByText('Error de cobro')).toBeTruthy()
    expect(table.getAllByText('S/ 59.90').length).toBe(2)
    const [from, to, sellerId, pageNumber] = vi.mocked(listSales).mock.calls[0] ?? []
    expect(from).toMatch(/^\d{4}-\d{2}-01$/)
    expect(to).toMatch(/^\d{4}-\d{2}-\d{2}$/)
    expect([sellerId, pageNumber]).toEqual(['', 0])

    fireEvent.change(await screen.findByLabelText('Vendedor'), { target: { value: 'u2' } })
    await waitFor(() => expect(listSales).toHaveBeenLastCalledWith(from, to, 'u2', 0))
    fireEvent.change(screen.getByLabelText('Desde'), { target: { value: '2026-11-05' } })
    await waitFor(() => expect(listSales).toHaveBeenLastCalledWith('2026-11-05', to, 'u2', 0))

    fireEvent.click(await screen.findByRole('button', { name: 'Ver comprobante V-000002' }))
    const dialog = within(await screen.findByRole('dialog', { name: 'Comprobante V-000002' }))
    expect(dialog.getByText('Anulada')).toBeTruthy()
  })

  it('avisa si el rango de fechas está al revés y permite reintentar un error', async () => {
    vi.mocked(listSales).mockRejectedValueOnce(new ApiError({ status: 500, title: 'Error', detail: 'Servidor caído' }))
    render(<SalesPage />)

    fireEvent.click(await screen.findByRole('button', { name: 'Reintentar' }))
    expect(await screen.findByText('No hay ventas en estas fechas.')).toBeTruthy()

    fireEvent.change(screen.getByLabelText('Desde'), { target: { value: '2999-01-01' } })
    expect(await screen.findByText('La fecha inicial no puede ser posterior a la final.')).toBeTruthy()
  })

  it('vuelve a cargar el historial después de registrar una venta', async () => {
    vi.mocked(registerSale).mockResolvedValue(sale)
    render(<SalesPage />)
    await screen.findByText('No hay ventas en estas fechas.')
    const calls = vi.mocked(listSales).mock.calls.length

    await addProduct('Arroz')
    fireEvent.click(screen.getByRole('button', { name: 'Registrar venta' }))

    await waitFor(() => expect(vi.mocked(listSales).mock.calls.length).toBeGreaterThan(calls))
  })

  it('el ADMIN anula una venta con motivo desde su comprobante y queda anulada con su número', async () => {
    vi.mocked(listSales).mockResolvedValue(page([sale]))
    vi.mocked(voidSale).mockResolvedValue({ ...sale, status: 'VOIDED', voidReason: 'Cliente devolvió todo' })
    render(<SalesPage />)

    fireEvent.click(await screen.findByRole('button', { name: 'Ver comprobante V-000001' }))
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Anular venta' }))
    const form = within(screen.getByRole('dialog', { name: 'Anular la venta V-000001' }))

    fireEvent.click(form.getByRole('button', { name: 'Anular venta' }))
    expect(await form.findByText(/al menos 3 caracteres/)).toBeTruthy()
    expect(voidSale).not.toHaveBeenCalled()

    fireEvent.change(form.getByLabelText('Motivo de la anulación'), { target: { value: '  Cliente devolvió todo ' } })
    fireEvent.click(form.getByRole('button', { name: 'Anular venta' }))

    await waitFor(() => expect(voidSale).toHaveBeenCalledWith('v1', 'Cliente devolvió todo'))
    const receipt = within(await screen.findByRole('dialog', { name: 'Comprobante V-000001' }))
    expect(receipt.getByText('Anulada')).toBeTruthy()
    expect(receipt.queryByRole('button', { name: 'Anular venta' })).toBeNull()
    expect(screen.getByText(/V-000001 anulada/)).toBeTruthy()
  })

  it('muestra el error de la API si rechaza la anulación', async () => {
    vi.mocked(listSales).mockResolvedValue(page([sale]))
    vi.mocked(voidSale).mockRejectedValue(new ApiError({ status: 422, title: 'Venta ya anulada', detail: 'La venta V-000001 ya está anulada.' }))
    render(<SalesPage />)

    fireEvent.click(await screen.findByRole('button', { name: 'Ver comprobante V-000001' }))
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Anular venta' }))
    const form = within(screen.getByRole('dialog'))
    fireEvent.change(form.getByLabelText('Motivo de la anulación'), { target: { value: 'Error de cobro' } })
    fireEvent.click(form.getByRole('button', { name: 'Anular venta' }))

    expect((await form.findByRole('alert')).textContent).toContain('ya está anulada')
  })

  it('un EMPLEADO no ve la acción de anular ni el filtro por vendedor', async () => {
    vi.mocked(listSales).mockResolvedValue(page([sale]))
    const session = {
      user: { id: 'u1', fullName: 'Ana Pérez', email: 'ana@bodega.pe', role: 'EMPLOYEE' as const },
    } as unknown as React.ContextType<typeof SessionContext>
    render(
      <SessionContext.Provider value={session}>
        <SalesPage />
      </SessionContext.Provider>,
    )

    fireEvent.click(await screen.findByRole('button', { name: 'Ver comprobante V-000001' }))
    expect(within(screen.getByRole('dialog')).queryByRole('button', { name: 'Anular venta' })).toBeNull()
    expect(screen.queryByLabelText('Vendedor')).toBeNull()
    expect(listSellers).not.toHaveBeenCalled()
  })

  it('exporta a CSV el historial con las fechas y el vendedor elegidos', async () => {
    vi.mocked(exportSales).mockResolvedValue()
    render(<SalesPage />)

    fireEvent.change(await screen.findByLabelText('Vendedor'), { target: { value: 'u2' } })
    fireEvent.change(screen.getByLabelText('Desde'), { target: { value: '2026-11-01' } })
    fireEvent.click(screen.getByRole('button', { name: 'Exportar CSV' }))

    await waitFor(() => expect(exportSales).toHaveBeenCalledWith('2026-11-01', expect.any(String), 'u2'))
  })
})
