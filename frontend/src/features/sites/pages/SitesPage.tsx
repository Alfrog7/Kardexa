import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router'
import { ApiError } from '@shared/api/http'
import { setActiveSite } from '@shared/site/activeSite'
import { ActionButton, Actions } from '@shared/ui/ActionButton'
import { Dialog } from '@shared/ui/Dialog'
import { Icon } from '@shared/ui/Icon'
import { Toast } from '@shared/ui/Toast'
import { createSite, deleteSite, listSites, setSiteActive, updateSite, type Site, type SiteData } from '../api/sites'
import { SiteForm } from '../components/SiteForm'
import './SitesPage.css'

type FormMode = { kind: 'closed' } | { kind: 'create' } | { kind: 'edit'; site: Site }

/** Eliminación en curso: confirma, y si la API la rechaza explica por qué y ofrece inhabilitar. */
interface Removal {
  site: Site
  busy?: boolean
  error?: string
}

function messageOf(error: unknown): string {
  return error instanceof ApiError ? error.message : 'Algo salió mal. Inténtalo otra vez.'
}

/** Gestión de sedes (HU-19), solo del administrador: ver, registrar, editar, eliminar, inhabilitar y habilitar. */
export function SitesPage() {
  const [sites, setSites] = useState<Site[]>()
  const [loadError, setLoadError] = useState<string>()
  const [actionError, setActionError] = useState<string>()
  const [attempt, setAttempt] = useState(0)
  const [form, setForm] = useState<FormMode>({ kind: 'closed' })
  const [togglingId, setTogglingId] = useState<string>()
  const [viewing, setViewing] = useState<Site>()
  const [removal, setRemoval] = useState<Removal>()
  const navigate = useNavigate()
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

  function workHere(site: Site) {
    setActiveSite({ id: site.id, name: site.name })
    navigate('/')
  }

  async function confirmRemoval() {
    if (!removal) return
    setRemoval({ ...removal, busy: true, error: undefined })
    try {
      await deleteSite(removal.site.id)
      setRemoval(undefined)
      setNotice(`Sede ${removal.site.name} eliminada.`)
      setAttempt((value) => value + 1)
    } catch (error) {
      setRemoval({ ...removal, busy: false, error: messageOf(error) })
    }
  }

  /** Alternativa cuando la sede tiene historial: inhabilitarla conserva todo. */
  async function deactivateInstead() {
    if (!removal) return
    const { site } = removal
    setRemoval({ ...removal, busy: true })
    try {
      replace(await setSiteActive(site.id, false))
      setRemoval(undefined)
      setNotice(`Sede ${site.name} inhabilitada.`)
    } catch (error) {
      setRemoval({ ...removal, busy: false, error: messageOf(error) })
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

      {viewing ? (
        <Dialog title={viewing.name} onClose={() => setViewing(undefined)}>
          <div className="form">
            <dl className="sites__details">
              <dt>Nombre</dt>
              <dd>{viewing.name}</dd>
              <dt>Dirección</dt>
              <dd>{viewing.address ?? 'Sin dirección'}</dd>
              <dt>Estado</dt>
              <dd>{viewing.active ? 'Habilitada' : 'Inhabilitada'}</dd>
            </dl>
            <div className="dialog__actions">
              <button className="button button--secondary" type="button" onClick={() => setViewing(undefined)}>
                Cerrar
              </button>
              {viewing.active ? (
                <button className="button" type="button" onClick={() => workHere(viewing)}>
                  Trabajar en esta sede
                </button>
              ) : null}
            </div>
          </div>
        </Dialog>
      ) : null}

      {removal ? (
        <Dialog title={`Eliminar ${removal.site.name}`} onClose={() => setRemoval(undefined)}>
          <div className="form">
            {removal.error ? (
              <>
                <p className="alert" role="alert">
                  {removal.error}
                </p>
                {removal.site.active ? (
                  <p>Al inhabilitarla deja de usarse, pero su historial se conserva y puedes habilitarla cuando quieras.</p>
                ) : null}
                <div className="dialog__actions">
                  <button className="button button--secondary" type="button" onClick={() => setRemoval(undefined)}>
                    Cancelar
                  </button>
                  {removal.site.active ? (
                    <button className="button" type="button" disabled={removal.busy} onClick={() => void deactivateInstead()}>
                      Inhabilitar sede
                    </button>
                  ) : null}
                </div>
              </>
            ) : (
              <>
                <p>
                  Se eliminará <strong>{removal.site.name}</strong>. Esta acción no se puede deshacer.
                </p>
                <p className="muted">Solo se puede eliminar una sede que nunca tuvo stock, ventas, compras ni trabajadores.</p>
                <div className="dialog__actions">
                  <button className="button button--secondary" type="button" onClick={() => setRemoval(undefined)}>
                    Cancelar
                  </button>
                  <button className="button button--danger" type="button" disabled={removal.busy} onClick={() => void confirmRemoval()}>
                    Eliminar sede
                  </button>
                </div>
              </>
            )}
          </div>
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
                <th scope="col">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {sites.map((site) => (
                <tr key={site.id}>
                  <td data-label="Nombre">{site.name}</td>
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
                  <td data-label="Acciones">
                    <Actions label={`Acciones de ${site.name}`}>
                      <ActionButton icon="eye" label={`Ver ${site.name}`} onClick={() => setViewing(site)} />
                      <ActionButton icon="pencil" label={`Editar ${site.name}`} onClick={() => setForm({ kind: 'edit', site })} />
                      <ActionButton icon="trash" label={`Eliminar ${site.name}`} tone="danger" onClick={() => setRemoval({ site })} />
                    </Actions>
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
