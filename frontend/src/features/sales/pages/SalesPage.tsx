import { useCallback, useState } from 'react'
import { useCanManage } from '@shared/auth/session'
import { businessDate, formatMoney } from '@shared/lib/format'
import { Dialog } from '@shared/ui/Dialog'
import { Toast } from '@shared/ui/Toast'
import { registerSale, voidSale, type Sale, type SaleRequest } from '../api/sales'
import { SaleForm } from '../components/SaleForm'
import { SaleHistory } from '../components/SaleHistory'
import { SaleReceipt } from '../components/SaleReceipt'
import { VoidSaleForm } from '../components/VoidSaleForm'
import './SalesPage.css'

/**
 * Ventas: registrar una venta en pocos pasos (HU-14), consultar el historial (HU-16) y, solo el `ADMIN`,
 * anular una venta desde su comprobante (HU-15). Para `ADMIN` y `EMPLEADO`.
 */
export function SalesPage() {
  const [today] = useState(() => businessDate(new Date()))
  const [formKey, setFormKey] = useState(0)
  const [version, setVersion] = useState(0)
  const admin = useCanManage()
  const [receipt, setReceipt] = useState<Sale>()
  const [voiding, setVoiding] = useState(false)
  const [notice, setNotice] = useState<string>()
  const dismissNotice = useCallback(() => setNotice(undefined), [])

  async function handleRegister(request: SaleRequest) {
    const sale = await registerSale(request)
    setFormKey((value) => value + 1)
    setVersion((value) => value + 1)
    setReceipt(sale)
    setNotice(`Venta ${sale.number} registrada por ${formatMoney(sale.total)}.`)
  }

  function openReceipt(sale: Sale) {
    setReceipt(sale)
    setVoiding(false)
  }

  async function handleVoid(reason: string) {
    if (!receipt) return
    const voided = await voidSale(receipt.id, reason)
    setReceipt(voided)
    setVoiding(false)
    setVersion((value) => value + 1)
    setNotice(`Venta ${voided.number} anulada; el stock volvió al inventario.`)
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

      <SaleHistory today={today} version={version} onOpen={openReceipt} />

      {receipt ? (
        <Dialog
          title={voiding ? `Anular la venta ${receipt.number}` : `Comprobante ${receipt.number}`}
          onClose={() => setReceipt(undefined)}
        >
          {voiding ? (
            <VoidSaleForm onSubmit={handleVoid} onCancel={() => setVoiding(false)} />
          ) : (
            <SaleReceipt
              sale={receipt}
              actions={
                admin && receipt.status === 'REGISTERED' ? (
                  <button className="button button--secondary" type="button" onClick={() => setVoiding(true)}>
                    Anular venta
                  </button>
                ) : null
              }
            />
          )}
        </Dialog>
      ) : null}

      {notice ? <Toast message={notice} onDismiss={dismissNotice} /> : null}
    </section>
  )
}
