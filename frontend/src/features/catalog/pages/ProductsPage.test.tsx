import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '@shared/api/http'
import { formatMoney } from '@shared/lib/format'
import { listCategories } from '../api/categories'
import { createProduct, searchProducts, updateProduct, type Product, type ProductPage } from '../api/products'
import { ProductsPage } from './ProductsPage'

vi.mock('../api/categories', () => ({ listCategories: vi.fn() }))
vi.mock('../api/products', () => ({
  searchProducts: vi.fn(),
  createProduct: vi.fn(),
  updateProduct: vi.fn(),
}))

const aceite: Product = {
  id: 'p1',
  code: 'ACE-1',
  name: 'Aceite 1 L',
  categoryId: 'c1',
  categoryName: 'Abarrotes',
  unit: 'LITER',
  salePrice: '12.50',
  minStock: '5.000',
  active: true,
  referenceCost: '10.00',
  stock: '2.000',
  critical: true,
}

const gaseosa: Product = {
  ...aceite,
  id: 'p2',
  code: 'GAS-1',
  name: 'Gaseosa',
  salePrice: '3.50',
  stock: '40.000',
  critical: false,
}

function pageOf(content: Product[], page = 0, totalPages = 1): ProductPage {
  return { content, page, size: 20, totalElements: content.length, totalPages }
}

function fillForm(values: Record<string, string>) {
  for (const [label, value] of Object.entries(values)) {
    fireEvent.change(screen.getByLabelText(label), { target: { value } })
  }
}

const validValues = {
  Código: 'ARR-1',
  Nombre: 'Arroz',
  'Categoría': 'c1',
  'Precio de venta (S/)': '4.20',
  'Stock mínimo': '2.5',
}

/** Abre el alta; el formulario y el filtro tienen un campo "Categoría", así que se acota al formulario. */
function openCreateForm() {
  fireEvent.click(screen.getByRole('button', { name: 'Nuevo producto' }))
  const form = screen.getByRole('heading', { name: 'Nuevo producto' }).closest('form') as HTMLFormElement
  return within(form)
}

function fillCreateForm(form: ReturnType<typeof within>, values: Record<string, string> = validValues) {
  for (const [label, value] of Object.entries(values)) {
    fireEvent.change(form.getByLabelText(label), { target: { value } })
  }
}

