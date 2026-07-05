import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it } from 'vitest'
import { useAuthStore } from '@fleetmgm/store'
import { ProtectedRoute } from './ProtectedRoute'

function renderProtected(initialPath: string, allowedRoles?: string[]) {
  return render(
    <MemoryRouter initialEntries={[initialPath]}>
      <Routes>
        <Route path="/login" element={<div>Login Page</div>} />
        <Route
          path="/"
          element={
            <ProtectedRoute allowedRoles={allowedRoles}>
              <div>Protected Content</div>
            </ProtectedRoute>
          }
        />
      </Routes>
    </MemoryRouter>,
  )
}

describe('ProtectedRoute', () => {
  beforeEach(() => {
    useAuthStore.getState().logout()
  })

  it('redirects to Login when there is no session', () => {
    renderProtected('/')

    expect(screen.getByText('Login Page')).toBeInTheDocument()
    expect(screen.queryByText('Protected Content')).not.toBeInTheDocument()
  })

  it('renders the protected content when the session role is allowed', () => {
    useAuthStore.getState().login({
      email: 'admin@fleetmgm.com',
      role: 'ADMIN',
      accessToken: 'token',
      refreshToken: 'refresh',
    })

    renderProtected('/', ['ADMIN', 'MANAGER'])

    expect(screen.getByText('Protected Content')).toBeInTheDocument()
  })

  it('shows a 403 message when the session role is not allowed', () => {
    useAuthStore.getState().login({
      email: 'driver@fleetmgm.com',
      role: 'DRIVER',
      accessToken: 'token',
      refreshToken: 'refresh',
    })

    renderProtected('/', ['ADMIN', 'MANAGER'])

    expect(screen.getByText(/403/)).toBeInTheDocument()
    expect(screen.queryByText('Protected Content')).not.toBeInTheDocument()
  })
})
