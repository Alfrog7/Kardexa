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
    <div className="sale-receipt-wrap">
      <article className={voided ? 'sale-receipt sale-receipt--voided' : 'sale-receipt'} aria-label={`Comprobante ${sale.number}`}>
        <header className="sale-receipt__header">
          <img className="sale-receipt__logo" src="/logo.svg" alt="Kardexa" width="120" height="32" />
          <p className="sale-receipt__kind">Comprobante interno de venta</p>
          <p className="sale-receipt__number">{sale.number}</p>
          {voided ? <p className="sale-receipt__voided">Anulada</p> : null}
        </header>

        <dl className="sale-receipt__meta">
          <div>
            <dt>Fecha</dt>
            <dd>{formatDateTime(sale.soldAt)}</dd>
          </div>
          <div>
            <dt>Vendedor</dt>
            <dd>{sale.seller}</dd>
          </div>
          {voided && sale.voidReason ? (
            <div>
              <dt>Motivo de anulación</dt>
              <dd>{sale.voidReason}</dd>
            </div>
          ) : null}
        </dl>

        <table className="sale-receipt__items">
          <thead>
            <tr>
              <th scope="col">Descripción</th>
              <th scope="col">Cant.</th>
              <th scope="col">Importe</th>
            </tr>
          </thead>
          <tbody>
            {sale.items.map((item) => (
              <tr key={item.productId}>
                <td>
                  <span className="sale-receipt__product">{item.productName}</span>
                  <span className="sale-receipt__detail">
                    {item.productCode} · {formatMoney(item.unitPrice)} c/u
                  </span>
                </td>
                <td>{formatQuantity(item.quantity)}</td>
                <td>{formatMoney(item.subtotal)}</td>
              </tr>
            ))}
          </tbody>
          <tfoot>
            <tr>
              <th scope="row" colSpan={2}>
                Total
              </th>
              <td>{formatMoney(sale.total)}</td>
            </tr>
          </tfoot>
        </table>

        <footer className="sale-receipt__footer">
          <p>Gracias por su compra.</p>
          <p>Documento interno: no es un comprobante electrónico de SUNAT.</p>
        </footer>
      </article>

      <div className="sale-receipt__actions">
        {actions}
        <button className="button" type="button" onClick={() => window.print()}>
          Imprimir
        </button>
      </div>
    </div>
  )
}
