import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { beforeEach, describe, expect, it } from 'vitest'
import { useAuthStore } from '@fleetmgm/store'
import { resetClientsMock, SEED_CLIENTS } from '@/mocks/handlers'
import { Clients } from './Clients'

function renderClients() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <Clients />
    </QueryClientProvider>,
  )
}

const FIRST_CLIENT = SEED_CLIENTS[0]!

function loginAs(role: 'ADMIN' | 'DRIVER') {
  useAuthStore.getState().login({
    email: 'user@fleetmgm.com',
    role,
    accessToken: 'token',
    refreshToken: 'refresh',
  })
}

describe('Clients', () => {
  beforeEach(() => {
    resetClientsMock()
    useAuthStore.getState().logout()
  })

  it('renders the paginated client list', async () => {
    loginAs('ADMIN')
    renderClients()

    for (const client of SEED_CLIENTS) {
      expect(await screen.findByText(client.name)).toBeInTheDocument()
    }
  })

  it('creating a client calls POST and adds it to the list', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderClients()

    await screen.findByText(FIRST_CLIENT.name)

    await user.click(screen.getByRole('button', { name: /nuevo cliente/i }))
    await user.type(screen.getByLabelText(/^nombre$/i), 'Nordic Freight')
    await user.type(screen.getByLabelText(/id fiscal/i), 'B99999999')
    await user.click(screen.getByRole('button', { name: /crear cliente/i }))

    expect(await screen.findByText('Nordic Freight')).toBeInTheDocument()
  })

  it('editing a client calls PUT and updates the row', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderClients()

    await screen.findByText(FIRST_CLIENT.name)

    await user.click(screen.getAllByLabelText("Editar cliente")[0]!)
    const nameInput = screen.getByLabelText(/^nombre$/i)
    await user.clear(nameInput)
    await user.type(nameInput, 'Acme Logistics Renamed')
    await user.click(screen.getByRole('button', { name: /guardar cambios/i }))

    expect(await screen.findByText('Acme Logistics Renamed')).toBeInTheDocument()
    expect(screen.queryByText(FIRST_CLIENT.name)).not.toBeInTheDocument()
  })

  it('deleting a client asks for confirmation before calling DELETE', async () => {
    loginAs('ADMIN')
    const user = userEvent.setup()
    renderClients()

    await screen.findByText(FIRST_CLIENT.name)

    await user.click(screen.getAllByLabelText("Eliminar cliente")[0]!)
    expect(
      screen.getByText(new RegExp(`¿Eliminar a ${FIRST_CLIENT.name}\\?`)),
    ).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Eliminar' }))

    await waitFor(() =>
      expect(screen.queryByText(FIRST_CLIENT.name)).not.toBeInTheDocument(),
    )
  })

  it('hides management actions for the DRIVER role', async () => {
    loginAs('DRIVER')
    renderClients()

    await screen.findByText(FIRST_CLIENT.name)

    expect(screen.queryByRole('button', { name: /nuevo cliente/i })).not.toBeInTheDocument()
    expect(screen.queryByLabelText('Editar cliente')).not.toBeInTheDocument()
    expect(screen.queryByLabelText('Eliminar cliente')).not.toBeInTheDocument()
  })
})
