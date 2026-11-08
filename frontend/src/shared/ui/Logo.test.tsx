import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { Logo } from './Logo'

describe('Logo', () => {
  it('usa la versión clara por defecto y conserva la proporción', () => {
    render(<Logo height={40} />)

    const image = screen.getByRole('img', { name: 'Kardexa' })
    expect(image).toHaveAttribute('src', '/logo.svg')
    expect(image).toHaveAttribute('height', '40')
    expect(image).toHaveAttribute('width', '172')
  })

  it('ofrece la versión oscura para el tema oscuro del sistema', () => {
    const { container } = render(<Logo />)

    const source = container.querySelector('source')
    expect(source).toHaveAttribute('media', '(prefers-color-scheme: dark)')
    expect(source).toHaveAttribute('srcset', '/logo-oscuro.svg')
  })
})
