import type { Product } from '../api/products'

const UNIT_LABELS: Record<Product['unit'], string> = { UNIT: 'und.', KG: 'kg', LITER: 'L', BOX: 'caja' }

/** Abreviatura de la unidad para acompañar una cantidad: «12 kg», «3 und.». */
export function unitLabel(product: Product): string {
  return UNIT_LABELS[product.unit]
}
