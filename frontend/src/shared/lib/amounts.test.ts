import { describe, expect, it } from 'vitest'
import { centsToAmount, exceeds, subtotalCents } from './amounts'

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

describe('exceeds', () => {
  it('compara cantidades con decimales sin pasar por números de coma flotante', () => {
    expect(exceeds('2.001', '2.000')).toBe(true)
    expect(exceeds('2', '2.000')).toBe(false)
    expect(exceeds('0.5', '1')).toBe(false)
    expect(exceeds('abc', '1')).toBe(false)
  })
})
