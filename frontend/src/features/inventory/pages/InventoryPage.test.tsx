import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '@shared/api/http'
import { findProducts, getKardex, getProduct, listMovements, registerAdjustment, type Kardex } from '../api/inventory'
import { InventoryPage } from './InventoryPage'

vi.mock('../api/inventory', () => ({
  findProducts: vi.fn(),
  getKardex: vi.fn(),
  getProduct: vi.fn(),
  listMovements: vi.fn(),
  registerAdjustment: vi.fn(),
}))

const aceite = { id: 'p1', code: 'ACE-1', name: 'Aceite 1 L', stock: '13.000' }

const kardex: Kardex = {
  productId: 'p1',
  productName: 'Aceite 1 L',
  openingBalance: '10.000',
  closingBalance: '13.000',
  movements: [
    {
      id: 'm1',
      occurredAt: '2026-11-01T15:00:00Z',
      type: 'PURCHASE',
      direction: 'IN',
      quantity: '5.000',
      balanceAfter: '15.000',
      sourceLabel: 'C-000001',
      user: 'Sistema',
    },
    {
      id: 'm2',
      occurredAt: '2026-11-02T15:00:00Z',
      type: 'ADJUSTMENT',
      direction: 'OUT',
      quantity: '2.000',
      balanceAfter: '13.000',
      user: 'Sistema',
    },
  ],
}

async function pickProduct() {
  render(<InventoryPage />, { wrapper: MemoryRouter })
  fireEvent.change(screen.getByLabelText('Producto'), { target: { value: 'ace' } })
  fireEvent.click(await screen.findByRole('button', { name: 'Aceite 1 L' }))
  await screen.findByText('Saldo final')
}

function openAdjustment(quantity: string) {
  fireEvent.click(screen.getByRole('button', { name: 'Registrar ajuste' }))
  fireEvent.change(screen.getByLabelText('Cantidad'), { target: { value: quantity } })
}

function submitAdjustment() {
  const dialog = screen.getByRole('dialog', { name: 'Ajustar stock de Aceite 1 L' })
  fireEvent.submit(dialog.querySelector('form') as HTMLFormElement)
}

