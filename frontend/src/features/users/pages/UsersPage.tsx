import { useCallback, useEffect, useState } from 'react'
import { ApiError } from '@shared/api/http'
import { Dialog } from '@shared/ui/Dialog'
import { Toast } from '@shared/ui/Toast'
import { createUser, listUsers, updateUser, type User } from '../api/users'
import { UserForm, type UserFormValues } from '../components/UserForm'

/** La ventana está cerrada, crea un usuario o edita el indicado. */
type FormMode = { kind: 'closed' } | { kind: 'create' } | { kind: 'edit'; user: User }

const ROLE_LABEL = { ADMIN: 'Administrador', EMPLOYEE: 'Empleado' } as const

function messageOf(error: unknown): string {
  return error instanceof ApiError ? error.message : 'Algo salió mal. Inténtalo otra vez.'
}

function sortByName(users: User[]): User[] {
  return [...users].sort((a, b) => a.fullName.localeCompare(b.fullName, 'es'))
}

/** Gestión de usuarios (HU-03): listar, crear, cambiar nombre y rol, activar y desactivar. Solo ADMIN. */
export function UsersPage() {
  const [users, setUsers] = useState<User[]>()
  const [loadError, setLoadError] = useState<string>()
  const [actionError, setActionError] = useState<string>()
  const [form, setForm] = useState<FormMode>({ kind: 'closed' })
  const [togglingId, setTogglingId] = useState<string>()
  const [loadAttempt, setLoadAttempt] = useState(0)
  const [notice, setNotice] = useState<string>()
  const dismissNotice = useCallback(() => setNotice(undefined), [])

  useEffect(() => {
    let current = true
    listUsers()
      .then((loaded) => {
        if (current) setUsers(sortByName(loaded))
      })
      .catch((error: unknown) => {
        if (current) setLoadError(messageOf(error))
      })
    return () => {
      current = false
    }
  }, [loadAttempt])

  function retryLoad() {
    setLoadError(undefined)
    setLoadAttempt((attempt) => attempt + 1)
  }

  function replace(updated: User) {
    setUsers((current) => sortByName((current ?? []).map((item) => (item.id === updated.id ? updated : item))))
  }

  async function handleSubmit(values: UserFormValues) {
    if (form.kind === 'edit') {
      replace(await updateUser(form.user.id, { fullName: values.fullName, role: values.role }))
    } else {
      const created = await createUser(values)
      setUsers((current) => sortByName([...(current ?? []), created]))
    }
    setForm({ kind: 'closed' })
    setNotice(`Usuario ${values.fullName.trim()} guardado.`)
  }

  async function handleToggle(user: User) {
    setActionError(undefined)
    setTogglingId(user.id)
    try {
      replace(await updateUser(user.id, { active: !user.active }))
    } catch (error) {
      // RN-11: el último administrador activo no se puede desactivar; la API explica por qué.
      setActionError(messageOf(error))
    } finally {
      setTogglingId(undefined)
    }
  }

  return (
    <section className="page">
      <header className="page__header page__header--spread">
        <h1>Usuarios</h1>
        <button className="button" type="button" onClick={() => setForm({ kind: 'create' })}>
          Nuevo usuario
        </button>
      </header>

      {form.kind !== 'closed' ? (
        <Dialog
          variant="drawer"
          title={form.kind === 'edit' ? `Editar ${form.user.fullName}` : 'Nuevo usuario'}
          onClose={() => setForm({ kind: 'closed' })}
        >
          <UserForm user={form.kind === 'edit' ? form.user : undefined} onSubmit={handleSubmit} />
        </Dialog>
      ) : null}

      {notice ? <Toast message={notice} onDismiss={dismissNotice} /> : null}

      {actionError ? (
        <p className="alert" role="alert">
          {actionError}
        </p>
      ) : null}

      {loadError ? (
        <div className="alert" role="alert">
          <p>{loadError}</p>
          <button className="button button--secondary" type="button" onClick={retryLoad}>
            Reintentar
          </button>
        </div>
      ) : !users ? (
        <p className="muted">Cargando usuarios…</p>
      ) : (
        <div className="card data-table-card">
          <table className="data-table">
            <thead>
              <tr>
                <th scope="col">Nombre</th>
                <th scope="col">Correo</th>
                <th scope="col">Rol</th>
                <th scope="col">Activo</th>
              </tr>
            </thead>
            <tbody>
              {users.map((user) => (
                <tr key={user.id}>
                  <td data-label="Nombre">
                    <button className="link-button" type="button" onClick={() => setForm({ kind: 'edit', user })}>
                      {user.fullName}
                    </button>
                  </td>
                  <td data-label="Correo">{user.email}</td>
                  <td data-label="Rol">{ROLE_LABEL[user.role]}</td>
                  <td data-label="Activo">
                    <label className="switch">
                      <input
                        type="checkbox"
                        role="switch"
                        aria-label={`${user.fullName} activo`}
                        checked={user.active}
                        disabled={togglingId === user.id}
                        onChange={() => void handleToggle(user)}
                      />
                    </label>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}
