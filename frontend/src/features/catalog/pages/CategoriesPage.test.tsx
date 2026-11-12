import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '@shared/api/http'
import { createCategory, listCategories, updateCategory, type Category } from '../api/categories'
import { CategoriesPage } from './CategoriesPage'

vi.mock('../api/categories', () => ({
  listCategories: vi.fn(),
  createCategory: vi.fn(),
  updateCategory: vi.fn(),
}))

const abarrotes: Category = { id: '1', name: 'Abarrotes', active: true, productCount: 3 }
const bebidas: Category = { id: '2', name: 'Bebidas', active: false, productCount: 1 }

function typeName(value: string) {
  fireEvent.change(screen.getByLabelText('Nombre'), { target: { value } })
}

async function renderedNames() {
  const list = await screen.findByRole('list', { name: 'Categorías' })
  return within(list)
    .getAllByRole('listitem')
    .map((item) => item.querySelector('.category-list__name')?.textContent)
}

/** Abre la modal de alta y envía el nombre. */
async function submitNew(value: string) {
  await renderedNames()
  fireEvent.click(screen.getByRole('button', { name: 'Nueva categoría' }))
  typeName(value)
  fireEvent.click(screen.getByRole('button', { name: 'Crear categoría' }))
}

describe('CategoriesPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(listCategories).mockResolvedValue([bebidas, abarrotes])
  })

  it('muestra las categorías ordenadas por nombre y con su estado', async () => {
    render(<CategoriesPage />)

    expect(screen.getByRole('status')).toHaveTextContent('Cargando')
    expect(await renderedNames()).toEqual(['Abarrotes', 'Bebidas'])
    expect(screen.getByRole('switch', { name: 'Abarrotes activa' })).toBeChecked()
    expect(screen.getByRole('switch', { name: 'Bebidas activa' })).not.toBeChecked()
    expect(screen.getByText('Desactivada')).toBeInTheDocument()
  })

  it('avisa cuando aún no hay categorías', async () => {
    vi.mocked(listCategories).mockResolvedValue([])
    render(<CategoriesPage />)

    expect(await screen.findByText(/Aún no tienes categorías/)).toBeInTheDocument()
  })

  it('muestra el error de carga y permite reintentar', async () => {
    vi.mocked(listCategories).mockRejectedValueOnce(new ApiError({ title: 'Sin conexión', status: 0, detail: 'Sin red.' }))
    render(<CategoriesPage />)

    expect(await screen.findByRole('alert')).toHaveTextContent('Sin red.')
    fireEvent.click(screen.getByRole('button', { name: 'Reintentar' }))

    expect(await renderedNames()).toEqual(['Abarrotes', 'Bebidas'])
  })

  it('crea en una modal, la cierra y agrega la categoría sin recargar', async () => {
    vi.mocked(createCategory).mockResolvedValue({ id: '3', name: 'Aceites', active: true })
    render(<CategoriesPage />)

    await submitNew('Aceites')

    expect(await screen.findByText('Categoría Aceites guardada.')).toBeInTheDocument()
    expect(await renderedNames()).toEqual(['Abarrotes', 'Aceites', 'Bebidas'])
    expect(createCategory).toHaveBeenCalledWith('Aceites')
    expect(listCategories).toHaveBeenCalledTimes(1)
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('con un nombre repetido muestra el mensaje de la API y conserva lo escrito', async () => {
    vi.mocked(createCategory).mockRejectedValue(
      new ApiError({ title: 'Conflicto', status: 409, detail: 'Ya existe una categoría con ese nombre.' }),
    )
    render(<CategoriesPage />)

    await submitNew('Bebidas')

    expect(await screen.findByRole('alert')).toHaveTextContent('Ya existe una categoría con ese nombre.')
    expect(screen.getByLabelText('Nombre')).toHaveValue('Bebidas')
    expect(screen.getByRole('dialog')).toBeInTheDocument()
  })

  it('muestra el error de validación junto al campo', async () => {
    vi.mocked(createCategory).mockRejectedValue(
      new ApiError({
        title: 'Datos inválidos',
        status: 400,
        errors: [{ field: 'name', message: 'Debe tener entre 2 y 60 caracteres.' }],
      }),
    )
    render(<CategoriesPage />)

    await submitNew('A')

    const message = await screen.findByText('Debe tener entre 2 y 60 caracteres.')
    expect(screen.getByLabelText('Nombre')).toHaveAttribute('aria-describedby', message.id)
    expect(screen.getByLabelText('Nombre')).toHaveAttribute('aria-invalid', 'true')
  })

  it('renombra desde el nombre de la fila, con el foco en el campo', async () => {
    vi.mocked(updateCategory).mockResolvedValue({ ...abarrotes, name: 'Conservas' })
    render(<CategoriesPage />)
    await renderedNames()

    fireEvent.click(screen.getByRole('button', { name: 'Abarrotes' }))
    expect(screen.getByRole('dialog', { name: 'Editar Abarrotes' })).toBeInTheDocument()
    expect(screen.getByLabelText('Nombre')).toHaveValue('Abarrotes')
    expect(screen.getByLabelText('Nombre')).toHaveFocus()

    typeName('Conservas')
    fireEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }))

    await screen.findByText('Categoría Conservas guardada.')
    expect(updateCategory).toHaveBeenCalledWith('1', 'Conservas', true)
    expect(await renderedNames()).toEqual(['Bebidas', 'Conservas'])
  })

  it('cierra la modal sin guardar', async () => {
    render(<CategoriesPage />)
    await renderedNames()

    fireEvent.click(screen.getByRole('button', { name: 'Abarrotes' }))
    fireEvent.click(screen.getByRole('button', { name: 'Cerrar' }))

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(updateCategory).not.toHaveBeenCalled()
  })

  it('desactiva y reactiva con el interruptor de la fila', async () => {
    vi.mocked(updateCategory)
      .mockResolvedValueOnce({ ...abarrotes, active: false })
      .mockResolvedValueOnce(abarrotes)
    render(<CategoriesPage />)
    await renderedNames()

    fireEvent.click(screen.getByRole('switch', { name: 'Abarrotes activa' }))
    await waitFor(() => expect(screen.getByRole('switch', { name: 'Abarrotes activa' })).not.toBeChecked())

    await waitFor(() => expect(screen.getByRole('switch', { name: 'Abarrotes activa' })).toBeEnabled())
    fireEvent.click(screen.getByRole('switch', { name: 'Abarrotes activa' }))
    await waitFor(() => expect(screen.getByRole('switch', { name: 'Abarrotes activa' })).toBeChecked())

    expect(updateCategory).toHaveBeenNthCalledWith(1, '1', 'Abarrotes', false)
    expect(updateCategory).toHaveBeenNthCalledWith(2, '1', 'Abarrotes', true)
  })

  it('avisa si no se pudo cambiar el estado y no altera la lista', async () => {
    vi.mocked(updateCategory).mockRejectedValue(new ApiError({ title: 'Error', status: 500, detail: 'Inténtalo otra vez.' }))
    render(<CategoriesPage />)
    await renderedNames()

    fireEvent.click(screen.getByRole('switch', { name: 'Abarrotes activa' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Inténtalo otra vez.')
    expect(screen.getByRole('switch', { name: 'Abarrotes activa' })).toBeChecked()
  })

  it('resume las categorías y muestra cuántos productos tiene cada una', async () => {
    vi.mocked(listCategories).mockResolvedValue([bebidas, abarrotes])
    render(<CategoriesPage />)

    const list = await screen.findByRole('list', { name: 'Categorías' })
    expect(list).toHaveTextContent('3 productos')
    expect(list).toHaveTextContent('1 producto')
    expect(screen.getByText('Activas').nextElementSibling).toHaveTextContent('1')
    expect(screen.getByText('Productos').nextElementSibling).toHaveTextContent('4')
  })
})
