import { fireEvent, render, screen, within } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import type { Sale } from '../api/sales'
import { SaleReceipt } from './SaleReceipt'

const sale: Sale = {
  id: 'v1',
  number: 'V-000007',
  soldAt: '2026-11-12T15:00:00Z',
  sellerId: 'u1',
  seller: 'Ana Pérez',
  status: 'REGISTERED',
  voidReason: null,
  total: '64.85',
  items: [
    { productId: 'rice', productCode: 'ARR-1', productName: 'Arroz 5 kg', quantity: '2.000', unitPrice: '25.00', subtotal: '50.00' },
    { productId: 'oil', productCode: 'ACE-1', productName: 'Aceite 1 L', quantity: '1.500', unitPrice: '9.90', subtotal: '14.85' },
  ],
}

describe('SaleReceipt', () => {
  it('muestra número, fecha en hora de Lima, vendedor, ítems con precio copiado y total', () => {
    render(<SaleReceipt sale={sale} />)

    const receipt = within(screen.getByRole('article', { name: 'Comprobante V-000007' }))
    expect(receipt.getByText('V-000007')).toBeTruthy()
    expect(receipt.getByText(/12\/11\/26/)).toBeTruthy()
    expect(receipt.getByText('Ana Pérez')).toBeTruthy()
    const rows = receipt.getAllByRole('row')
    expect(rows[1]?.textContent).toMatch(/Arroz 5 kg.*ARR-1.*2.*25\.00.*50\.00/)
    expect(rows[2]?.textContent).toMatch(/1\.5/)
    expect(rows[3]?.textContent).toMatch(/Total.*64\.85/)
    expect(receipt.queryByText('Anulada')).toBeNull()
  })

  it('marca una venta anulada con su motivo', () => {
    render(<SaleReceipt sale={{ ...sale, status: 'VOIDED', voidReason: 'Cliente devolvió todo' }} />)

    expect(screen.getByText('Anulada')).toBeTruthy()
    expect(screen.getByText('Cliente devolvió todo')).toBeTruthy()
  })

  it('imprime con el botón y muestra las acciones extra', () => {
    const print = vi.spyOn(window, 'print').mockImplementation(() => {})
    render(<SaleReceipt sale={sale} actions={<button type="button">Anular venta</button>} />)

    fireEvent.click(screen.getByRole('button', { name: 'Imprimir' }))
    expect(print).toHaveBeenCalledOnce()
    expect(screen.getByRole('button', { name: 'Anular venta' })).toBeTruthy()
    print.mockRestore()
  })
})
