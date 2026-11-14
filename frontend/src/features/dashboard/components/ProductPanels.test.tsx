import { render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { SessionContext, type Role } from '@shared/auth/session'
import { formatMoney, formatQuantity } from '@shared/lib/format'
import type { CriticalProduct, TopProduct } from '../api/dashboard'
import { CriticalStock } from './CriticalStock'
import { TopProducts } from './TopProducts'

const top: TopProduct[] = [
  { productId: 'p1', productCode: 'ARR-1', productName: 'Arroz', quantity: '5.000', amount: '20.00', imageUrl: '/api/v1/products/p1/image?v=1' },
  { productId: 'p2', productCode: 'AZU-1', productName: 'Azúcar', quantity: '2.500', amount: '7.50' },
]

const critical: CriticalProduct[] = [
  { productId: 'c1', productCode: 'SAL-1', productName: 'Sal', categoryName: 'Abarrotes', stock: '0.000', minStock: '5.000' },
  { productId: 'c2', productCode: 'ACE-1', productName: 'Aceite', categoryName: 'Abarrotes', stock: '2.000', minStock: '3.000' },
]

function withRole(role: Role, children: React.ReactNode) {
  const user = { id: '1', fullName: 'Ana Pérez', email: 'ana@correo.com', role, active: true }
  return (
    <SessionContext.Provider value={{ user, expired: false, start: vi.fn(), end: vi.fn() }}>
      <MemoryRouter>{children}</MemoryRouter>
    </SessionContext.Provider>
  )
}

describe('Más vendidos', () => {
  it('muestra puesto, producto con miniatura, cantidad y monto, y lleva al kárdex del producto', () => {
    const { container } = render(<TopProducts products={top} />, { wrapper: MemoryRouter })

    const rows = within(screen.getByRole('table', { name: 'Más vendidos' })).getAllByRole('row')
    expect(rows[1]).toHaveTextContent('1')
    expect(rows[1]).toHaveTextContent('ArrozARR-1')
    expect(rows[1]).toHaveTextContent('5')
    expect(rows[1]?.textContent?.replace(/\s/g, ' ')).toContain(formatMoney('20.00').replace(/\s/g, ' '))
    expect(rows[2]).toHaveTextContent(formatQuantity('2.500'))
    expect(container.querySelector('img')?.getAttribute('src')).toContain('/api/v1/products/p1/image?v=1')
    expect(screen.getByRole('link', { name: 'Ver kárdex de Arroz' })).toHaveAttribute('href', '/inventario?producto=p1')
  })

  it('sin ventas muestra un estado vacío', () => {
    render(<TopProducts products={[]} />, { wrapper: MemoryRouter })

    expect(screen.getByText('Sin ventas en este rango.')).toBeInTheDocument()
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
  })
})

describe('Stock crítico', () => {
  it('muestra stock y mínimo; ajustar abre el ajuste y el ADMIN puede registrar la compra', () => {
    render(withRole('ADMIN', <CriticalStock products={critical} />))

    const items = within(screen.getByRole('list', { name: 'Stock crítico' })).getAllByRole('listitem')
    expect(items[0]).toHaveTextContent('Agotado · mín. 5')
    expect(items[1]).toHaveTextContent('Stock 2 · mín. 3')
    expect(screen.getByRole('link', { name: 'Ajustar stock de Sal' })).toHaveAttribute('href', '/inventario?producto=c1&ajustar=1')
    expect(screen.getByRole('link', { name: 'Registrar compra de Sal' })).toHaveAttribute('href', '/compras?producto=c1')
  })

  it('el EMPLEADO no ve «Registrar compra»', () => {
    render(withRole('EMPLOYEE', <CriticalStock products={critical} />))

    expect(screen.getByRole('link', { name: 'Ajustar stock de Sal' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Registrar compra de Sal' })).not.toBeInTheDocument()
  })

  it('sin productos bajo el mínimo muestra un estado vacío', () => {
    render(withRole('ADMIN', <CriticalStock products={[]} />))

    expect(screen.getByText('Todo el stock está sobre el mínimo.')).toBeInTheDocument()
  })
})
