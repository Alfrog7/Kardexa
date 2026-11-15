import { apiRequest } from '@shared/api/http'

/** Producto de la lista rápida, de `GET /products` (solo activos; el stock es el de la sede). */
export interface ReceiptProduct {
  id: string
  code: string
  name: string
  stock: string
}

/** Esquema `ReceiptRequest` de docs/api/openapi.yaml: sin costo ni proveedor. */
export interface ReceiptRequest {
  items: { productId: string; quantity: string }[]
}

/** Esquema `Receipt`. */
export interface Receipt {
  id: string
  siteId: string
  createdAt: string
  items: { productId: string; quantity: string }[]
}

/** Una línea del historial de entradas: un movimiento `RECEIPT` del kárdex de la sede. */
export interface ReceiptEntry {
  productName: string
  movement: { id: string; occurredAt: string; quantity: string; user: string }
}

const LIST_SIZE = 100

/** La lista de productos para marcar lo que llegó (máximo 100: el tope de la API). */
export async function listReceiptProducts(): Promise<ReceiptProduct[]> {
  const page = await apiRequest<{ content: ReceiptProduct[] }>(`/products?size=${LIST_SIZE}`)
  return page.content
}

export function registerReceipt(request: ReceiptRequest): Promise<Receipt> {
  return apiRequest<Receipt>('/receipts', { method: 'POST', body: JSON.stringify(request) })
}

/** Las entradas de un día (fecha del negocio, `AAAA-MM-DD`) en la sede de quien consulta. */
export async function listReceiptsOfDay(day: string): Promise<ReceiptEntry[]> {
  const params = new URLSearchParams({ from: day, to: day, type: 'RECEIPT', size: String(LIST_SIZE) })
  const page = await apiRequest<{ content: ReceiptEntry[] }>(`/stock-movements?${params}`)
  return page.content
}
