import { apiRequest } from '@shared/api/http'
import type { Supplier } from './suppliers'

export type PurchaseStatus = 'REGISTERED' | 'VOIDED'

/** Esquema `PurchaseItem` de docs/api/openapi.yaml. */
export interface PurchaseItem {
  productId: string
  productName: string
  quantity: string
  unitCost: string
  subtotal: string
}

/** Esquema `Purchase`. */
export interface Purchase {
  id: string
  supplier: Supplier
  supplierDocument: string | null
  purchaseDate: string
  status: PurchaseStatus
  voidReason: string | null
  total: string
  items: PurchaseItem[]
}

export interface PurchasePage {
  content: Purchase[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

/** Esquema `PurchaseRequest`: cantidades y montos viajan como texto decimal. */
export interface PurchaseRequest {
  supplierId: string
  supplierDocument: string | null
  purchaseDate: string
  items: { productId: string; quantity: string; unitCost: string }[]
}

/** Producto que ofrece el buscador del formulario, de `GET /products`. */
export interface ProductOption {
  id: string
  code: string
  name: string
}

export const PAGE_SIZE = 20
const PICKER_SIZE = 8

export function listPurchases(from: string, to: string, page: number): Promise<PurchasePage> {
  const params = new URLSearchParams({ from, to, page: String(page), size: String(PAGE_SIZE) })
  return apiRequest<PurchasePage>(`/purchases?${params}`)
}

export function registerPurchase(request: PurchaseRequest): Promise<Purchase> {
  return apiRequest<Purchase>('/purchases', { method: 'POST', body: JSON.stringify(request) })
}

export async function findProducts(q: string): Promise<ProductOption[]> {
  const params = new URLSearchParams({ q, size: String(PICKER_SIZE) })
  const page = await apiRequest<{ content: ProductOption[] }>(`/products?${params}`)
  return page.content
}
