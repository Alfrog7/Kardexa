import { useCallback, useState } from 'react'
import { businessDate, formatMoney } from '@shared/lib/format'
import { Dialog } from '@shared/ui/Dialog'
import { Toast } from '@shared/ui/Toast'
import { registerSale, type Sale, type SaleRequest } from '../api/sales'
import { SaleForm } from '../components/SaleForm'
import { SaleHistory } from '../components/SaleHistory'
import { SaleReceipt } from '../components/SaleReceipt'
import './SalesPage.css'

/** Ventas: registrar una venta en pocos pasos (HU-14) y consultar el historial (HU-16). Para `ADMIN` y `EMPLEADO`. */
export function SalesPage() {
  const [today] = useState(() => businessDate(new Date()))
  const [formKey, setFormKey] = useState(0)
  const [version, setVersion] = useState(0)
  const [receipt, setReceipt] = useState<Sale>()
  const [notice, setNotice] = useState<string>()
  const dismissNotice = useCallback(() => setNotice(undefined), [])

  async function handleRegister(request: SaleRequest) {
    const sale = await registerSale(request)
    setFormKey((value) => value + 1)
    setVersion((value) => value + 1)
    setReceipt(sale)
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

      <SaleHistory today={today} version={version} onOpen={setReceipt} />

      {receipt ? (
        <Dialog title={`Comprobante ${receipt.number}`} onClose={() => setReceipt(undefined)}>
          <SaleReceipt sale={receipt} />
        </Dialog>
      ) : null}

      {notice ? <Toast message={notice} onDismiss={dismissNotice} /> : null}
    </section>
  )
}
