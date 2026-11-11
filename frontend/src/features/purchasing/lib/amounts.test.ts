import { describe, expect, it } from 'vitest'
import { centsToAmount, subtotalCents } from './amounts'

describe('subtotalCents', () => {
  it('multiplica sin perder precisión y redondea al céntimo', () => {
    expect(subtotalCents('2.5', '3.33')).toBe(833n)
    expect(subtotalCents('0.001', '0.50')).toBe(0n)
    expect(subtotalCents('1.005', '1.00')).toBe(101n)
  })

  it('devuelve null si la cantidad o el costo no son válidos', () => {
    expect(subtotalCents('', '1')).toBeNull()
    expect(subtotalCents('1', '1.234')).toBeNull()
  })
})

describe('centsToAmount', () => {
  it('escribe los céntimos con dos decimales', () => {
    expect(centsToAmount(22460n)).toBe('224.60')
    expect(centsToAmount(5n)).toBe('0.05')
  })
})
