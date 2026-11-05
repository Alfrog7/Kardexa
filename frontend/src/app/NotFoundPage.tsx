import { Link } from 'react-router'
import { routes } from './navigation'

export function NotFoundPage() {
  return (
    <section className="page">
      <h1>Página no encontrada</h1>
      <div className="card">
        <p>La dirección no existe o se movió.</p>
        <Link to={routes.dashboard}>Volver al inicio</Link>
      </div>
    </section>
  )
}
