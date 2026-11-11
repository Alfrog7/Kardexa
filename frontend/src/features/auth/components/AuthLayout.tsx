import type { CSSProperties, ReactNode } from 'react'
import { Logo } from '@shared/ui/Logo'
import './AuthLayout.css'

interface AuthLayoutProps {
  title: string
  description: string
  children: ReactNode
}

interface Movement {
  date: string
  detail: string
  entry?: string
  exit?: string
  balance: string
  low?: boolean
}

/** Datos de ejemplo de la tarjeta de kárdex del panel de marca (no vienen del sistema). */
const MOVEMENTS: Movement[] = [
  { date: '02 nov', detail: 'Saldo inicial', balance: '18' },
  { date: '04 nov', detail: 'Compra a proveedor', entry: '24', balance: '42' },
  { date: '05 nov', detail: 'Venta del día', exit: '6', balance: '36' },
  { date: '06 nov', detail: 'Venta del día', exit: '9', balance: '27' },
  { date: '07 nov', detail: 'Venta del día', exit: '22', balance: '5', low: true },
]

const BLANK_ROWS = 4

/** Pantalla de ingreso: la tarjeta de kárdex sobre fondo de marca y, a la derecha, el formulario. */
export function AuthLayout({ title, description, children }: AuthLayoutProps) {
  return (
    <div className="auth">
      <aside className="auth__stage">
        <Logo height={34} onDark />
        <div className="auth__pitch">
          <p className="auth__headline">Cada producto con su propia tarjeta.</p>
          <p className="auth__lead">
            Lo que entra, lo que sale y lo que queda, anotado sin errores. Kardexa te avisa antes de que el estante
            se quede vacío.
          </p>
        </div>
        <figure className="kardex" aria-hidden="true">
          <header className="kardex__head">
            <div>
              <p className="kardex__title">Kárdex</p>
              <p className="kardex__item">Arroz extra 5 kg</p>
            </div>
            <dl className="kardex__meta">
              <div>
                <dt>Unidad</dt>
                <dd>Saco</dd>
              </div>
              <div>
                <dt>Mínimo</dt>
                <dd>8</dd>
              </div>
            </dl>
          </header>
          <table className="kardex__table">
            <thead>
              <tr>
                <th rowSpan={2}>Fecha</th>
                <th rowSpan={2}>Detalle</th>
                <th colSpan={3} className="kardex__group">
                  Cantidad
                </th>
              </tr>
              <tr>
                <th>Entra</th>
                <th>Sale</th>
                <th>Saldo</th>
              </tr>
            </thead>
            <tbody>
              {MOVEMENTS.map((row, index) => (
                <tr
                  key={row.date}
                  className={row.low ? 'kardex__row kardex__row--low' : 'kardex__row'}
                  style={{ '--i': index } as CSSProperties}
                >
                  <td>{row.date}</td>
                  <td>
                    {row.detail}
                    {row.low ? <span className="kardex__flag">Stock bajo</span> : null}
                  </td>
                  <td className="kardex__in">{row.entry}</td>
                  <td>{row.exit}</td>
                  <td className="kardex__balance">{row.balance}</td>
                </tr>
              ))}
              {Array.from({ length: BLANK_ROWS }, (_, index) => (
                <tr key={`blank-${index}`} className="kardex__row kardex__row--blank">
                  <td />
                  <td />
                  <td />
                  <td />
                  <td />
                </tr>
              ))}
            </tbody>
          </table>
          <figcaption className="kardex__note">Datos de ejemplo</figcaption>
        </figure>
      </aside>
      <main className="auth__main">
        <div className="auth__panel">
          <div className="auth__intro">
            <h1 className="auth__title">{title}</h1>
            <p className="auth__description">{description}</p>
          </div>
          {children}
        </div>
      </main>
    </div>
  )
}
