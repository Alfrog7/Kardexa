import type { ReactNode } from 'react'
import { formatDateTime, formatMoney, formatQuantity } from '@shared/lib/format'
import type { Sale } from '../api/sales'
import './SaleReceipt.css'

interface SaleReceiptProps {
  sale: Sale
  /** Acciones extra junto a «Imprimir», por ejemplo anular la venta. */
  actions?: ReactNode
}

/**
 * Comprobante interno de una venta (RF-19): número, fecha y hora, vendedor, ítems con el precio copiado,
 * total y estado. Al imprimir solo sale el comprobante. No es un comprobante electrónico de SUNAT.
 */
export function SaleReceipt({ sale, actions }: SaleReceiptProps) {
  const voided = sale.status === 'VOIDED'

  return (
    <article className="sale-receipt" aria-label={`Comprobante ${sale.number}`}>
      <header className="sale-receipt__header">
        <p className="sale-receipt__kind">Comprobante interno de venta</p>
        <p className="sale-receipt__number">{sale.number}</p>
        {voided ? <p className="sale-receipt__voided">Anulada</p> : null}
      </header>

      <dl className="sale-receipt__meta">
        <dt>Fecha</dt>
        <dd>{formatDateTime(sale.soldAt)}</dd>
        <dt>Vendedor</dt>
        <dd>{sale.seller}</dd>
        {voided && sale.voidReason ? (
          <>
            <dt>Motivo de anulación</dt>
            <dd>{sale.voidReason}</dd>
          </>
        ) : null}
      </dl>

      <table className="sale-receipt__items">
        <thead>
          <tr>
            <th scope="col">Producto</th>
            <th scope="col">Cant.</th>
            <th scope="col">P. unit.</th>
            <th scope="col">Subtotal</th>
          </tr>
        </thead>
        <tbody>
          {sale.items.map((item) => (
            <tr key={item.productId}>
              <td>
                {item.productName}
                <span className="sale-receipt__code">{item.productCode}</span>
              </td>
              <td>{formatQuantity(item.quantity)}</td>
              <td>{formatMoney(item.unitPrice)}</td>
              <td>{formatMoney(item.subtotal)}</td>
            </tr>
          ))}
        </tbody>
        <tfoot>
          <tr>
            <th scope="row" colSpan={3}>
              Total
            </th>
            <td>{formatMoney(sale.total)}</td>
          </tr>
        </tfoot>
      </table>

      <div className="sale-receipt__actions">
        {actions}
        <button className="button" type="button" onClick={() => window.print()}>
          Imprimir
        </button>
      </div>
    </article>
  )
}
