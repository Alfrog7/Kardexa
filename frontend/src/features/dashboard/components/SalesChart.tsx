import { useRef, useState, type KeyboardEvent } from 'react'
import { formatMoney } from '@shared/lib/format'
import type { DaySales } from '../api/dashboard'
import { niceMax } from '../lib/chart'

/** Medidas del lienzo SVG; el gráfico se escala al ancho disponible. */
const WIDTH = 640
const HEIGHT = 240
const PLOT = { top: 12, right: 8, bottom: 28, left: 64 }
const TICKS = 4
/** Como máximo esta cantidad de etiquetas de fecha; en rangos largos se espacian. */
const MAX_DATE_LABELS = 8

const shortDate = new Intl.DateTimeFormat('es-PE', { day: 'numeric', month: 'short', timeZone: 'UTC' })
const longDate = new Intl.DateTimeFormat('es-PE', { weekday: 'long', day: 'numeric', month: 'long', timeZone: 'UTC' })

/** Las fechas ya son del negocio (`AAAA-MM-DD`): se formatean sin cambiar de zona. */
function asDate(date: string): Date {
  return new Date(`${date}T00:00:00Z`)
}

function salesLabel(count: number): string {
  return count === 1 ? '1 venta' : `${count} ventas`
}

function describe(day: DaySales): string {
  return `${longDate.format(asDate(day.date))}: ${formatMoney(day.total)}, ${salesLabel(day.count)}`
}

/**
 * Ventas por día en barras (HU-17, RF-22), en SVG propio sin librería. Una sola parada de tabulación: las flechas,
 * Inicio y Fin recorren los días. La tabla equivalente se puede abrir debajo.
 */
export function SalesChart({ days }: { days: DaySales[] }) {
  const [active, setActive] = useState<number>()
  const [focusIndex, setFocusIndex] = useState(Math.max(days.length - 1, 0))
  const bars = useRef<(SVGRectElement | null)[]>([])

  if (days.length === 0) return null

  const totals = days.map((day) => Number(day.total))
  const max = niceMax(Math.max(...totals))
  const plotWidth = WIDTH - PLOT.left - PLOT.right
  const plotHeight = HEIGHT - PLOT.top - PLOT.bottom
  const slot = plotWidth / days.length
  const barWidth = Math.max(slot * 0.7, 1)
  const labelStep = Math.ceil(days.length / MAX_DATE_LABELS)
  const y = (value: number) => PLOT.top + plotHeight - (value / max) * plotHeight
  const shown = active === undefined ? undefined : days[active]
  const tabbable = Math.min(focusIndex, days.length - 1)

  function move(event: KeyboardEvent<SVGRectElement>, index: number) {
    const next =
      event.key === 'ArrowRight' ? index + 1
      : event.key === 'ArrowLeft' ? index - 1
      : event.key === 'Home' ? 0
      : event.key === 'End' ? days.length - 1
      : undefined
    if (next === undefined) return
    event.preventDefault()
    const target = Math.min(Math.max(next, 0), days.length - 1)
    setFocusIndex(target)
    bars.current[target]?.focus()
  }

  return (
    <figure className="sales-chart">
      <figcaption className="sales-chart__caption">
        <span>Ventas por día</span>
        <span className="sales-chart__detail" aria-live="polite">
          {shown ? describe(shown) : 'Pasa el puntero o usa las flechas sobre una barra para ver el detalle.'}
        </span>
      </figcaption>
      <svg
        className="sales-chart__svg"
        viewBox={`0 0 ${WIDTH} ${HEIGHT}`}
        role="group"
        aria-label={`Ventas por día, ${days.length} días`}
        onMouseLeave={() => setActive(undefined)}
      >
        {Array.from({ length: TICKS + 1 }, (_, tick) => {
          const value = (max / TICKS) * tick
          return (
            <g key={tick} className="sales-chart__tick" aria-hidden="true">
              <line x1={PLOT.left} x2={WIDTH - PLOT.right} y1={y(value)} y2={y(value)} />
              <text x={PLOT.left - 6} y={y(value)} dy="0.32em" textAnchor="end">
                {formatMoney(value.toFixed(2))}
              </text>
            </g>
          )
        })}
        {days.map((day, index) => {
          const x = PLOT.left + slot * index + (slot - barWidth) / 2
          const top = y(totals[index] ?? 0)
          return (
            <g key={day.date}>
              {/* Franja transparente de todo el alto: facilita apuntar a los días en 0. */}
              <rect
                className="sales-chart__hit"
                x={PLOT.left + slot * index}
                y={PLOT.top}
                width={slot}
                height={plotHeight}
                onMouseEnter={() => setActive(index)}
                aria-hidden="true"
              />
              <rect
                ref={(element) => {
                  bars.current[index] = element
                }}
                className={active === index ? 'sales-chart__bar sales-chart__bar--active' : 'sales-chart__bar'}
                x={x}
                y={top}
                width={barWidth}
                height={Math.max(PLOT.top + plotHeight - top, 0)}
                role="img"
                aria-label={describe(day)}
                tabIndex={index === tabbable ? 0 : -1}
                onMouseEnter={() => setActive(index)}
                onFocus={() => {
                  setActive(index)
                  setFocusIndex(index)
                }}
                onBlur={() => setActive(undefined)}
                onKeyDown={(event) => move(event, index)}
              />
              {index % labelStep === 0 ? (
                <text className="sales-chart__date" x={x + barWidth / 2} y={HEIGHT - 8} textAnchor="middle" aria-hidden="true">
                  {shortDate.format(asDate(day.date))}
                </text>
              ) : null}
            </g>
          )
        })}
        <line className="sales-chart__axis" x1={PLOT.left} x2={WIDTH - PLOT.right} y1={y(0)} y2={y(0)} aria-hidden="true" />
      </svg>
      <details className="sales-chart__table">
        <summary>Ver como tabla</summary>
        <div className="data-table-card">
          <table className="data-table" aria-label="Ventas por día">
            <thead>
              <tr>
                <th scope="col">Fecha</th>
                <th scope="col" className="data-table__number">
                  Total
                </th>
                <th scope="col" className="data-table__number">
                  Ventas
                </th>
              </tr>
            </thead>
            <tbody>
              {days.map((day) => (
                <tr key={day.date}>
                  <td data-label="Fecha">{longDate.format(asDate(day.date))}</td>
                  <td data-label="Total" className="data-table__number">
                    {formatMoney(day.total)}
                  </td>
                  <td data-label="Ventas" className="data-table__number">
                    {day.count}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </details>
    </figure>
  )
}
