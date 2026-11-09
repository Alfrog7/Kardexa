import { apiRequest } from '@shared/api/http'

export type Unit = 'UNIT' | 'KG' | 'LITER' | 'BOX'

/** Esquema `Product` de docs/api/openapi.yaml. Los decimales llegan como string. */
export interface Product {
  id: string
  code: string
  name: string
  categoryId: string
  categoryName: string
  unit: Unit
  salePrice: string
  minStock: string
  active: boolean
  referenceCost: string
  stock: string
  critical: boolean
}

/** Esquema `ProductPage`. */
export interface ProductPage {
  content: Product[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

/** Esquema `ProductRequest`. */
export interface ProductRequest {
  code: string
  name: string
  categoryId: string
  unit: Unit
  salePrice: string
  minStock: string
  active?: boolean
}

export interface ProductSearch {
  q: string
  categoryId: string
  criticalOnly: boolean
  /** Incluye los productos inhabilitados, para gestionarlos y reactivarlos. */
  includeInactive: boolean
  page: number
}

export function searchProducts({ q, categoryId, criticalOnly, includeInactive, page }: ProductSearch): Promise<ProductPage> {
  const params = new URLSearchParams({ page: String(page) })
  if (q) params.set('q', q)
  if (categoryId) params.set('categoryId', categoryId)
  if (criticalOnly) params.set('criticalOnly', 'true')
  if (includeInactive) params.set('includeInactive', 'true')
  return apiRequest<ProductPage>(`/products?${params}`)
}

export function createProduct(request: ProductRequest): Promise<Product> {
  return apiRequest<Product>('/products', { method: 'POST', body: JSON.stringify(request) })
}

export function updateProduct(id: string, request: ProductRequest): Promise<Product> {
  return apiRequest<Product>(`/products/${id}`, { method: 'PUT', body: JSON.stringify(request) })
}
