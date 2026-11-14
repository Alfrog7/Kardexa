import { apiRequest } from '@shared/api/http'

/** Datos del producto que necesita esta pantalla, de `GET /products`. */
export interface ProductOption {
  id: string
  code: string
  name: string
  /** Stock actual; el kárdex lo explica movimiento a movimiento. Falta mientras se carga. */
  stock?: string
}

export type Direction = 'IN' | 'OUT'
export type MovementType = 'PURCHASE' | 'SALE' | 'ADJUSTMENT' | 'PURCHASE_VOID' | 'SALE_VOID'
export type AdjustmentReason = 'SHRINKAGE' | 'PHYSICAL_COUNT' | 'CORRECTION'

/** Esquema `StockMovement` de docs/api/openapi.yaml. Los decimales llegan como string. */
export interface StockMovement {
  id: string
  occurredAt: string
  type: MovementType
  direction: Direction
  quantity: string
  balanceAfter: string
  sourceLabel?: string
  user: string
}

/** Esquema `Kardex`. */
export interface Kardex {
  productId: string
  productName: string
  openingBalance: string
  closingBalance: string
  movements: StockMovement[]
}

/** Esquema `StockAdjustmentRequest`. */
export interface StockAdjustmentRequest {
  productId: string
  direction: Direction
  quantity: string
  reason: AdjustmentReason
  note?: string
}

const PICKER_SIZE = 8

export async function findProducts(q: string): Promise<ProductOption[]> {
  const params = new URLSearchParams({ q, size: String(PICKER_SIZE) })
  const page = await apiRequest<{ content: ProductOption[] }>(`/products?${params}`)
  return page.content
}

export function getProduct(id: string): Promise<ProductOption> {
  return apiRequest<ProductOption>(`/products/${id}`)
}

export function getKardex(productId: string, from: string, to: string): Promise<Kardex> {
  const params = new URLSearchParams()
  if (from) params.set('from', from)
  if (to) params.set('to', to)
  return apiRequest<Kardex>(`/products/${productId}/kardex?${params}`)
}

export function registerAdjustment(request: StockAdjustmentRequest): Promise<StockMovement> {
  return apiRequest<StockMovement>('/stock-adjustments', { method: 'POST', body: JSON.stringify(request) })
}

/** Un movimiento del historial general, con su producto. */
export interface MovementEntry {
  productId: string
  productCode: string
  productName: string
  movement: StockMovement
}

export interface MovementPage {
  content: MovementEntry[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export const HISTORY_PAGE_SIZE = 20

/** `GET /stock-movements`: todos los movimientos, del más reciente al más antiguo. */
export function listMovements(from: string, to: string, type: MovementType | '', page: number): Promise<MovementPage> {
  const params = new URLSearchParams({ page: String(page), size: String(HISTORY_PAGE_SIZE) })
  if (from) params.set('from', from)
  if (to) params.set('to', to)
  if (type) params.set('type', type)
  return apiRequest<MovementPage>(`/stock-movements?${params}`)
}
