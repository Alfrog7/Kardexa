import { useEffect, useState } from 'react'
import { formatDateTime, formatMoney, formatQuantity } from '@shared/lib/format'
import { getShift, listShifts, type Dashboard, type ShiftRow } from '../api/dashboard'
import './ShiftsPanel.css'

const MEDIA = [
  { key: 'cash', label: 'Efectivo' },
  { key: 'yape', label: 'Yape' },
  { key: 'plin', label: 'Plin' },
  { key: 'transfer', label: 'Transferencia' },
] as const

/** «Falta S/ 2.00», «Sobra S/ 1.00» o «Cuadra»; vacío si el turno sigue abierto o no anotó pagos. */
function differenceLabel(difference: number | null | string): string | undefined {
  if (difference === null) return undefined
  const value = Number(difference)
  if (value < 0) return `Falta ${formatMoney(String(-value))}`
  return value > 0 ? `Sobra ${formatMoney(String(value))}` : 'Cuadra'
}

/** Detalle de un turno: lo vendido por producto, cargado al abrirlo. */
function ShiftDetail({ id }: { id: string }) {
  const [shift, setShift] = useState<ShiftRow>()
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    let current = true
    getShift(id)
      .then((loaded) => current && setShift(loaded))
      .catch(() => current && setFailed(true))
    return () => {
      current = false
    }
  }, [id])

  if (failed) return <p className="alert">No se pudo cargar el detalle del turno.</p>
  if (!shift) return <p className="muted">Cargando detalle…</p>
  const sold = shift.items.filter((item) => Number(item.soldByCount ?? 0) > 0)
  return (
    <div className="shifts__detail">
      {shift.payments ? (
        <p className="muted">
          {MEDIA.map((medium) => `${medium.label} ${formatMoney(String(shift.payments?.[medium.key] ?? 0))}`).join(' · ')}
        </p>
      ) : null}
      {shift.note ? <p>Observación: {shift.note}</p> : null}
      {sold.length === 0 ? (
        <p className="muted">Sin productos vendidos por conteo.</p>
      ) : (
        <ul aria-label="Vendido por producto">
          {sold.map((item) => (
            <li key={item.productId}>
              {item.productName}: {formatQuantity(item.soldByCount ?? '0')}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

/**
 * Turnos y sedes en el inicio del dueño (HU-24): lo recibido por medio de pago, cómo le fue a cada sede y los turnos
 * del rango con sus diferencias; un faltante se destaca.
 */
export function ShiftsPanel({ dashboard }: { dashboard: Dashboard }) {
  const { from, to } = dashboard
  const [shifts, setShifts] = useState<ShiftRow[]>()
  const [failed, setFailed] = useState(false)
  const [open, setOpen] = useState<string>()

  useEffect(() => {
    let current = true
    listShifts(from, to)
      .then((rows) => current && setShifts(rows))
      .catch(() => current && setFailed(true))
    return () => {
      current = false
    }
  }, [from, to])

  const siteName = (siteId: string) => dashboard.sites.find((site) => site.siteId === siteId)?.name ?? ''

  return (
    <div className="shifts">
      <section className="card shifts__block" aria-labelledby="shifts-payments">
        <h3 id="shifts-payments">Recibido por medio de pago</h3>
        <p className="muted">Lo que las trabajadoras anotaron al cerrar sus turnos, sumado por efectivo, Yape, Plin y transferencia.</p>
        <dl className="shifts__media">
          {MEDIA.map((medium) => (
            <div key={medium.key}>
              <dt>{medium.label}</dt>
              <dd>{formatMoney(dashboard.payments[medium.key])}</dd>
            </div>
          ))}
          <div className="shifts__media-total">
            <dt>Total recibido</dt>
            <dd>{formatMoney(dashboard.payments.total)}</dd>
          </div>
        </dl>
      </section>

      {dashboard.sites.length > 1 ? (
        <section className="card shifts__block" aria-labelledby="shifts-sites">
          <h3 id="shifts-sites">Por sede</h3>
          <p className="muted">Lo vendido en cada sede; en rojo, lo que faltó al cuadrar sus turnos.</p>
          <ul className="shifts__list" aria-label="Ventas por sede">
            {dashboard.sites.map((site) => (
              <li key={site.siteId} className="shifts__row">
                <span>
                  <strong>{site.name}</strong>
                  <span className="muted"> · {site.count} ventas</span>
                </span>
                <span className="shifts__amounts">
                  {formatMoney(site.total)}
                  {Number(site.difference) < 0 ? (
                    <span className="shifts__missing">{differenceLabel(site.difference)}</span>
                  ) : null}
                </span>
              </li>
            ))}
          </ul>
        </section>
      ) : null}

      <section className="card shifts__block" aria-labelledby="shifts-list">
        <h3 id="shifts-list">Turnos</h3>
        <p className="muted">Quién llevó la caja, de qué hora a qué hora, y si lo recibido cuadró con lo esperado.</p>
        {failed ? <p className="alert">No se pudieron cargar los turnos.</p> : null}
        {!failed && !shifts ? <p className="muted">Cargando turnos…</p> : null}
        {shifts && shifts.length === 0 ? <p className="muted">No hubo turnos en este rango.</p> : null}
        {shifts && shifts.length > 0 ? (
          <ul className="shifts__list" aria-label="Turnos">
            {shifts.map((shift) => {
              const label = differenceLabel(shift.difference)
              const missing = (shift.difference ?? 0) < 0
              return (
                <li key={shift.id} className={missing ? 'shifts__row shifts__row--missing' : 'shifts__row'}>
                  <div className="shifts__summary">
                    <span>
                      <strong>{shift.responsible}</strong>
                      <span className="muted">
                        {' '}
                        · {siteName(shift.siteId)} · {formatDateTime(shift.openedAt)}
                        {shift.closedAt ? ` a ${formatDateTime(shift.closedAt)}` : ' · abierto'}
                      </span>
                    </span>
                    <span className="shifts__amounts">
                      {shift.status === 'CLOSED' ? (
                        <>
                          Esperado {formatMoney(String(shift.expectedAmount ?? 0))}
                          {shift.receivedAmount !== null ? ` · Recibido ${formatMoney(String(shift.receivedAmount))}` : ''}
                        </>
                      ) : null}
                      {label ? <span className={missing ? 'shifts__missing' : 'shifts__ok'}>{label}</span> : null}
                    </span>
                  </div>
                  {shift.status === 'CLOSED' ? (
                    <button
                      className="button button--secondary shifts__toggle"
                      type="button"
                      aria-expanded={open === shift.id}
                      onClick={() => setOpen(open === shift.id ? undefined : shift.id)}
                    >
                      {open === shift.id ? 'Ocultar detalle' : 'Ver detalle'}
                    </button>
                  ) : null}
                  {open === shift.id ? <ShiftDetail id={shift.id} /> : null}
                </li>
              )
            })}
          </ul>
        ) : null}
      </section>
    </div>
  )
}
