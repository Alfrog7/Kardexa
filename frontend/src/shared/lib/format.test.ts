import { describe, expect, it } from 'vitest'
import { businessDate, formatDateTime, formatMoney, formatQuantity } from './format'

// Intl usa espacios especiales (NBSP) entre el símbolo y el número; se normalizan para comparar.
const normalize = (value: string) => value.replace(/\s/g, ' ')

describe('formatMoney', () => {
  it('muestra soles con 2 decimales', () => {
    expect(normalize(formatMoney('1234.5'))).toBe('S/ 1,234.50')
    expect(normalize(formatMoney('0'))).toBe('S/ 0.00')
  })
})

describe('formatQuantity', () => {
  it('muestra hasta 3 decimales sin ceros sobrantes', () => {
    expect(formatQuantity('1.250')).toBe('1.25')
    expect(formatQuantity('3.000')).toBe('3')
    expect(formatQuantity('0.125')).toBe('0.125')
  })
})

describe('fechas del negocio', () => {
  it('muestra la hora en la zona de Lima', () => {
    expect(normalize(formatDateTime('2026-11-01T15:00:00Z'))).toContain('10:00')
  })

  it('usa la fecha de Lima, aunque en UTC ya sea el día siguiente', () => {
    expect(businessDate(new Date('2026-11-02T03:00:00Z'))).toBe('2026-11-01')
  })
})
