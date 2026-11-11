import { useCallback, useEffect, useState } from 'react'
import { ApiError } from '@shared/api/http'
import { useCanManage } from '@shared/auth/session'
import { Dialog } from '@shared/ui/Dialog'
import { Icon } from '@shared/ui/Icon'
import { Toast } from '@shared/ui/Toast'
import { createSupplier, listSuppliers, toSupplierData, updateSupplier, type Supplier, type SupplierData } from '../api/suppliers'
import { SupplierForm } from '../components/SupplierForm'
import './SuppliersPage.css'

/** Espera tras la última tecla antes de buscar. */
const SEARCH_DELAY_MS = 300

type FormMode = { kind: 'closed' } | { kind: 'create' } | { kind: 'edit'; supplier: Supplier }

function messageOf(error: unknown): string {
  return error instanceof ApiError ? error.message : 'Algo salió mal. Inténtalo otra vez.'
}

/** Gestión de proveedores (HU-11): buscar, registrar, editar, activar y desactivar. */
export function SuppliersPage() {
  const canManage = useCanManage()
  const [text, setText] = useState('')
  const [q, setQ] = useState('')
  const [suppliers, setSuppliers] = useState<Supplier[]>()
  const [loadError, setLoadError] = useState<string>()
  const [actionError, setActionError] = useState<string>()
  const [attempt, setAttempt] = useState(0)
  const [form, setForm] = useState<FormMode>({ kind: 'closed' })
  const [togglingId, setTogglingId] = useState<string>()
  const [notice, setNotice] = useState<string>()
  const dismissNotice = useCallback(() => setNotice(undefined), [])

  useEffect(() => {
    const timer = setTimeout(() => setQ(text.trim()), SEARCH_DELAY_MS)
    return () => clearTimeout(timer)
  }, [text])

  // Solo la última búsqueda actualiza la tabla.
  useEffect(() => {
    let current = true
    listSuppliers(q)
      .then((loaded) => {
        if (current) {
          setSuppliers(loaded)
          setLoadError(undefined)
        }
      })
      .catch((error: unknown) => {
        if (current) setLoadError(messageOf(error))
      })
    return () => {
      current = false
    }
  }, [q, attempt])

  function replace(updated: Supplier) {
    setSuppliers((current) => (current ?? []).map((item) => (item.id === updated.id ? updated : item)))
  }

  async function handleSubmit(data: SupplierData) {
    if (form.kind === 'edit') {
      replace(await updateSupplier(form.supplier.id, data))
    } else {
      await createSupplier(data)
      setAttempt((value) => value + 1)
    }
    setForm({ kind: 'closed' })
    setNotice(`Proveedor ${data.businessName} guardado.`)
  }

  async function handleToggle(supplier: Supplier) {
    setActionError(undefined)
    setTogglingId(supplier.id)
    try {
      const updated = await updateSupplier(supplier.id, { ...toSupplierData(supplier), active: !supplier.active })
      replace(updated)
      setNotice(`Proveedor ${updated.businessName} ${updated.active ? 'activado' : 'desactivado'}.`)
    } catch (error) {
      setActionError(messageOf(error))
    } finally {
      setTogglingId(undefined)
    }
  }

  return (
    <section className="page">
      <header className="page__header page__header--spread">
        <h1>Proveedores</h1>
        {canManage ? (
          <button className="button button--icon" type="button" aria-label="Nuevo proveedor" title="Nuevo proveedor" onClick={() => setForm({ kind: 'create' })}>
            <Icon name="plus" />
          </button>
        ) : null}
      </header>

      {form.kind !== 'closed' ? (
        <Dialog
          title={form.kind === 'edit' ? `Editar ${form.supplier.businessName}` : 'Nuevo proveedor'}
          onClose={() => setForm({ kind: 'closed' })}
        >
          <SupplierForm supplier={form.kind === 'edit' ? form.supplier : undefined} onSubmit={handleSubmit} />
        </Dialog>
      ) : null}

      {notice ? <Toast message={notice} onDismiss={dismissNotice} /> : null}

      <div className="suppliers__filters" role="search">
        <div className="field">
          <label htmlFor="suppliers-search">Buscar por RUC o razón social</label>
          <input id="suppliers-search" type="search" value={text} maxLength={150} onChange={(event) => setText(event.target.value)} />
        </div>
      </div>

      {actionError ? (
        <p className="alert" role="alert">
          {actionError}
        </p>
      ) : null}

      {loadError ? (
        <div className="alert" role="alert">
          <p>{loadError}</p>
          <button className="button button--secondary" type="button" onClick={() => setAttempt((value) => value + 1)}>
            Reintentar
          </button>
        </div>
      ) : suppliers === undefined ? (
        <p className="muted" role="status">
          Cargando proveedores…
        </p>
      ) : suppliers.length === 0 ? (
        <p className="muted">
          {q ? 'No hay proveedores que coincidan con la búsqueda.' : 'Aún no tienes proveedores. Registra el primero con «Nuevo proveedor».'}
        </p>
      ) : (
        <div className="card data-table-card">
          <table className="data-table" aria-label="Proveedores">
            <thead>
              <tr>
                <th scope="col">RUC</th>
                <th scope="col">Razón social</th>
                <th scope="col">Teléfono</th>
                <th scope="col">Correo</th>
                <th scope="col">Estado</th>
              </tr>
            </thead>
            <tbody>
              {suppliers.map((supplier) => (
                <tr key={supplier.id}>
                  <td data-label="RUC">{supplier.ruc}</td>
                  <td data-label="Razón social">
                    {canManage ? (
                      <button className="link-button" type="button" onClick={() => setForm({ kind: 'edit', supplier })}>
                        {supplier.businessName}
                      </button>
                    ) : (
                      supplier.businessName
                    )}
                  </td>
                  <td data-label="Teléfono">{supplier.phone ?? '—'}</td>
                  <td data-label="Correo" className="suppliers__email">
                    {supplier.email ?? '—'}
                  </td>
                  <td data-label="Estado">
                    <label className="switch">
                      <input
                        type="checkbox"
                        role="switch"
                        aria-label={`${supplier.businessName} activo`}
                        checked={supplier.active}
                        disabled={!canManage || togglingId === supplier.id}
                        onChange={() => void handleToggle(supplier)}
                      />
                      <span aria-hidden="true">{supplier.active ? 'Activo' : 'Desactivado'}</span>
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
