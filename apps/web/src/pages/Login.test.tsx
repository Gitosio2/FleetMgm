import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it } from 'vitest'
import { useAuthStore, useThemeStore } from '@fleetmgm/store'
import { Login } from './Login'
import { VALID_CREDENTIALS } from '@/mocks/handlers'
import { DEMO_LOGIN_ACCOUNTS } from '@/lib/demo-login'

function renderLogin() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/login']}>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/dashboard" element={<div>Dashboard Home</div>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('Login', () => {
  beforeEach(() => {
    useAuthStore.getState().logout()
    useThemeStore.getState().setTheme('light')
  })

  it('renders the sign-in form', () => {
    renderLogin()

    expect(screen.getByLabelText(/correo electrónico/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/contraseña/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /iniciar sesión/i })).toBeInTheDocument()
  })

  it('renders one demo shortcut button per AppRole', () => {
    renderLogin()

    for (const account of DEMO_LOGIN_ACCOUNTS) {
      expect(screen.getByRole('button', { name: account.label })).toBeInTheDocument()
    }
  })

  it('redirects to the dashboard on successful login', async () => {
    const user = userEvent.setup()
    renderLogin()

    await user.type(screen.getByLabelText(/correo electrónico/i), VALID_CREDENTIALS.email)
    await user.type(screen.getByLabelText(/contraseña/i), VALID_CREDENTIALS.password)
    await user.click(screen.getByRole('button', { name: /iniciar sesión/i }))

    await waitFor(() => expect(screen.getByText('Dashboard Home')).toBeInTheDocument())
  })

  it('logs in as the selected demo role with one click', async () => {
    const user = userEvent.setup()
    renderLogin()

    await user.click(screen.getByRole('button', { name: 'Conductor' }))

    await waitFor(() => expect(screen.getByText('Dashboard Home')).toBeInTheDocument())
    expect(useAuthStore.getState().role).toBe('DRIVER')
    expect(useAuthStore.getState().email).toBe('conductor1@fleetmgm.demo')
  })

  it('shows a generic error message on invalid credentials', async () => {
    const user = userEvent.setup()
    renderLogin()

    await user.type(screen.getByLabelText(/correo electrónico/i), VALID_CREDENTIALS.email)
    await user.type(screen.getByLabelText(/contraseña/i), 'wrong-password')
    await user.click(screen.getByRole('button', { name: /iniciar sesión/i }))

    expect(await screen.findByText(/credenciales inválidas/i)).toBeInTheDocument()
  })

  it('toggles the theme when the switch is clicked', async () => {
    const user = userEvent.setup()
    renderLogin()

    await user.click(screen.getByRole('switch', { name: /cambiar entre modo claro y oscuro/i }))

    expect(useThemeStore.getState().theme).toBe('dark')
  })
})