describe('ProductsPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(listCategories).mockResolvedValue([
      { id: 'c1', name: 'Abarrotes', active: true },
      { id: 'c2', name: 'Antiguos', active: false },
    ])
    vi.mocked(searchProducts).mockResolvedValue(pageOf([aceite, gaseosa]))
  })

  it('muestra los productos con precio y stock, y distingue el stock crítico con texto', async () => {
    render(<ProductsPage />)

    const row = (await screen.findByText('Aceite 1 L')).closest('tr') as HTMLElement
    expect(row).toHaveTextContent('ACE-1')
    expect(row).toHaveTextContent(formatMoney('12.50').replace(/\s/g, ' '))
    expect(within(row).getByText('Stock crítico')).toBeInTheDocument()

    const normal = screen.getByText('Gaseosa').closest('tr') as HTMLElement
    expect(within(normal).queryByText('Stock crítico')).not.toBeInTheDocument()
  })

  it('busca tras una espera corta, sin consultar por cada tecla', async () => {
    render(<ProductsPage />)
    await screen.findByText('Aceite 1 L')
    vi.mocked(searchProducts).mockClear()

    const search = screen.getByLabelText('Buscar por nombre o código')
    fireEvent.change(search, { target: { value: 'ac' } })
    fireEvent.change(search, { target: { value: 'ace' } })

    await waitFor(() => expect(searchProducts).toHaveBeenCalledTimes(1))
    expect(searchProducts).toHaveBeenCalledWith(expect.objectContaining({ q: 'ace', page: 0 }))
  })

  it('combina el filtro de categoría con el de stock crítico', async () => {
    render(<ProductsPage />)
    await screen.findByRole('option', { name: 'Abarrotes' })

    fireEvent.change(screen.getByLabelText('Categoría'), { target: { value: 'c1' } })
    expect(searchProducts).toHaveBeenLastCalledWith(expect.objectContaining({ categoryId: 'c1', criticalOnly: false }))

    fireEvent.click(screen.getByLabelText('Solo stock crítico'))
    await waitFor(() =>
      expect(searchProducts).toHaveBeenLastCalledWith(expect.objectContaining({ categoryId: 'c1', criticalOnly: true })),
    )
  })

  it('pagina los resultados y vuelve a la primera página al filtrar', async () => {
    vi.mocked(searchProducts).mockImplementation(async ({ page }) => pageOf([aceite], page, 2))
    render(<ProductsPage />)
    await screen.findByText('Página 1 de 2')

    fireEvent.click(screen.getByRole('button', { name: 'Siguiente' }))
    await screen.findByText('Página 2 de 2')
    expect(searchProducts).toHaveBeenLastCalledWith(expect.objectContaining({ page: 1 }))
    expect(screen.getByRole('button', { name: 'Siguiente' })).toBeDisabled()

    fireEvent.click(screen.getByLabelText('Solo stock crítico'))
    await screen.findByText('Página 1 de 2')
    expect(searchProducts).toHaveBeenLastCalledWith(expect.objectContaining({ page: 0, criticalOnly: true }))
  })

  it('avisa cuando ningún producto coincide', async () => {
    vi.mocked(searchProducts).mockResolvedValue(pageOf([], 0, 0))
    render(<ProductsPage />)

    expect(await screen.findByText('No hay productos con estos filtros.')).toBeInTheDocument()
  })

  it('muestra el error de carga y permite reintentar', async () => {
    vi.mocked(searchProducts).mockRejectedValueOnce(new ApiError({ title: 'Sin conexión', status: 0, detail: 'Sin red.' }))
    render(<ProductsPage />)

    expect(await screen.findByRole('alert')).toHaveTextContent('Sin red.')
    fireEvent.click(screen.getByRole('button', { name: 'Reintentar' }))

    expect(await screen.findByText('Aceite 1 L')).toBeInTheDocument()
  })

  it('crea un producto, cierra el formulario y actualiza la tabla', async () => {
    vi.mocked(createProduct).mockResolvedValue(aceite)
    render(<ProductsPage />)
    await screen.findByRole('option', { name: 'Abarrotes' })
    vi.mocked(searchProducts).mockClear()

    const form = openCreateForm()
    fillCreateForm(form)
    fireEvent.click(form.getByRole('button', { name: 'Crear producto' }))

    await screen.findByText('Producto Arroz guardado.')
    expect(createProduct).toHaveBeenCalledWith({
      code: 'ARR-1',
      name: 'Arroz',
      categoryId: 'c1',
      unit: 'UNIT',
      salePrice: '4.20',
      minStock: '2.5',
    })
    expect(searchProducts).toHaveBeenCalled()
    expect(screen.queryByRole('heading', { name: 'Nuevo producto' })).not.toBeInTheDocument()
  })

  it('valida como el servidor antes de enviar', async () => {
    render(<ProductsPage />)
    await screen.findByRole('option', { name: 'Abarrotes' })

    const form = openCreateForm()
    fillCreateForm(form, { ...validValues, 'Precio de venta (S/)': '4.205', 'Stock mínimo': '1.2345' })
    fireEvent.click(form.getByRole('button', { name: 'Crear producto' }))

    expect(form.getByText('Monto con hasta 2 decimales, por ejemplo 3.50.')).toBeInTheDocument()
    expect(form.getByText('Cantidad con hasta 3 decimales, por ejemplo 2.5.')).toBeInTheDocument()
    expect(form.getByLabelText('Precio de venta (S/)')).toHaveAttribute('aria-invalid', 'true')
    expect(createProduct).not.toHaveBeenCalled()
  })

  it('muestra el código repetido junto a su campo y conserva lo escrito', async () => {
    vi.mocked(createProduct).mockRejectedValue(
      new ApiError({ title: 'Conflicto', status: 409, rule: 'RN-10', detail: 'Ya existe un producto con el código "ARR-1".' }),
    )
    render(<ProductsPage />)
    await screen.findByRole('option', { name: 'Abarrotes' })

    const form = openCreateForm()
    fillCreateForm(form)
    fireEvent.click(form.getByRole('button', { name: 'Crear producto' }))

    const message = await form.findByText('Ya existe un producto con el código "ARR-1".')
    expect(form.getByLabelText('Código')).toHaveAttribute('aria-describedby', message.id)
    expect(form.getByLabelText('Código')).toHaveValue('ARR-1')
    expect(form.getByRole('button', { name: 'Crear producto' })).toBeEnabled()
  })

  it('muestra la categoría desactivada junto a su campo', async () => {
    vi.mocked(createProduct).mockRejectedValue(
      new ApiError({ title: 'Regla', status: 422, rule: 'RF-06', detail: 'La categoría "Abarrotes" está desactivada.' }),
    )
    render(<ProductsPage />)
    await screen.findByRole('option', { name: 'Abarrotes' })

    const form = openCreateForm()
    fillCreateForm(form)
    fireEvent.click(form.getByRole('button', { name: 'Crear producto' }))

    const message = await form.findByText('La categoría "Abarrotes" está desactivada.')
    expect(form.getByLabelText('Categoría')).toHaveAttribute('aria-describedby', message.id)
  })

  it('al crear solo ofrece categorías activas', async () => {
    render(<ProductsPage />)
    await screen.findByRole('option', { name: 'Abarrotes' })

    const form = openCreateForm()

    expect(form.getByRole('option', { name: 'Abarrotes' })).toBeInTheDocument()
    expect(form.queryByRole('option', { name: /Antiguos/ })).not.toBeInTheDocument()
  })

  it('edita un producto y permite desactivarlo', async () => {
    vi.mocked(updateProduct).mockResolvedValue({ ...aceite, active: false })
    render(<ProductsPage />)
    await screen.findByRole('option', { name: 'Abarrotes' })

    fireEvent.click(await screen.findByRole('button', { name: 'Editar Aceite 1 L' }))
    expect(screen.getByLabelText('Código')).toHaveFocus()
    expect(screen.getByLabelText('Precio de venta (S/)')).toHaveValue('12.50')

    fillForm({ Nombre: 'Aceite 900 ml' })
    fireEvent.click(screen.getByLabelText(/Producto activo/))
    fireEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }))

    await screen.findByText('Producto Aceite 900 ml guardado.')
    expect(updateProduct).toHaveBeenCalledWith(
      'p1',
      expect.objectContaining({ name: 'Aceite 900 ml', unit: 'LITER', active: false }),
    )
    expect(screen.getByRole('button', { name: 'Nuevo producto' })).toHaveFocus()
  })
})
