import { useCallback, useEffect, useRef, useState } from 'react'
import { NavLink, Outlet } from 'react-router'
import { ChangePasswordForm } from '@features/auth'
import { useSession } from '@shared/auth/session'
import { Dialog } from '@shared/ui/Dialog'
import { Icon, KMark } from '@shared/ui/Icon'
import { Logo } from '@shared/ui/Logo'
import { Toast } from '@shared/ui/Toast'
import { navigation } from '../navigation'
import { NavIcon } from './NavIcon'
import './PanelLayout.css'

const COLLAPSED_KEY = 'kardexa.sidebar-collapsed'

/** Iniciales para el avatar: primera letra de las dos primeras palabras del nombre. */
function initials(fullName = '') {
  return fullName
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((word) => word[0]?.toUpperCase())
    .join('')
}

function readCollapsed() {
  try {
    return localStorage.getItem(COLLAPSED_KEY) === '1'
  } catch {
    return false
  }
}

/** Estructura del panel: barra lateral (contraíble) con los módulos y el contenido de la ruta activa. */
export function PanelLayout() {
  const { user, end } = useSession()
  const [collapsed, setCollapsed] = useState(readCollapsed)
  const [menuOpen, setMenuOpen] = useState(false)
  const [changingPassword, setChangingPassword] = useState(false)
  const [notice, setNotice] = useState<string>()
  const accountRef = useRef<HTMLDivElement>(null)
  const dismissNotice = useCallback(() => setNotice(undefined), [])
  const visibleNavigation = navigation.filter((item) => !('adminOnly' in item) || user?.role === 'ADMIN')

  useEffect(() => {
    if (!menuOpen) return
    function onPointerDown(event: PointerEvent) {
      if (!accountRef.current?.contains(event.target as Node)) setMenuOpen(false)
    }
    function onKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setMenuOpen(false)
        accountRef.current?.querySelector('button')?.focus()
      }
    }
    document.addEventListener('pointerdown', onPointerDown)
    document.addEventListener('keydown', onKeyDown)
    return () => {
      document.removeEventListener('pointerdown', onPointerDown)
      document.removeEventListener('keydown', onKeyDown)
    }
  }, [menuOpen])

  function toggleSidebar() {
    const next = !collapsed
    setCollapsed(next)
    try {
      localStorage.setItem(COLLAPSED_KEY, next ? '1' : '0')
    } catch {
      /* sin almacenamiento: la preferencia solo dura mientras la página siga abierta */
    }
  }

  return (
    <div className={collapsed ? 'panel panel--collapsed' : 'panel'}>
      <aside className="panel__sidebar">
        <div className="panel__top">
          {collapsed ? (
            <button
              className="panel__mark"
              type="button"
              aria-label="Expandir barra lateral"
              title="Expandir barra lateral"
              onClick={toggleSidebar}
            >
              <span className="panel__mark-k">
                <KMark />
              </span>
              <span className="panel__mark-open">
                <Icon name="panelOpen" />
              </span>
            </button>
          ) : null}
          <span className="panel__logo">
            <Logo onDark />
          </span>
          {collapsed ? null : (
            <button
              className="panel__toggle"
              type="button"
              aria-label="Contraer barra lateral"
              title="Contraer barra lateral"
              onClick={toggleSidebar}
            >
              <Icon name="panelClose" />
            </button>
          )}
        </div>
        <nav aria-label="Módulos">
          <ul className="panel__nav">
            {visibleNavigation.map((item) => (
              <li key={item.to}>
                <NavLink to={item.to} end={item.to === '/'} className="panel__link" title={item.label}>
                  <NavIcon to={item.to} />
                  <span className="panel__label">{item.label}</span>
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>
        <div className="panel__account" ref={accountRef}>
          {menuOpen ? (
            <div className="panel__menu" role="group" aria-label="Opciones de la cuenta">
              <button
                className="panel__menu-item"
                type="button"
                onClick={() => {
                  setMenuOpen(false)
                  setChangingPassword(true)
                }}
              >
                <Icon name="key" />
                Cambiar contraseña
              </button>
              <button className="panel__menu-item" type="button" onClick={end}>
                <Icon name="logout" />
                Cerrar sesión
              </button>
            </div>
          ) : null}
          <button
            className="panel__account-button"
            type="button"
            aria-label="Menú de la cuenta"
            aria-haspopup="true"
            aria-expanded={menuOpen}
            title={collapsed ? user?.fullName : undefined}
            onClick={() => setMenuOpen((open) => !open)}
          >
            <span className="panel__avatar" aria-hidden="true">
              {initials(user?.fullName)}
            </span>
            <span className="panel__person">
              <span className="panel__user-name">{user?.fullName}</span>
              <span className="panel__user-role">{user?.role === 'ADMIN' ? 'Administrador' : 'Empleado'}</span>
            </span>
            <span className="panel__account-chevron">
              <Icon name="chevronUp" size={18} />
            </span>
          </button>
        </div>
      </aside>
      <main className="panel__content">
        <Outlet />
      </main>
      {changingPassword ? (
        <Dialog title="Cambiar contraseña" onClose={() => setChangingPassword(false)}>
          <ChangePasswordForm
            onChanged={() => {
              setChangingPassword(false)
              setNotice('Contraseña actualizada.')
            }}
          />
        </Dialog>
      ) : null}
      {notice ? <Toast message={notice} onDismiss={dismissNotice} /> : null}
    </div>
  )
}
