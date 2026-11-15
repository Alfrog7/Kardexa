import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { PasswordInput } from './PasswordInput'

describe('PasswordInput', () => {
  it('oculta la contraseña y el ojo la muestra y la vuelve a ocultar', () => {
    render(<PasswordInput aria-label="Contraseña" defaultValue="secreta" />)
    const input = screen.getByLabelText('Contraseña')
    expect(input).toHaveAttribute('type', 'password')

    fireEvent.click(screen.getByRole('button', { name: 'Mostrar contraseña' }))
    expect(input).toHaveAttribute('type', 'text')

    fireEvent.click(screen.getByRole('button', { name: 'Ocultar contraseña' }))
    expect(input).toHaveAttribute('type', 'password')
  })
})
