import { fireEvent, render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '@shared/api/http'
import { createSite, deleteSite, listSites, setSiteActive, updateSite, type Site } from '../api/sites'
import { SitesPage } from './SitesPage'

vi.mock('../api/sites', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../api/sites')>()),
  listSites: vi.fn(),
  createSite: vi.fn(),
  deleteSite: vi.fn(),
  updateSite: vi.fn(),
  setSiteActive: vi.fn(),
}))

const galeno: Site = { id: '1', name: 'Galeno', address: 'Av. Larco 123', active: true }
const principal: Site = { id: '2', name: 'Sede principal', address: null, active: false }

function renderPage() {
  return render(
    <MemoryRouter>
      <SitesPage />
    </MemoryRouter>,
  )
}

async function table() {
  return screen.findByRole('table', { name: 'Sedes' })
}

function openCreateForm() {
  fireEvent.click(screen.getByRole('button', { name: 'Nueva sede' }))
  return within(screen.getByRole('dialog'))
}

describe('SitesPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(listSites).mockResolvedValue([galeno, principal])
  })

  it('lista las sedes con su dirección y estado', async () => {
    renderPage()

    expect(screen.getByRole('status')).toHaveTextContent('Cargando')
    expect(within(await table()).getAllByRole('row')).toHaveLength(3)
    expect(screen.getByText('Av. Larco 123')).toBeInTheDocument()
    expect(screen.getByRole('switch', { name: 'Galeno habilitada' })).toBeChecked()
    expect(screen.getByRole('switch', { name: 'Sede principal habilitada' })).not.toBeChecked()
  })

  it('crea una sede y recarga la lista', async () => {
    vi.mocked(createSite).mockResolvedValue({ ...galeno, id: '3', name: 'Monserrate' })
    renderPage()
    await table()

    const form = openCreateForm()
    fireEvent.change(form.getByLabelText('Nombre'), { target: { value: ' Monserrate ' } })
    fireEvent.click(form.getByRole('button', { name: 'Crear sede' }))

    expect(await screen.findByText('Sede Monserrate guardada.')).toBeInTheDocument()
    expect(createSite).toHaveBeenCalledWith({ name: 'Monserrate', address: null })
    expect(listSites).toHaveBeenCalledTimes(2)
  })

  it('pide el nombre sin llamar a la API', async () => {
    renderPage()
    await table()

    const form = openCreateForm()
    fireEvent.click(form.getByRole('button', { name: 'Crear sede' }))

    expect(form.getByText('Escribe el nombre de la sede.')).toBeInTheDocument()
    expect(form.getByLabelText('Nombre')).toHaveAttribute('aria-invalid', 'true')
    expect(createSite).not.toHaveBeenCalled()
  })

  it('muestra el nombre repetido junto a su campo', async () => {
    vi.mocked(createSite).mockRejectedValue(
      new ApiError({ title: 'Nombre de sede duplicado', status: 409, rule: 'RF-26', detail: 'Ya existe una sede llamada "Galeno".' }),
    )
    renderPage()
    await table()

    const form = openCreateForm()
    fireEvent.change(form.getByLabelText('Nombre'), { target: { value: 'Galeno' } })
    fireEvent.click(form.getByRole('button', { name: 'Crear sede' }))

    const message = await form.findByText('Ya existe una sede llamada "Galeno".')
    expect(form.getByLabelText('Nombre')).toHaveAttribute('aria-describedby', message.id)
    expect(form.getByRole('button', { name: 'Crear sede' })).toBeEnabled()
  })

  it('edita el nombre y la dirección de una sede', async () => {
    vi.mocked(updateSite).mockResolvedValue({ ...galeno, name: 'Galeno Norte', address: 'Av. España 100' })
    renderPage()
    await table()

    fireEvent.click(screen.getByRole('button', { name: 'Editar Galeno' }))
    const form = within(screen.getByRole('dialog'))
    expect(form.getByLabelText('Dirección (opcional)')).toHaveValue('Av. Larco 123')
    fireEvent.change(form.getByLabelText('Nombre'), { target: { value: 'Galeno Norte' } })
    fireEvent.change(form.getByLabelText('Dirección (opcional)'), { target: { value: 'Av. España 100' } })
    fireEvent.click(form.getByRole('button', { name: 'Guardar cambios' }))

    expect(await screen.findByRole('cell', { name: 'Galeno Norte' })).toBeInTheDocument()
    expect(updateSite).toHaveBeenCalledWith('1', { name: 'Galeno Norte', address: 'Av. España 100' })
  })

  it('inhabilita una sede con el interruptor y lo confirma', async () => {
    vi.mocked(setSiteActive).mockResolvedValue({ ...galeno, active: false })
    renderPage()
    await table()

    fireEvent.click(screen.getByRole('switch', { name: 'Galeno habilitada' }))

    expect(await screen.findByText('Sede Galeno inhabilitada.')).toBeInTheDocument()
    expect(setSiteActive).toHaveBeenCalledWith('1', false)
    expect(screen.getByRole('switch', { name: 'Galeno habilitada' })).not.toBeChecked()
  })

  it('ofrece reintentar si la lista no carga', async () => {
    vi.mocked(listSites).mockRejectedValueOnce(new Error('sin red')).mockResolvedValue([galeno])
    renderPage()

    fireEvent.click(await screen.findByRole('button', { name: 'Reintentar' }))

    expect(await table()).toBeInTheDocument()
  })

  it('«Ver» muestra los datos de la sede', async () => {
    renderPage()
    await table()

    fireEvent.click(screen.getByRole('button', { name: 'Ver Galeno' }))

    const dialog = within(screen.getByRole('dialog'))
    expect(dialog.getByText('Av. Larco 123')).toBeInTheDocument()
    expect(dialog.getByText('Habilitada')).toBeInTheDocument()
    expect(dialog.getByRole('button', { name: 'Trabajar en esta sede' })).toBeInTheDocument()
  })

  it('elimina una sede tras confirmar y recarga la lista', async () => {
    vi.mocked(deleteSite).mockResolvedValue(undefined)
    renderPage()
    await table()

    fireEvent.click(screen.getByRole('button', { name: 'Eliminar Galeno' }))
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Eliminar sede' }))

    expect(await screen.findByText('Sede Galeno eliminada.')).toBeInTheDocument()
    expect(deleteSite).toHaveBeenCalledWith('1')
    expect(listSites).toHaveBeenCalledTimes(2)
  })

  it('si la sede tiene historial explica por qué y ofrece inhabilitarla', async () => {
    vi.mocked(deleteSite).mockRejectedValue(
      new ApiError({ title: 'La sede no se puede eliminar', status: 409, rule: 'RF-26', detail: 'La sede tiene stock, ventas, compras o trabajadores.' }),
    )
    vi.mocked(setSiteActive).mockResolvedValue({ ...galeno, active: false })
    renderPage()
    await table()

    fireEvent.click(screen.getByRole('button', { name: 'Eliminar Galeno' }))
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Eliminar sede' }))
    const dialog = within(screen.getByRole('dialog'))
    expect(await dialog.findByRole('alert')).toHaveTextContent('tiene stock')
    fireEvent.click(dialog.getByRole('button', { name: 'Inhabilitar sede' }))

    expect(await screen.findByText('Sede Galeno inhabilitada.')).toBeInTheDocument()
    expect(setSiteActive).toHaveBeenCalledWith('1', false)
  })
})
