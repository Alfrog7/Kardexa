import { apiRequest } from '@shared/api/http'
import type { Role, SessionUser } from '@shared/auth/session'

export type User = SessionUser

interface UserPage {
  content: User[]
}

export interface CreateUserRequest {
  fullName: string
  email: string
  password: string
  role: Role
}

export interface UpdateUserRequest {
  fullName?: string
  role?: Role
  active?: boolean
}

/** `GET /users` (HU-03, solo ADMIN). La lista de un negocio pequeño cabe en una página de 100. */
export async function listUsers(): Promise<User[]> {
  const page = await apiRequest<UserPage>('/users?size=100')
  return page.content
}

export function createUser(request: CreateUserRequest): Promise<User> {
  return apiRequest<User>('/users', { method: 'POST', body: JSON.stringify(request) })
}

/** `PATCH /users/{id}`: cambia solo lo que se envía. El último ADMIN activo responde 422 (RN-11). */
export function updateUser(id: string, request: UpdateUserRequest): Promise<User> {
  return apiRequest<User>(`/users/${id}`, { method: 'PATCH', body: JSON.stringify(request) })
}
