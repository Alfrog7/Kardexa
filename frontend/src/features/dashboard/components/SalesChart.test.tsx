import { fireEvent, render, screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { formatMoney } from '@shared/lib/format'
import type { DaySales } from '../api/dashboard'
import { niceMax } from '../lib/chart'
import { SalesChart } from './SalesChart'

/** 30 días desde el 16 de octubre de 2026; solo el 1 de noviembre tiene ventas. */
function month(): DaySales[] {
  return Array.from({ length: 30 }, (_, index) => {
    const date = new Date(Date.UTC(2026, 9, 16 + index)).toISOString().slice(0, 10)
    return date === '2026-11-01' ? { date, total: '15.50', count: 2 } : { date, total: '0.00', count: 0 }
  })
}

const text = (value: string) => value.replace(/\s/g, ' ')

/** Texto del detalle que acompaña al gráfico (se anuncia con `aria-live`). */
const detail = () => text(document.querySelector('.sales-chart__detail')?.textContent ?? '')

describe('SalesChart', () => {
  it('un rango de 30 días muestra 30 barras, incluidos los días en 0, con texto alternativo', () => {
    render(<SalesChart days={month()} />)

    const chart = screen.getByRole('group', { name: 'Ventas por día, 30 días' })
    const bars = within(chart).getAllByRole('img')
    expect(bars).toHaveLength(30)
    expect(text(bars[16]?.getAttribute('aria-label') ?? '')).toBe(`domingo, 1 de noviembre: ${text(formatMoney('15.50'))}, 2 ventas`)
    expect(text(bars[0]?.getAttribute('aria-label') ?? '')).toBe(`viernes, 16 de octubre: ${text(formatMoney('0.00'))}, 0 ventas`)
  })

  it('espacia las etiquetas de fecha en rangos largos', () => {
    const { container } = render(<SalesChart days={month()} />)

    expect(container.querySelectorAll('.sales-chart__date')).toHaveLength(6)
  })

  it('con el teclado hay una sola parada y las flechas, Inicio y Fin recorren los días mostrando el detalle', () => {
    render(<SalesChart days={month()} />)

    const bars = screen.getAllByRole('img')
    expect(bars.filter((bar) => bar.getAttribute('tabindex') === '0')).toEqual([bars[29]])

    bars[29]?.focus()
    fireEvent.keyDown(bars[29] as Element, { key: 'Home' })
    expect(document.activeElement).toBe(bars[0])
    fireEvent.keyDown(bars[0] as Element, { key: 'ArrowLeft' })
    expect(document.activeElement).toBe(bars[0])
    fireEvent.keyDown(bars[0] as Element, { key: 'ArrowRight' })
    expect(document.activeElement).toBe(bars[1])
    expect(detail()).toMatch(/^Díasábado, 17 de octubre/)
    fireEvent.keyDown(bars[1] as Element, { key: 'End' })
    expect(document.activeElement).toBe(bars[29])
  })

  it('al pasar el puntero muestra fecha, total y número de ventas', () => {
    render(<SalesChart days={month()} />)

    fireEvent.mouseEnter(screen.getAllByRole('img')[16] as Element)
    expect(detail()).toBe(`Díadomingo, 1 de noviembre${text(formatMoney('15.50'))}2 ventas`)
  })

  it('sin puntero destaca el mejor día y, sin ventas, lo dice', () => {
    const { unmount } = render(<SalesChart days={month()} />)
    expect(detail()).toBe(`Mejor díadomingo, 1 de noviembre${text(formatMoney('15.50'))}2 ventas`)
    unmount()

    render(<SalesChart days={month().map((day) => ({ ...day, total: '0.00', count: 0 }))} />)
    expect(detail()).toBe('Sin ventas en este rango')
  })

  it('tiene una tabla equivalente solo para lectores de pantalla', () => {
    render(<SalesChart days={month()} />)

    const table = screen.getByRole('table', { name: 'Ventas por día' })
    expect(table.parentElement).toHaveClass('visually-hidden')
    expect(within(table).getAllByRole('row')).toHaveLength(31)
  })

  it('redondea el tope del eje a 1, 2, 5 o 10 por potencia de diez', () => {
    expect(niceMax(0)).toBe(1)
    expect(niceMax(15.5)).toBe(20)
    expect(niceMax(420)).toBe(500)
    expect(niceMax(7)).toBe(10)
    expect(niceMax(100)).toBe(100)
  })
})
