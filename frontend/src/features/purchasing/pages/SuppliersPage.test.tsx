import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '@shared/api/http'
import { SessionProvider } from '@shared/auth/SessionProvider'
import { createSupplier, listSuppliers, updateSupplier, type Supplier } from '../api/suppliers'
import { SuppliersPage } from './SuppliersPage'

vi.mock('../api/suppliers', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../api/suppliers')>()),
  listSuppliers: vi.fn(),
  createSupplier: vi.fn(),
  updateSupplier: vi.fn(),
}))

const andina: Supplier = {
  id: '1',
  ruc: '20123456789',
  businessName: 'Distribuidora Andina',
  phone: '987654321',
  email: 'ventas@andina.pe',
  active: true,
}
const norte: Supplier = { id: '2', ruc: '20999999999', businessName: 'Norte SAC', phone: null, email: null, active: false }

async function table() {
  return screen.findByRole('table', { name: 'Proveedores' })
}

function openCreateForm() {
  fireEvent.click(screen.getByRole('button', { name: 'Nuevo proveedor' }))
  return within(screen.getByRole('dialog'))
}

function fill(form: ReturnType<typeof within>, ruc: string, businessName: string) {
  fireEvent.change(form.getByLabelText('RUC'), { target: { value: ruc } })
  fireEvent.change(form.getByLabelText('Razón social'), { target: { value: businessName } })
}

describe('SuppliersPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(listSuppliers).mockResolvedValue([andina, norte])
  })

  it('lista los proveedores con su estado', async () => {
    render(<SuppliersPage />)

    expect(screen.getByRole('status')).toHaveTextContent('Cargando')
    const rows = within(await table()).getAllByRole('row')
    expect(rows).toHaveLength(3)
    expect(screen.getByRole('switch', { name: 'Distribuidora Andina activo' })).toBeChecked()
    expect(screen.getByRole('switch', { name: 'Norte SAC activo' })).not.toBeChecked()
  })

  it('busca por RUC o razón social en la API', async () => {
    render(<SuppliersPage />)
    await table()

    fireEvent.change(screen.getByLabelText('Buscar por RUC o razón social'), { target: { value: ' andina ' } })

    await waitFor(() => expect(listSuppliers).toHaveBeenLastCalledWith('andina'))
  })

  it('crea un proveedor y recarga la lista', async () => {
    vi.mocked(createSupplier).mockResolvedValue({ ...andina, id: '3', businessName: 'Lima Foods' })
    render(<SuppliersPage />)
    await table()

    const form = openCreateForm()
    fill(form, '20555555555', 'Lima Foods')
    fireEvent.click(form.getByRole('button', { name: 'Crear proveedor' }))

    expect(await screen.findByText('Proveedor Lima Foods guardado.')).toBeInTheDocument()
    expect(createSupplier).toHaveBeenCalledWith({
      ruc: '20555555555',
      businessName: 'Lima Foods',
      phone: null,
      email: null,
      active: true,
    })
    expect(listSuppliers).toHaveBeenCalledTimes(2)
  })

  it('rechaza un RUC que no tiene 11 dígitos sin llamar a la API', async () => {
    render(<SuppliersPage />)
    await table()

    const form = openCreateForm()
    fill(form, '2012345', 'Lima Foods')
    fireEvent.click(form.getByRole('button', { name: 'Crear proveedor' }))

    expect(form.getByText('El RUC debe tener exactamente 11 dígitos.')).toBeInTheDocument()
    expect(form.getByLabelText('RUC')).toHaveAttribute('aria-invalid', 'true')
    expect(createSupplier).not.toHaveBeenCalled()
  })

  it('muestra el RUC repetido junto a su campo', async () => {
    vi.mocked(createSupplier).mockRejectedValue(
      new ApiError({ title: 'Conflicto', status: 409, rule: 'RN-10', detail: 'Ya existe un proveedor con el RUC 20123456789.' }),
    )
    render(<SuppliersPage />)
    await table()

    const form = openCreateForm()
    fill(form, '20123456789', 'Otra')
    fireEvent.click(form.getByRole('button', { name: 'Crear proveedor' }))

    const message = await form.findByText('Ya existe un proveedor con el RUC 20123456789.')
    expect(form.getByLabelText('RUC')).toHaveAttribute('aria-describedby', message.id)
    expect(form.getByRole('button', { name: 'Crear proveedor' })).toBeEnabled()
  })

  it('edita un proveedor existente', async () => {
    vi.mocked(updateSupplier).mockResolvedValue({ ...andina, businessName: 'Andina SAC' })
    render(<SuppliersPage />)
    await table()

    fireEvent.click(screen.getByRole('button', { name: 'Distribuidora Andina' }))
    const form = within(screen.getByRole('dialog'))
    expect(form.getByLabelText('Correo (opcional)')).toHaveValue('ventas@andina.pe')
    fireEvent.change(form.getByLabelText('Razón social'), { target: { value: 'Andina SAC' } })
    fireEvent.click(form.getByRole('button', { name: 'Guardar cambios' }))

    expect(await screen.findByRole('button', { name: 'Andina SAC' })).toBeInTheDocument()
    expect(updateSupplier).toHaveBeenCalledWith('1', expect.objectContaining({ businessName: 'Andina SAC', active: true }))
  })

  it('desactiva un proveedor con el interruptor y lo confirma', async () => {
    vi.mocked(updateSupplier).mockResolvedValue({ ...andina, active: false })
    render(<SuppliersPage />)
    await table()

    fireEvent.click(screen.getByRole('switch', { name: 'Distribuidora Andina activo' }))

    expect(await screen.findByText('Proveedor Distribuidora Andina desactivado.')).toBeInTheDocument()
    expect(updateSupplier).toHaveBeenCalledWith('1', expect.objectContaining({ active: false, ruc: '20123456789' }))
    expect(screen.getByRole('switch', { name: 'Distribuidora Andina activo' })).not.toBeChecked()
  })

  it('al EMPLEADO no le muestra las acciones de administración', async () => {
    render(
      <SessionProvider
        initialSession={{
          token: 't',
          user: { id: '9', fullName: 'Eva', email: 'eva@correo.com', role: 'EMPLOYEE', active: true },
        }}
      >
        <SuppliersPage />
      </SessionProvider>,
    )
    await table()

    expect(screen.queryByRole('button', { name: 'Nuevo proveedor' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Distribuidora Andina' })).not.toBeInTheDocument()
    expect(screen.getByRole('switch', { name: 'Distribuidora Andina activo' })).toBeDisabled()
  })
})
