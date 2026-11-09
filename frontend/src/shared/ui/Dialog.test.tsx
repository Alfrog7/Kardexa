import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { Dialog } from './Dialog'

describe('Dialog', () => {
  it('se abre con su título y lleva el foco al campo marcado', () => {
    render(
      <Dialog title="Nueva categoría" onClose={vi.fn()}>
        <input aria-label="Nombre" data-autofocus />
      </Dialog>,
    )

    expect(screen.getByRole('dialog', { name: 'Nueva categoría' })).toBeInTheDocument()
    expect(screen.getByLabelText('Nombre')).toHaveFocus()
  })

  it('avisa al cerrar con la X o con Esc', () => {
    const onClose = vi.fn()
    render(
      <Dialog title="Editar" variant="drawer" onClose={onClose}>
        contenido
      </Dialog>,
    )

    fireEvent.click(screen.getByRole('button', { name: 'Cerrar' }))
    fireEvent(screen.getByRole('dialog'), new Event('cancel', { cancelable: true }))

    expect(onClose).toHaveBeenCalledTimes(2)
    expect(screen.getByRole('dialog')).toHaveClass('dialog--drawer')
  })
})
