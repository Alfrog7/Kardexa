import { apiRequest } from '@shared/api/http'

export type SaleStatus = 'REGISTERED' | 'VOIDED'

/** Esquema `SaleItem` de docs/api/openapi.yaml: código, nombre y precio copiados al vender (RN-06). */
export interface SaleItem {
  productId: string
  productCode: string
  productName: string
  quantity: string
  unitPrice: string
  subtotal: string
}

/** Esquema `Sale`: el comprobante interno de una venta (RF-19). */
export interface Sale {
  id: string
  number: string
  soldAt: string
  sellerId: string
  seller: string
  status: SaleStatus
  voidReason: string | null
  total: string
  items: SaleItem[]
}

/** Esquema `SaleRequest`: el precio no viaja, lo pone el catálogo. */
export interface SaleRequest {
  items: { productId: string; quantity: string }[]
}

/** Producto que ofrece el buscador de la venta, de `GET /products` (solo activos). */
export interface ProductOption {
  id: string
  code: string
  name: string
  salePrice: string
  stock: string
}

const PICKER_SIZE = 8

export function registerSale(request: SaleRequest): Promise<Sale> {
  return apiRequest<Sale>('/sales', { method: 'POST', body: JSON.stringify(request) })
}

export async function findProducts(q: string): Promise<ProductOption[]> {
  const params = new URLSearchParams({ q, size: String(PICKER_SIZE) })
  const page = await apiRequest<{ content: ProductOption[] }>(`/products?${params}`)
  return page.content
}
