import { ApiError, apiRequest } from '@shared/api/http'

/** Una línea del turno por producto (esquema `Shift` de docs/api/openapi.yaml). */
export interface ShiftItem {
  productId: string
  productName: string
  openingStock: string
  leftover: string | null
  soldByCount: string | null
  unitPrice: number | null
}

/** Lo recibido por medio de pago al cerrar (esquema `Payments`). */
export interface Payments {
  cash: number
  yape: number
  plin: number
  transfer: number
}

export interface Shift {
  id: string
  siteId: string
  responsibleId: string
  responsible: string
  status: 'OPEN' | 'CLOSED'
  openedAt: string
  closedAt: string | null
  registeredSales: number | null
  countedSales: number | null
  expectedAmount: number | null
  note: string | null
  payments: Payments | null
  receivedAmount: number | null
  /** Recibido − esperado, calculado en el servidor: negativo falta, positivo sobra. */
  difference: number | null
  items: ShiftItem[]
}

/** Producto que se cuenta al cerrar: los que tienen stock registrado en la sede. */
export interface CountProduct {
  id: string
  code: string
  name: string
  salePrice: string
  stock: string
}

/** El turno abierto de la sede, o `null` si todavía nadie marcó su ingreso (la API responde 404). */
export async function getCurrentShift(): Promise<Shift | null> {
  try {
    return await apiRequest<Shift>('/shifts/current')
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) return null
    throw error
  }
}

/** Marca el ingreso: abre el turno de la sede con una foto del stock. */
export function openShift(): Promise<Shift> {
  return apiRequest<Shift>('/shifts', { method: 'POST', body: '{}' })
}

/** Cierra el turno con el sobrante contado de cada producto (0 es válido), lo recibido por medio y una observación. */
export function closeShift(
  id: string,
  counts: { productId: string; leftover: string }[],
  payments?: Payments,
  note?: string,
): Promise<Shift> {
  return apiRequest<Shift>(`/shifts/${id}/close`, {
    method: 'POST',
    body: JSON.stringify({ counts, payments, note: note?.trim() ? note.trim() : undefined }),
  })
}

/** Los productos con stock en la sede (máximo 100: el tope de la API), para contar lo que sobró. */
export async function listCountProducts(): Promise<CountProduct[]> {
  const page = await apiRequest<{ content: CountProduct[] }>('/products?size=100')
  return page.content.filter((product) => Number(product.stock) > 0)
}
