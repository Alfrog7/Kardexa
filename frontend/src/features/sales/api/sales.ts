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

export interface SalePage {
  content: Sale[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

/** Vendedor para el filtro del historial (solo `ADMIN`, de `GET /users`). */
export interface Seller {
  id: string
  fullName: string
}

export const PAGE_SIZE = 20

/** HU-16: fechas inclusivas en hora de Lima. Al `EMPLEADO` la API solo le devuelve sus ventas. */
export function listSales(from: string, to: string, sellerId: string, page: number): Promise<SalePage> {
  const params = new URLSearchParams({ from, to, page: String(page), size: String(PAGE_SIZE) })
  if (sellerId) params.set('sellerId', sellerId)
  return apiRequest<SalePage>(`/sales?${params}`)
}

export async function listSellers(): Promise<Seller[]> {
  const page = await apiRequest<{ content: Seller[] }>('/users?size=100')
  return page.content
}

/** HU-15: anula una venta registrada (solo `ADMIN`); el motivo es obligatorio, de 3 a 255 caracteres. */
export function voidSale(id: string, reason: string): Promise<Sale> {
  return apiRequest<Sale>(`/sales/${id}/void`, { method: 'POST', body: JSON.stringify({ reason }) })
}
