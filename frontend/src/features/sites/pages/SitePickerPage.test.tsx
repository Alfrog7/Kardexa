import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { SessionProvider } from '@shared/auth/SessionProvider'
import { getActiveSite, setActiveSite } from '@shared/site/activeSite'
import { listSites, type Site } from '../api/sites'
import { SitePickerPage } from './SitePickerPage'

vi.mock('../api/sites', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../api/sites')>()),
  listSites: vi.fn(),
}))

const galeno: Site = { id: '1', name: 'Galeno', address: 'Av. Larco 123', active: true }
const monserrate: Site = { id: '2', name: 'Monserrate', address: null, active: true }
const cerrada: Site = { id: '3', name: 'Vieja', address: null, active: false }

function renderPicker() {
  render(
    <SessionProvider
      initialSession={{
        token: 't',
        user: { id: '1', fullName: 'Ana Pérez', email: 'ana@correo.com', role: 'ADMIN', active: true },
      }}
    >
      <MemoryRouter initialEntries={['/elegir-sede']}>
        <Routes>
          <Route path="/elegir-sede" element={<SitePickerPage />} />
          <Route path="/" element={<p>Panel</p>} />
        </Routes>
      </MemoryRouter>
    </SessionProvider>,
  )
}

describe('SitePickerPage', () => {
  beforeEach(() => {
    setActiveSite(undefined)
    vi.mocked(listSites).mockResolvedValue([galeno, monserrate, cerrada])
  })

  it('muestra las sedes habilitadas en tarjetas, más «Todas las sedes»', async () => {
    renderPicker()

    expect(await screen.findByRole('button', { name: /Galeno/ })).toHaveTextContent('Av. Larco 123')
    expect(screen.getByRole('button', { name: /Monserrate/ })).toHaveTextContent('Sin dirección')
    expect(screen.getByRole('button', { name: /Todas las sedes/ })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Vieja/ })).not.toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Ana')
  })

  it('al elegir una sede la recuerda y abre el panel', async () => {
    renderPicker()

    fireEvent.click(await screen.findByRole('button', { name: /Monserrate/ }))

    expect(getActiveSite()).toEqual({ id: '2', name: 'Monserrate' })
    expect(await screen.findByText('Panel')).toBeInTheDocument()
  })

  it('«Todas las sedes» queda como vista sin sede concreta', async () => {
    renderPicker()

    fireEvent.click(await screen.findByRole('button', { name: /Todas las sedes/ }))

    expect(getActiveSite()).toEqual({ id: null, name: 'Todas las sedes' })
  })
})
