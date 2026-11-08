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

const abarrotes: Category = { id: '1', name: 'Abarrotes', active: true }
const bebidas: Category = { id: '2', name: 'Bebidas', active: false }

function typeName(value: string) {
  fireEvent.change(screen.getByLabelText('Nombre'), { target: { value } })
}

async function renderedNames() {
  const items = within(await screen.findByRole('list', { name: 'Categorías' })).getAllByRole('listitem')
  return items.map((item) => item.querySelector('.category-list__name')?.textContent)
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
    expect(screen.getByText('Activa')).toBeInTheDocument()
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

  it('crea una categoría y la agrega a la lista sin recargar', async () => {
    vi.mocked(createCategory).mockResolvedValue({ id: '3', name: 'Aceites', active: true })
    render(<CategoriesPage />)
    await renderedNames()

    typeName('Aceites')
    fireEvent.click(screen.getByRole('button', { name: 'Crear categoría' }))

    await waitFor(() => expect(screen.getByText('Aceites')).toBeInTheDocument())
    expect(await renderedNames()).toEqual(['Abarrotes', 'Aceites', 'Bebidas'])
    expect(createCategory).toHaveBeenCalledWith('Aceites')
    expect(listCategories).toHaveBeenCalledTimes(1)
    expect(screen.getByLabelText('Nombre')).toHaveValue('')
  })

  it('con un nombre repetido muestra el mensaje de la API y conserva lo escrito', async () => {
    vi.mocked(createCategory).mockRejectedValue(
      new ApiError({ title: 'Conflicto', status: 409, detail: 'Ya existe una categoría con ese nombre.' }),
    )
    render(<CategoriesPage />)
    await renderedNames()

    typeName('Bebidas')
    fireEvent.click(screen.getByRole('button', { name: 'Crear categoría' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Ya existe una categoría con ese nombre.')
    expect(screen.getByLabelText('Nombre')).toHaveValue('Bebidas')
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
    await renderedNames()

    typeName('A')
    fireEvent.click(screen.getByRole('button', { name: 'Crear categoría' }))

    const message = await screen.findByText('Debe tener entre 2 y 60 caracteres.')
    expect(screen.getByLabelText('Nombre')).toHaveAttribute('aria-describedby', message.id)
    expect(screen.getByLabelText('Nombre')).toHaveAttribute('aria-invalid', 'true')
  })

  it('renombra una categoría y lleva el foco al campo para usarlo con teclado', async () => {
    vi.mocked(updateCategory).mockResolvedValue({ ...abarrotes, name: 'Conservas' })
    render(<CategoriesPage />)
    await renderedNames()

    fireEvent.click(screen.getByRole('button', { name: 'Editar Abarrotes' }))
    expect(screen.getByLabelText('Nombre')).toHaveValue('Abarrotes')
    expect(screen.getByLabelText('Nombre')).toHaveFocus()

    typeName('Conservas')
    fireEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }))

    await waitFor(() => expect(screen.getByRole('button', { name: 'Crear categoría' })).toBeInTheDocument())
    expect(updateCategory).toHaveBeenCalledWith('1', 'Conservas', true)
    expect(await renderedNames()).toEqual(['Bebidas', 'Conservas'])
    expect(screen.getByLabelText('Nombre')).toHaveValue('')
    expect(screen.getByLabelText('Nombre')).toHaveFocus()
  })

  it('cancela la edición sin guardar', async () => {
    render(<CategoriesPage />)
    await renderedNames()

    fireEvent.click(screen.getByRole('button', { name: 'Editar Abarrotes' }))
    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }))

    expect(screen.getByLabelText('Nombre')).toHaveValue('')
    expect(updateCategory).not.toHaveBeenCalled()
  })

  it('desactiva y reactiva una categoría desde la lista', async () => {
    vi.mocked(updateCategory)
      .mockResolvedValueOnce({ ...abarrotes, active: false })
      .mockResolvedValueOnce(abarrotes)
    render(<CategoriesPage />)
    await renderedNames()

    fireEvent.click(screen.getByRole('button', { name: 'Desactivar Abarrotes' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Reactivar Abarrotes' }))

    await screen.findByRole('button', { name: 'Desactivar Abarrotes' })
    expect(updateCategory).toHaveBeenNthCalledWith(1, '1', 'Abarrotes', false)
    expect(updateCategory).toHaveBeenNthCalledWith(2, '1', 'Abarrotes', true)
  })

  it('avisa si no se pudo cambiar el estado y no altera la lista', async () => {
    vi.mocked(updateCategory).mockRejectedValue(new ApiError({ title: 'Error', status: 500, detail: 'Inténtalo otra vez.' }))
    render(<CategoriesPage />)
    await renderedNames()

    fireEvent.click(screen.getByRole('button', { name: 'Desactivar Abarrotes' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Inténtalo otra vez.')
    expect(screen.getByRole('button', { name: 'Desactivar Abarrotes' })).toBeEnabled()
  })
})
