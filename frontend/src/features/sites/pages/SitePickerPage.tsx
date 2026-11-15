import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router'
import { ApiError } from '@shared/api/http'
import { useSession } from '@shared/auth/session'
import { setActiveSite, useActiveSite } from '@shared/site/activeSite'
import { Icon } from '@shared/ui/Icon'
import { Logo } from '@shared/ui/Logo'
import { listSites, type Site } from '../api/sites'
import './SitePickerPage.css'

/**
 * Elección de sede del administrador: después de ingresar ve sus sedes en tarjetas y elige con cuál trabajar
 * (o «Todas», solo para consultar). El panel muestra siempre la sede elegida y deja cambiarla.
 */
export function SitePickerPage() {
  const { user, end } = useSession()
  const active = useActiveSite()
  const navigate = useNavigate()
  const from = (useLocation().state as { from?: string } | null)?.from
  const [sites, setSites] = useState<Site[]>()
  const [error, setError] = useState<string>()
  const firstName = user?.fullName.split(' ')[0]

  useEffect(() => {
    listSites().then(
      (all) => setSites(all.filter((site) => site.active)),
      (failure: unknown) => setError(failure instanceof ApiError ? failure.message : 'No se pudieron cargar las sedes.'),
    )
  }, [])

  function choose(id: string | null, name: string) {
    setActiveSite({ id, name })
    navigate(from ?? '/', { replace: true })
  }

  return (
    <main className="site-picker">
      <header className="site-picker__top">
        <Logo height={30} />
        <button className="button button--secondary" type="button" onClick={end}>
          Cerrar sesión
        </button>
      </header>
      <section className="site-picker__body">
        <h1 className="site-picker__title">¿Con qué sede quieres trabajar{firstName ? `, ${firstName}` : ''}?</h1>
        <p className="muted">
          Cada sede tiene su propio stock, sus ventas y sus trabajadores. Puedes cambiar de sede cuando quieras.
        </p>
        {error ? (
          <p className="auth__error" role="alert">
            {error}
          </p>
        ) : null}
        {sites === undefined && !error ? <p className="muted">Cargando sedes…</p> : null}
        {sites ? (
          <ul className="site-picker__grid">
            {sites.map((site) => (
              <li key={site.id}>
                <button
                  className="site-card"
                  type="button"
                  aria-current={active?.id === site.id ? 'true' : undefined}
                  onClick={() => choose(site.id, site.name)}
                >
                  <span className="site-card__icon">
                    <Icon name="store" size={26} />
                  </span>
                  <span className="site-card__name">{site.name}</span>
                  <span className="site-card__address">{site.address ?? 'Sin dirección'}</span>
                  {active?.id === site.id ? <span className="site-card__badge">Sede actual</span> : null}
                </button>
              </li>
            ))}
            <li>
              <button
                className="site-card site-card--all"
                type="button"
                aria-current={active && active.id === null ? 'true' : undefined}
                onClick={() => choose(null, 'Todas las sedes')}
              >
                <span className="site-card__icon">
                  <Icon name="grid" size={26} />
                </span>
                <span className="site-card__name">Todas las sedes</span>
                <span className="site-card__address">Solo consulta: ver el negocio completo</span>
                {active && active.id === null ? <span className="site-card__badge">Vista actual</span> : null}
              </button>
            </li>
          </ul>
        ) : null}
      </section>
    </main>
  )
}
