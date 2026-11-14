import { useRef, useState, type KeyboardEvent } from 'react'
import { formatMoney } from '@shared/lib/format'
import type { DaySales } from '../api/dashboard'
import { niceMax } from '../lib/chart'

/** Medidas del lienzo SVG; el gráfico se escala al ancho disponible. */
const WIDTH = 640
const HEIGHT = 260
const PLOT = { top: 16, right: 12, bottom: 32, left: 52 }
const TICKS = 3
/** Como máximo esta cantidad de etiquetas de fecha; en rangos largos se espacian. */
const MAX_DATE_LABELS = 6
/** Ancho máximo de una barra, para que en rangos cortos no queden bloques enormes. */
const MAX_BAR_WIDTH = 28
/** Alto mínimo de la marca de un día sin ventas, para que se vea que el día existe. */
const EMPTY_BAR_HEIGHT = 2

const shortDate = new Intl.DateTimeFormat('es-PE', { day: 'numeric', month: 'short', timeZone: 'UTC' })
const longDate = new Intl.DateTimeFormat('es-PE', { weekday: 'long', day: 'numeric', month: 'long', timeZone: 'UTC' })
const axisMoney = new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN', maximumFractionDigits: 0 })

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

/** El día con más monto vendido; `undefined` si no hubo ventas en el rango. */
function bestDay(days: DaySales[]): DaySales | undefined {
  return days.reduce<DaySales | undefined>(
    (best, day) => (day.count > 0 && (!best || Number(day.total) > Number(best.total)) ? day : best),
    undefined,
  )
}

/**
 * Ventas por día en barras (HU-17, RF-22), en SVG propio sin librería. Una sola parada de tabulación: las flechas,
 * Inicio y Fin recorren los días. Los lectores de pantalla tienen además una tabla equivalente oculta a la vista.
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
  const barWidth = Math.min(Math.max(slot * 0.6, 1), MAX_BAR_WIDTH)
  const labelStep = Math.ceil(days.length / MAX_DATE_LABELS)
  const baseline = PLOT.top + plotHeight
  const y = (value: number) => baseline - (value / max) * plotHeight
  const best = bestDay(days)
  const shown = active === undefined ? undefined : days[active]
  const highlighted = shown ?? best
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
      <figcaption className="sales-chart__header">
        <span className="sales-chart__title">Ventas por día</span>
        <span className="sales-chart__detail" aria-live="polite">
          {highlighted ? (
            <>
              <span className="sales-chart__detail-label">{shown ? 'Día' : 'Mejor día'}</span>
              <span className="sales-chart__detail-date">{longDate.format(asDate(highlighted.date))}</span>
              <span className="sales-chart__detail-total">{formatMoney(highlighted.total)}</span>
              <span className="sales-chart__detail-count">{salesLabel(highlighted.count)}</span>
            </>
          ) : (
            <span className="sales-chart__detail-label">Sin ventas en este rango</span>
          )}
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
              {tick > 0 ? <line x1={PLOT.left} x2={WIDTH - PLOT.right} y1={y(value)} y2={y(value)} /> : null}
              <text x={PLOT.left - 8} y={y(value)} dy="0.32em" textAnchor="end">
                {axisMoney.format(value)}
              </text>
            </g>
          )
        })}
        {days.map((day, index) => {
          const x = PLOT.left + slot * index + (slot - barWidth) / 2
          const empty = (totals[index] ?? 0) === 0
          const height = empty ? EMPTY_BAR_HEIGHT : Math.max(baseline - y(totals[index] ?? 0), EMPTY_BAR_HEIGHT)
          const classes = ['sales-chart__bar']
          if (empty) classes.push('sales-chart__bar--empty')
          if (day === best) classes.push('sales-chart__bar--best')
          if (active === index) classes.push('sales-chart__bar--active')
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
                className={classes.join(' ')}
                x={x}
                y={baseline - height}
                width={barWidth}
                height={height}
                rx={Math.min(barWidth / 4, 4)}
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
        <line className="sales-chart__axis" x1={PLOT.left} x2={WIDTH - PLOT.right} y1={baseline} y2={baseline} aria-hidden="true" />
      </svg>
      <table className="visually-hidden" aria-label="Ventas por día">
        <thead>
          <tr>
            <th scope="col">Fecha</th>
            <th scope="col">Total</th>
            <th scope="col">Ventas</th>
          </tr>
        </thead>
        <tbody>
          {days.map((day) => (
            <tr key={day.date}>
              <td>{longDate.format(asDate(day.date))}</td>
              <td>{formatMoney(day.total)}</td>
              <td>{day.count}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </figure>
  )
}
