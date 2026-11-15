import { useState, type InputHTMLAttributes } from 'react'
import { Icon } from './Icon'
import './PasswordInput.css'

/** Campo de contraseña con el botón del ojo para mostrarla u ocultarla. Recibe los mismos atributos que un `input`. */
export function PasswordInput(props: Omit<InputHTMLAttributes<HTMLInputElement>, 'type'>) {
  const [visible, setVisible] = useState(false)
  const label = visible ? 'Ocultar contraseña' : 'Mostrar contraseña'

  return (
    <span className="password-input">
      <input {...props} type={visible ? 'text' : 'password'} />
      <button
        className="password-input__toggle"
        type="button"
        aria-label={label}
        aria-pressed={visible}
        title={label}
        onClick={() => setVisible((current) => !current)}
      >
        <Icon name={visible ? 'eyeOff' : 'eye'} size={20} />
      </button>
    </span>
  )
}
