import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

// jsdom no implementa los métodos modales de <dialog>.
HTMLDialogElement.prototype.showModal ??= function (this: HTMLDialogElement) {
  this.open = true
}
HTMLDialogElement.prototype.close ??= function (this: HTMLDialogElement) {
  this.open = false
}

afterEach(() => {
  cleanup()
  sessionStorage.clear()
  localStorage.clear()
})
