import type { MovementType } from '../api/inventory'

export const MOVEMENT_LABELS: Record<MovementType, string> = {
  PURCHASE: 'Compra',
  SALE: 'Venta',
  ADJUSTMENT: 'Ajuste',
  PURCHASE_VOID: 'Anulación de compra',
  SALE_VOID: 'Anulación de venta',
}
