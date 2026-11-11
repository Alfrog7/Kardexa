import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '@shared/api/http'
import { findProducts, listPurchases, registerPurchase, voidPurchase, type Purchase } from '../api/purchases'
import { listSuppliers, type Supplier } from '../api/suppliers'
import { PurchasesPage } from './PurchasesPage'

vi.mock('../api/purchases', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../api/purchases')>()),
  listPurchases: vi.fn(),
  registerPurchase: vi.fn(),
  voidPurchase: vi.fn(),
  findProducts: vi.fn(),
}))
vi.mock('../api/suppliers', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../api/suppliers')>()),
  listSuppliers: vi.fn(),
}))

const andina: Supplier = { id: 's1', ruc: '20123456789', businessName: 'Distribuidora Andina', phone: null, email: null, active: true }
const inactive: Supplier = { ...andina, id: 's2', businessName: 'Norte SAC', active: false }

const purchase: Purchase = {
  id: 'p1',
  supplier: andina,
  supplierDocument: 'F001-77',
  purchaseDate: '2026-11-10',
  status: 'REGISTERED',
  voidReason: null,
  total: '224.60',
  items: [
    { productId: 'rice', productName: 'Arroz', quantity: '10.000', unitCost: '18.50', subtotal: '185.00' },
    { productId: 'oil', productName: 'Aceite', quantity: '4.000', unitCost: '9.90', subtotal: '39.60' },
  ],
}

function page(content: Purchase[]) {
  return { content, page: 0, size: 20, totalElements: content.length, totalPages: content.length ? 1 : 0 }
}

async function openForm() {
  fireEvent.click(screen.getByRole('button', { name: 'Registrar compra' }))
  const dialog = within(screen.getByRole('dialog'))
  await dialog.findByLabelText('Proveedor')
  return dialog
}

async function addProduct(form: ReturnType<typeof within>, name: string) {
  fireEvent.change(form.getByLabelText('Agregar producto'), { target: { value: name } })
  fireEvent.click(await form.findByRole('button', { name }))
}

