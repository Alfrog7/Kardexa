import { fireEvent, render, screen, within } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '@shared/api/http'
import { createSite, listSites, setSiteActive, updateSite, type Site } from '../api/sites'
import { SitesPage } from './SitesPage'

vi.mock('../api/sites', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../api/sites')>()),
  listSites: vi.fn(),
  createSite: vi.fn(),
  updateSite: vi.fn(),
  setSiteActive: vi.fn(),
}))

const galeno: Site = { id: '1', name: 'Galeno', address: 'Av. Larco 123', active: true }
const principal: Site = { id: '2', name: 'Sede principal', address: null, active: false }

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
    render(<SitesPage />)

    expect(screen.getByRole('status')).toHaveTextContent('Cargando')
    expect(within(await table()).getAllByRole('row')).toHaveLength(3)
    expect(screen.getByText('Av. Larco 123')).toBeInTheDocument()
    expect(screen.getByRole('switch', { name: 'Galeno habilitada' })).toBeChecked()
    expect(screen.getByRole('switch', { name: 'Sede principal habilitada' })).not.toBeChecked()
  })

  it('crea una sede y recarga la lista', async () => {
    vi.mocked(createSite).mockResolvedValue({ ...galeno, id: '3', name: 'Monserrate' })
    render(<SitesPage />)
    await table()

    const form = openCreateForm()
    fireEvent.change(form.getByLabelText('Nombre'), { target: { value: ' Monserrate ' } })
    fireEvent.click(form.getByRole('button', { name: 'Crear sede' }))

    expect(await screen.findByText('Sede Monserrate guardada.')).toBeInTheDocument()
    expect(createSite).toHaveBeenCalledWith({ name: 'Monserrate', address: null })
    expect(listSites).toHaveBeenCalledTimes(2)
  })

  it('pide el nombre sin llamar a la API', async () => {
    render(<SitesPage />)
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
    render(<SitesPage />)
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
    render(<SitesPage />)
    await table()

    fireEvent.click(screen.getByRole('button', { name: 'Galeno' }))
    const form = within(screen.getByRole('dialog'))
    expect(form.getByLabelText('Dirección (opcional)')).toHaveValue('Av. Larco 123')
    fireEvent.change(form.getByLabelText('Nombre'), { target: { value: 'Galeno Norte' } })
    fireEvent.change(form.getByLabelText('Dirección (opcional)'), { target: { value: 'Av. España 100' } })
    fireEvent.click(form.getByRole('button', { name: 'Guardar cambios' }))

    expect(await screen.findByRole('button', { name: 'Galeno Norte' })).toBeInTheDocument()
    expect(updateSite).toHaveBeenCalledWith('1', { name: 'Galeno Norte', address: 'Av. España 100' })
  })

  it('inhabilita una sede con el interruptor y lo confirma', async () => {
    vi.mocked(setSiteActive).mockResolvedValue({ ...galeno, active: false })
    render(<SitesPage />)
    await table()

    fireEvent.click(screen.getByRole('switch', { name: 'Galeno habilitada' }))

    expect(await screen.findByText('Sede Galeno inhabilitada.')).toBeInTheDocument()
    expect(setSiteActive).toHaveBeenCalledWith('1', false)
    expect(screen.getByRole('switch', { name: 'Galeno habilitada' })).not.toBeChecked()
  })

  it('ofrece reintentar si la lista no carga', async () => {
    vi.mocked(listSites).mockRejectedValueOnce(new Error('sin red')).mockResolvedValue([galeno])
    render(<SitesPage />)

    fireEvent.click(await screen.findByRole('button', { name: 'Reintentar' }))

    expect(await table()).toBeInTheDocument()
  })
})
