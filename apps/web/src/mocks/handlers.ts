import { http, HttpResponse } from 'msw'

export const VALID_CREDENTIALS = {
  email: 'admin@fleetmgm.com',
  password: 'admin123',
}

export const MOCK_ACCESS_TOKEN = 'mock-access-token'
export const MOCK_REFRESH_TOKEN = 'mock-refresh-token'
export const MOCK_ROLE = 'ADMIN'

type LoginRequestBody = {
  email: string
  password: string
}

type RefreshRequestBody = {
  refreshToken: string
}

const invalidCredentialsResponse = () =>
  HttpResponse.json(
    {
      status: 401,
      code: 'INVALID_CREDENTIALS',
      message: 'Invalid credentials',
      correlationId: 'test-correlation-id',
    },
    { status: 401 },
  )

type Client = {
  id: string
  name: string
  taxId: string
  email: string | null
  phone: string | null
  address: string | null
  createdAt: string
}

type ClientRequestBody = {
  name: string
  taxId: string
  email?: string | null
  phone?: string | null
  address?: string | null
}

export const SEED_CLIENTS: Client[] = [
  {
    id: 'client-1',
    name: 'Acme Logistics',
    taxId: 'B12345678',
    email: 'ops@acme.test',
    phone: '+34600000000',
    address: 'Calle Falsa 123',
    createdAt: '2026-01-10T09:00:00Z',
  },
  {
    id: 'client-2',
    name: 'Transportes Ibérica',
    taxId: 'B87654321',
    email: 'contacto@iberica.test',
    phone: '+34600000001',
    address: 'Av. Principal 45',
    createdAt: '2026-02-15T09:00:00Z',
  },
]

let clients: Client[] = [...SEED_CLIENTS]

export function resetClientsMock() {
  clients = [...SEED_CLIENTS]
}

export const handlers = [
  http.get('/api/v1/clients', ({ request }) => {
    const url = new URL(request.url)
    const page = Number(url.searchParams.get('page') ?? 0)
    const size = Number(url.searchParams.get('size') ?? 20)
    const start = page * size
    const content = clients.slice(start, start + size)

    return HttpResponse.json({
      content,
      page,
      size,
      totalElements: clients.length,
      totalPages: Math.max(1, Math.ceil(clients.length / size)),
    })
  }),

  http.post('/api/v1/clients', async ({ request }) => {
    const body = (await request.json()) as ClientRequestBody

    const newClient: Client = {
      id: `client-${clients.length + 1}`,
      name: body.name,
      taxId: body.taxId,
      email: body.email ?? null,
      phone: body.phone ?? null,
      address: body.address ?? null,
      createdAt: new Date().toISOString(),
    }
    clients = [...clients, newClient]

    return HttpResponse.json(newClient, { status: 201 })
  }),

  http.put('/api/v1/clients/:id', async ({ request, params }) => {
    const body = (await request.json()) as ClientRequestBody
    const index = clients.findIndex((client) => client.id === params.id)
    const existing = clients[index]

    if (!existing) {
      return HttpResponse.json(
        {
          status: 404,
          code: 'CLIENT_NOT_FOUND',
          message: `Client ${params.id} not found`,
          correlationId: 'test-correlation-id',
        },
        { status: 404 },
      )
    }

    const updated: Client = {
      ...existing,
      name: body.name,
      taxId: body.taxId,
      email: body.email ?? null,
      phone: body.phone ?? null,
      address: body.address ?? null,
    }
    clients = clients.map((client, i) => (i === index ? updated : client))

    return HttpResponse.json(updated)
  }),

  http.delete('/api/v1/clients/:id', ({ params }) => {
    clients = clients.filter((client) => client.id !== params.id)
    return new HttpResponse(null, { status: 204 })
  }),

  http.post('/api/v1/auth/login', async ({ request }) => {
    const body = (await request.json()) as LoginRequestBody

    if (body.email !== VALID_CREDENTIALS.email || body.password !== VALID_CREDENTIALS.password) {
      return invalidCredentialsResponse()
    }

    return HttpResponse.json({
      accessToken: MOCK_ACCESS_TOKEN,
      refreshToken: MOCK_REFRESH_TOKEN,
      role: MOCK_ROLE,
    })
  }),

  http.post('/api/v1/auth/refresh', async ({ request }) => {
    const body = (await request.json()) as RefreshRequestBody

    if (body.refreshToken !== MOCK_REFRESH_TOKEN) {
      return invalidCredentialsResponse()
    }

    return HttpResponse.json({
      accessToken: `${MOCK_ACCESS_TOKEN}-refreshed`,
      refreshToken: MOCK_REFRESH_TOKEN,
      role: MOCK_ROLE,
    })
  }),
]
