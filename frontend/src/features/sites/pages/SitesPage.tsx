import { useCallback, useEffect, useState } from 'react'
import { ApiError } from '@shared/api/http'
import { Dialog } from '@shared/ui/Dialog'
import { Icon } from '@shared/ui/Icon'
import { Toast } from '@shared/ui/Toast'
import { createSite, listSites, setSiteActive, updateSite, type Site, type SiteData } from '../api/sites'
import { SiteForm } from '../components/SiteForm'
import './SitesPage.css'

type FormMode = { kind: 'closed' } | { kind: 'create' } | { kind: 'edit'; site: Site }

function messageOf(error: unknown): string {
  return error instanceof ApiError ? error.message : 'Algo salió mal. Inténtalo otra vez.'
}

/** Gestión de sedes (HU-19), solo del administrador: registrar, editar, inhabilitar y habilitar. */
export function SitesPage() {
  const [sites, setSites] = useState<Site[]>()
  const [loadError, setLoadError] = useState<string>()
  const [actionError, setActionError] = useState<string>()
  const [attempt, setAttempt] = useState(0)
  const [form, setForm] = useState<FormMode>({ kind: 'closed' })
  const [togglingId, setTogglingId] = useState<string>()
  const [notice, setNotice] = useState<string>()
  const dismissNotice = useCallback(() => setNotice(undefined), [])

  useEffect(() => {
    let current = true
    listSites()
      .then((loaded) => {
        if (current) {
          setSites(loaded)
          setLoadError(undefined)
        }
      })
      .catch((error: unknown) => {
        if (current) setLoadError(messageOf(error))
      })
    return () => {
      current = false
    }
  }, [attempt])

  function replace(updated: Site) {
    setSites((current) => (current ?? []).map((item) => (item.id === updated.id ? updated : item)))
  }

  async function handleSubmit(data: SiteData) {
    if (form.kind === 'edit') {
      replace(await updateSite(form.site.id, data))
    } else {
      await createSite(data)
      setAttempt((value) => value + 1)
    }
    setForm({ kind: 'closed' })
    setNotice(`Sede ${data.name} guardada.`)
  }

  async function handleToggle(site: Site) {
    setActionError(undefined)
    setTogglingId(site.id)
    try {
      const updated = await setSiteActive(site.id, !site.active)
      replace(updated)
      setNotice(`Sede ${updated.name} ${updated.active ? 'habilitada' : 'inhabilitada'}.`)
    } catch (error) {
      setActionError(messageOf(error))
    } finally {
      setTogglingId(undefined)
    }
  }

  return (
    <section className="page">
      <header className="page__header page__header--spread">
        <h1>Sedes</h1>
        <button className="button button--icon" type="button" aria-label="Nueva sede" title="Nueva sede" onClick={() => setForm({ kind: 'create' })}>
          <Icon name="plus" />
        </button>
      </header>

      {form.kind !== 'closed' ? (
        <Dialog title={form.kind === 'edit' ? `Editar ${form.site.name}` : 'Nueva sede'} onClose={() => setForm({ kind: 'closed' })}>
          <SiteForm site={form.kind === 'edit' ? form.site : undefined} onSubmit={handleSubmit} />
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
          <button className="button button--secondary" type="button" onClick={() => setAttempt((value) => value + 1)}>
            Reintentar
          </button>
        </div>
      ) : sites === undefined ? (
        <p className="muted" role="status">
          Cargando sedes…
        </p>
      ) : sites.length === 0 ? (
        <p className="muted">Aún no tienes sedes. Registra la primera con «Nueva sede».</p>
      ) : (
        <div className="card data-table-card">
          <table className="data-table" aria-label="Sedes">
            <thead>
              <tr>
                <th scope="col">Nombre</th>
                <th scope="col">Dirección</th>
                <th scope="col">Estado</th>
              </tr>
            </thead>
            <tbody>
              {sites.map((site) => (
                <tr key={site.id}>
                  <td data-label="Nombre">
                    <button className="link-button" type="button" onClick={() => setForm({ kind: 'edit', site })}>
                      {site.name}
                    </button>
                  </td>
                  <td data-label="Dirección" className="sites__address">
                    {site.address ?? '—'}
                  </td>
                  <td data-label="Estado">
                    <label className="switch">
                      <input
                        type="checkbox"
                        role="switch"
                        aria-label={`${site.name} habilitada`}
                        checked={site.active}
                        disabled={togglingId === site.id}
                        onChange={() => void handleToggle(site)}
                      />
                      <span aria-hidden="true">{site.active ? 'Habilitada' : 'Inhabilitada'}</span>
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