describe('InventoryPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(findProducts).mockResolvedValue([aceite])
    vi.mocked(getKardex).mockResolvedValue(kardex)
    vi.mocked(listMovements).mockResolvedValue({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 })
  })

  it('pide elegir un producto y no consulta el kárdex antes', () => {
    render(<InventoryPage />, { wrapper: MemoryRouter })

    expect(screen.getByText('Elige un producto para ver su kárdex.')).toBeInTheDocument()
    expect(getKardex).not.toHaveBeenCalled()
    expect(screen.queryByRole('button', { name: 'Registrar ajuste' })).not.toBeInTheDocument()
  })

  it('avisa si ningún producto coincide con la búsqueda', async () => {
    vi.mocked(findProducts).mockResolvedValue([])
    render(<InventoryPage />, { wrapper: MemoryRouter })

    fireEvent.change(screen.getByLabelText('Producto'), { target: { value: 'xyz' } })

    expect(await screen.findByText('Ningún producto coincide con tu búsqueda.')).toBeInTheDocument()
  })

  it('muestra el kárdex en orden con saldo inicial, saldo final, entradas y salidas', async () => {
    await pickProduct()

    expect(screen.getByText('Saldo inicial').nextElementSibling).toHaveTextContent('10')
    expect(screen.getByText('Saldo final').nextElementSibling).toHaveTextContent('13')

    const rows = screen.getAllByRole('row').slice(1)
    expect(rows).toHaveLength(2)
    expect(rows[0]).toHaveTextContent('Compra')
    expect(rows[0]).toHaveTextContent('C-000001')
    expect(rows[1]).toHaveTextContent('Ajuste')
  })

  it('al cambiar las fechas vuelve a consultar con el nuevo rango', async () => {
    await pickProduct()

    fireEvent.change(screen.getByLabelText('Desde'), { target: { value: '2026-11-02' } })
    fireEvent.change(screen.getByLabelText('Hasta'), { target: { value: '2026-11-05' } })

    await waitFor(() => expect(getKardex).toHaveBeenLastCalledWith('p1', '2026-11-02', '2026-11-05'))
  })

  it('no consulta si la fecha inicial es posterior a la final', async () => {
    await pickProduct()
    vi.mocked(getKardex).mockClear()

    fireEvent.change(screen.getByLabelText('Hasta'), { target: { value: '2000-01-01' } })

    expect(await screen.findByRole('alert')).toHaveTextContent('La fecha inicial no puede ser posterior a la final.')
    expect(getKardex).not.toHaveBeenCalled()
    expect(screen.queryByText('Saldo final')).not.toBeInTheDocument()
  })

  it('avisa si no hay movimientos en el rango', async () => {
    vi.mocked(getKardex).mockResolvedValue({ ...kardex, movements: [] })
    await pickProduct()

    expect(screen.getByText('No hay movimientos en este rango de fechas.')).toBeInTheDocument()
  })

  it('muestra el error de carga y permite reintentar', async () => {
    vi.mocked(getKardex).mockRejectedValueOnce(new ApiError({ title: 'Sin conexión', status: 0, detail: 'Sin red.' }))
    render(<InventoryPage />, { wrapper: MemoryRouter })
    fireEvent.change(screen.getByLabelText('Producto'), { target: { value: 'ace' } })
    fireEvent.click(await screen.findByRole('button', { name: 'Aceite 1 L' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Sin red.')
    fireEvent.click(screen.getByRole('button', { name: 'Reintentar' }))

    expect(await screen.findByText('Saldo final')).toBeInTheDocument()
  })

  it('registra una salida por merma y actualiza el kárdex', async () => {
    vi.mocked(registerAdjustment).mockResolvedValue(kardex.movements[1]!)
    await pickProduct()
    vi.mocked(getKardex).mockClear()

    openAdjustment('2.5')
    fireEvent.change(screen.getByLabelText('Nota (opcional)'), { target: { value: ' Botella rota ' } })
    submitAdjustment()

    await screen.findByText('Ajuste registrado. Ya se actualizó el stock.')
    expect(registerAdjustment).toHaveBeenCalledWith({
      productId: 'p1',
      direction: 'OUT',
      quantity: '2.5',
      reason: 'SHRINKAGE',
      note: 'Botella rota',
    })
    await waitFor(() => expect(getKardex).toHaveBeenCalledTimes(1))
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('una salida mayor que el stock muestra el error junto a la cantidad y no cierra', async () => {
    vi.mocked(registerAdjustment).mockRejectedValue(
      new ApiError({ title: 'Regla', status: 422, rule: 'RN-01', detail: 'Stock insuficiente: disponible 13, solicitado 99.' }),
    )
    await pickProduct()

    openAdjustment('99')
    submitAdjustment()

    const message = await screen.findByText('Stock insuficiente: disponible 13, solicitado 99.')
    expect(screen.getByLabelText('Cantidad')).toHaveAttribute('aria-describedby', message.id)
    expect(screen.getByLabelText('Cantidad')).toHaveValue('99')
    expect(screen.getByRole('dialog')).toBeInTheDocument()
  })

  it.each(['0', '1.2345', 'abc'])('rechaza la cantidad %s antes de enviar', async (quantity) => {
    await pickProduct()

    openAdjustment(quantity)
    submitAdjustment()

    expect(await screen.findByText(/Cantidad mayor que 0/)).toBeInTheDocument()
    expect(registerAdjustment).not.toHaveBeenCalled()
  })

  it('permite cambiar de producto', async () => {
    await pickProduct()

    fireEvent.click(screen.getByRole('button', { name: 'Cambiar producto' }))

    expect(screen.getByLabelText('Producto')).toBeInTheDocument()
    expect(screen.queryByText('Saldo final')).not.toBeInTheDocument()
  })

  it('desde Productos llega con el producto elegido y el ajuste abierto', async () => {
    vi.mocked(getProduct).mockResolvedValue(aceite)
    render(
      <MemoryRouter initialEntries={['/inventario?producto=p1&ajustar=1']}>
        <InventoryPage />
      </MemoryRouter>,
    )

    expect(await screen.findByRole('dialog', { name: 'Ajustar stock de Aceite 1 L' })).toBeInTheDocument()
    expect(getProduct).toHaveBeenCalledWith('p1')
    expect(screen.getByText('Stock actual').nextElementSibling).toHaveTextContent('13')
  })

  it('sin producto elegido muestra el historial general y abre el kárdex desde una fila', async () => {
    vi.mocked(listMovements).mockResolvedValue({
      content: [{ productId: 'p1', productCode: 'ACE-1', productName: 'Aceite 1 L', movement: kardex.movements[1]! }],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })
    vi.mocked(getProduct).mockResolvedValue(aceite)
    render(<InventoryPage />, { wrapper: MemoryRouter })

    const table = await screen.findByRole('table', { name: 'Historial de movimientos' })
    expect(table).toHaveTextContent('Aceite 1 L')
    expect(table).toHaveTextContent('Ajuste')
    expect(table).toHaveTextContent('−2')

    fireEvent.click(screen.getByRole('button', { name: 'Ver kárdex de Aceite 1 L' }))
    expect(await screen.findByText('Saldo final')).toBeInTheDocument()
    expect(screen.queryByRole('table', { name: 'Historial de movimientos' })).not.toBeInTheDocument()
  })
})
