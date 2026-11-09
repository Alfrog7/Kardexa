import { apiRequest } from '@shared/api/http'

/** Esquema `Category` de docs/api/openapi.yaml. */
export interface Category {
  id: string
  name: string
  active: boolean
}

export function listCategories(): Promise<Category[]> {
  return apiRequest<Category[]>('/categories')
}

export function createCategory(name: string): Promise<Category> {
  return apiRequest<Category>('/categories', {
    method: 'POST',
    body: JSON.stringify({ name }),
  })
}

export function updateCategory(id: string, name: string, active: boolean): Promise<Category> {
  return apiRequest<Category>(`/categories/${id}`, {
    method: 'PUT',
    body: JSON.stringify({ name, active }),
  })
}