describe('PurchasesPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(listPurchases).mockResolvedValue(page([purchase]))
    vi.mocked(listSuppliers).mockResolvedValue([andina, inactive])
    vi.mocked(findProducts).mockImplementation(async (q) =>
      [
        { id: 'rice', code: 'ARR-1', name: 'Arroz' },
        { id: 'oil', code: 'ACE-1', name: 'Aceite' },
      ].filter((product) => product.name.toLowerCase().startsWith(q.toLowerCase())),
    )
  })

  it('lista las compras del mes actual con su total y estado', async () => {
    render(<PurchasesPage />)

    const table = await screen.findByRole('table', { name: 'Compras' })
    expect(within(table).getByText('Distribuidora Andina')).toBeInTheDocument()
    expect(within(table).getByText('Registrada')).toBeInTheDocument()
    const [from = '', to = ''] = vi.mocked(listPurchases).mock.calls[0] ?? []
    expect(from).toMatch(/^\d{4}-\d{2}-01$/)
    expect(to.slice(0, 7)).toBe(from.slice(0, 7))
  })

  it('cambia el rango de fechas y avisa si está invertido', async () => {
    render(<PurchasesPage />)
    await screen.findByRole('table', { name: 'Compras' })

    fireEvent.change(screen.getByLabelText('Desde'), { target: { value: '2026-10-01' } })
    await waitFor(() => expect(listPurchases).toHaveBeenLastCalledWith('2026-10-01', expect.any(String), 0))

    fireEvent.change(screen.getByLabelText('Desde'), { target: { value: '2099-01-01' } })
    expect(screen.getByRole('alert')).toHaveTextContent('La fecha inicial no puede ser posterior a la final.')
  })

  it('muestra el detalle de una compra con sus productos', async () => {
    render(<PurchasesPage />)

    fireEvent.click(await screen.findByRole('button', { name: /Ver compra del 10\/11\/2026/ }))

    const dialog = within(screen.getByRole('dialog'))
    const items = dialog.getByRole('table', { name: 'Productos de la compra' })
    expect(within(items).getAllByRole('row')).toHaveLength(3)
    expect(dialog.getByText('F001-77')).toBeInTheDocument()
  })

  it('registra una compra con dos productos, calcula el total en vivo y refresca la lista', async () => {
    vi.mocked(registerPurchase).mockResolvedValue(purchase)
    render(<PurchasesPage />)
    await screen.findByRole('table', { name: 'Compras' })

    const form = await openForm()
    expect(form.queryByRole('option', { name: 'Norte SAC' })).not.toBeInTheDocument()
    fireEvent.change(form.getByLabelText('Proveedor'), { target: { value: 's1' } })
    fireEvent.change(form.getByLabelText('Documento del proveedor (opcional)'), { target: { value: 'F001-77' } })
    await addProduct(form, 'Arroz')
    await addProduct(form, 'Aceite')
    fireEvent.change(form.getByLabelText('Cantidad de Arroz'), { target: { value: '10' } })
    fireEvent.change(form.getByLabelText('Costo unitario de Arroz (S/)'), { target: { value: '18.50' } })
    fireEvent.change(form.getByLabelText('Cantidad de Aceite'), { target: { value: '4' } })
    fireEvent.change(form.getByLabelText('Costo unitario de Aceite (S/)'), { target: { value: '9.90' } })

    expect(form.getByText(/Total:/)).toHaveTextContent('224.60')
    fireEvent.click(form.getByRole('button', { name: 'Registrar compra' }))

    expect(await screen.findByText(/Compra a Distribuidora Andina registrada/)).toBeInTheDocument()
    expect(registerPurchase).toHaveBeenCalledWith({
      supplierId: 's1',
      supplierDocument: 'F001-77',
      purchaseDate: expect.stringMatching(/^\d{4}-\d{2}-\d{2}$/),
      items: [
        { productId: 'rice', quantity: '10', unitCost: '18.50' },
        { productId: 'oil', quantity: '4', unitCost: '9.90' },
      ],
    })
    expect(listPurchases).toHaveBeenCalledTimes(2)
  })

  it('marca un ítem inválido antes de enviar y no llama a la API', async () => {
    render(<PurchasesPage />)
    await screen.findByRole('table', { name: 'Compras' })

    const form = await openForm()
    fireEvent.click(form.getByRole('button', { name: 'Registrar compra' }))
    expect(form.getByText('Elige el proveedor.')).toBeInTheDocument()
    expect(form.getByText('Agrega al menos un producto.')).toBeInTheDocument()

    fireEvent.change(form.getByLabelText('Proveedor'), { target: { value: 's1' } })
    await addProduct(form, 'Arroz')
    fireEvent.change(form.getByLabelText('Cantidad de Arroz'), { target: { value: '0' } })
    fireEvent.change(form.getByLabelText('Costo unitario de Arroz (S/)'), { target: { value: '1.234' } })
    fireEvent.click(form.getByRole('button', { name: 'Registrar compra' }))

    expect(form.getByText('La cantidad debe ser mayor que 0.')).toBeInTheDocument()
    expect(form.getByLabelText('Costo unitario de Arroz (S/)')).toHaveAttribute('aria-invalid', 'true')
    expect(form.getByText(/Total:/)).toHaveTextContent('—')
    expect(registerPurchase).not.toHaveBeenCalled()
  })

  it('muestra junto a su fila el error de un ítem que devuelve la API', async () => {
    vi.mocked(registerPurchase).mockRejectedValue(
      new ApiError({
        title: 'Datos inválidos',
        status: 400,
        detail: 'Revisa los campos.',
        errors: [{ field: 'items[0].quantityPositive', message: 'La cantidad debe ser mayor que 0.' }],
      }),
    )
    render(<PurchasesPage />)
    await screen.findByRole('table', { name: 'Compras' })

    const form = await openForm()
    fireEvent.change(form.getByLabelText('Proveedor'), { target: { value: 's1' } })
    await addProduct(form, 'Arroz')
    fireEvent.change(form.getByLabelText('Cantidad de Arroz'), { target: { value: '1' } })
    fireEvent.change(form.getByLabelText('Costo unitario de Arroz (S/)'), { target: { value: '2' } })
    fireEvent.click(form.getByRole('button', { name: 'Registrar compra' }))

    const message = await form.findByText('La cantidad debe ser mayor que 0.')
    expect(form.getByLabelText('Cantidad de Arroz')).toHaveAttribute('aria-describedby', message.id)
    expect(form.getByRole('button', { name: 'Registrar compra' })).toBeEnabled()
  })

  it('muestra el mensaje de la API cuando rechaza la compra entera', async () => {
    vi.mocked(registerPurchase).mockRejectedValue(
      new ApiError({ title: 'Conflicto', status: 409, detail: 'El stock cambió mientras se registraba. Inténtalo otra vez.' }),
    )
    render(<PurchasesPage />)
    await screen.findByRole('table', { name: 'Compras' })

    const form = await openForm()
    fireEvent.change(form.getByLabelText('Proveedor'), { target: { value: 's1' } })
    await addProduct(form, 'Arroz')
    fireEvent.change(form.getByLabelText('Cantidad de Arroz'), { target: { value: '1' } })
    fireEvent.change(form.getByLabelText('Costo unitario de Arroz (S/)'), { target: { value: '2' } })
    fireEvent.click(form.getByRole('button', { name: 'Registrar compra' }))

    expect(await form.findByRole('alert')).toHaveTextContent('El stock cambió')
  })

  describe('anular una compra (HU-13)', () => {
    async function openVoidForm() {
      render(<PurchasesPage />)
      fireEvent.click(await screen.findByRole('button', { name: /Ver compra del 10\/11\/2026/ }))
      fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Anular compra' }))
      return within(screen.getByRole('dialog'))
    }

    it('anula con un motivo y la marca como anulada en el detalle y la lista', async () => {
      vi.mocked(voidPurchase).mockResolvedValue({ ...purchase, status: 'VOIDED', voidReason: 'Factura duplicada' })
      const form = await openVoidForm()

      fireEvent.change(form.getByLabelText('Motivo de la anulación'), { target: { value: '  Factura duplicada ' } })
      fireEvent.click(form.getByRole('button', { name: 'Anular compra' }))

      expect(await screen.findByText('Compra a Distribuidora Andina anulada.')).toBeInTheDocument()
      expect(voidPurchase).toHaveBeenCalledWith('p1', 'Factura duplicada')
      const dialog = within(screen.getByRole('dialog'))
      expect(dialog.getByText('Anulada')).toBeInTheDocument()
      expect(dialog.getByText('Factura duplicada')).toBeInTheDocument()
      expect(dialog.queryByRole('button', { name: 'Anular compra' })).not.toBeInTheDocument()
      const table = screen.getByRole('table', { name: 'Compras' })
      expect(within(table).getByText('Anulada')).toBeInTheDocument()
      expect(within(table).getByText('Factura duplicada')).toBeInTheDocument()
    })

    it('pide un motivo antes de llamar a la API', async () => {
      const form = await openVoidForm()

      fireEvent.click(form.getByRole('button', { name: 'Anular compra' }))

      expect(form.getByLabelText('Motivo de la anulación')).toHaveAttribute('aria-invalid', 'true')
      expect(voidPurchase).not.toHaveBeenCalled()
    })

    it('si la API la rechaza por RN-01, muestra el mensaje y nada cambia', async () => {
      vi.mocked(voidPurchase).mockRejectedValue(
        new ApiError({ title: 'Regla de negocio', status: 422, rule: 'RN-01', detail: 'Stock insuficiente de Arroz: hay 3 y se pide 10.' }),
      )
      const form = await openVoidForm()

      fireEvent.change(form.getByLabelText('Motivo de la anulación'), { target: { value: 'Error de digitación' } })
      fireEvent.click(form.getByRole('button', { name: 'Anular compra' }))

      expect(await form.findByRole('alert')).toHaveTextContent('Stock insuficiente de Arroz')
      expect(form.getByRole('button', { name: 'Anular compra' })).toBeEnabled()
      expect(within(screen.getByRole('table', { name: 'Compras' })).getByText('Registrada')).toBeInTheDocument()
    })

    it('una compra anulada no ofrece la acción', async () => {
      vi.mocked(listPurchases).mockResolvedValue(page([{ ...purchase, status: 'VOIDED', voidReason: 'Duplicada' }]))
      render(<PurchasesPage />)

      fireEvent.click(await screen.findByRole('button', { name: /Ver compra del 10\/11\/2026/ }))

      expect(within(screen.getByRole('dialog')).queryByRole('button', { name: 'Anular compra' })).not.toBeInTheDocument()
    })
  })
})
