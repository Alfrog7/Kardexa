import { Logo } from '@shared/ui/Logo'
import './LoginPage.css'

/**
 * Inicio de sesión (HU-02). Por ahora solo la interfaz: el envío se conecta a
 * `POST /api/v1/auth/login` en la fase 4.
 */
export function LoginPage() {
  return (
    <main className="login">
      <div className="login__card card">
        <Logo height={40} />
        <div>
          <h1 className="login__title">Ingresa a tu negocio</h1>
          <p className="muted">Controla tu inventario y tus ventas en un solo lugar.</p>
        </div>
        <form className="login__form" aria-describedby="login-estado" onSubmit={(event) => event.preventDefault()}>
          <div className="field">
            <label htmlFor="email">Correo</label>
            <input id="email" name="email" type="email" autoComplete="username" required />
          </div>
          <div className="field">
            <label htmlFor="password">Contraseña</label>
            <input id="password" name="password" type="password" autoComplete="current-password" required />
          </div>
          <button className="button" type="submit" disabled>
            Ingresar
          </button>
          <p id="login-estado" className="badge">
            El inicio de sesión se habilita en la fase 4
          </p>
        </form>
      </div>
    </main>
  )
}
