import { useCallback, useState } from 'react'
import { formatMoney, formatQuantity } from '@shared/lib/format'
import { Dialog } from '@shared/ui/Dialog'
import { Toast } from '@shared/ui/Toast'
import { registerSale, type Sale, type SaleRequest } from '../api/sales'
import { SaleForm } from '../components/SaleForm'
import './SalesPage.css'

/** Ventas (HU-14): registrar una venta en pocos pasos. Para `ADMIN` y `EMPLEADO`. */
export function SalesPage() {
  const [formKey, setFormKey] = useState(0)
  const [registered, setRegistered] = useState<Sale>()
  const [notice, setNotice] = useState<string>()
  const dismissNotice = useCallback(() => setNotice(undefined), [])

  async function handleRegister(request: SaleRequest) {
    const sale = await registerSale(request)
    setFormKey((value) => value + 1)
    setRegistered(sale)
    setNotice(`Venta ${sale.number} registrada por ${formatMoney(sale.total)}.`)
  }

  return (
    <section className="page">
      <header className="page__header">
        <h1>Ventas</h1>
      </header>

      <div className="card sales__new">
        <h2>Nueva venta</h2>
        <SaleForm key={formKey} onSubmit={handleRegister} />
      </div>

      {registered ? (
        <Dialog title={`Venta ${registered.number}`} onClose={() => setRegistered(undefined)}>
          <ul className="sale-summary" aria-label="Productos vendidos">
            {registered.items.map((item) => (
              <li key={item.productId}>
                <span>
                  {formatQuantity(item.quantity)} × {item.productName}
                </span>
                <span>{formatMoney(item.subtotal)}</span>
              </li>
            ))}
          </ul>
          <p className="sale-form__total">
            Total: <strong>{formatMoney(registered.total)}</strong>
          </p>
        </Dialog>
      ) : null}

      {notice ? <Toast message={notice} onDismiss={dismissNotice} /> : null}
    </section>
  )
}
