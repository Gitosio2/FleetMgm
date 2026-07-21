import type { AppRole } from '@fleetmgm/store'

/** Shared demo password from V20 seed (`Demo1234!`). Exposed on the login UI for demos only. */
export const DEMO_PASSWORD = 'Demo1234!'

export type DemoLoginAccount = {
  role: AppRole
  email: string
  label: string
}

/** One account per AppRole — first seeded user of each role from README / V20. */
export const DEMO_LOGIN_ACCOUNTS: DemoLoginAccount[] = [
  { role: 'ADMIN', email: 'admin@fleetmgm.demo', label: 'Administrador' },
  { role: 'MANAGER', email: 'gerente@fleetmgm.demo', label: 'Gerente' },
  { role: 'ADMINISTRATIVE', email: 'administrativo1@fleetmgm.demo', label: 'Administrativo' },
  { role: 'WORKSHOP_STAFF', email: 'taller1@fleetmgm.demo', label: 'Taller' },
  { role: 'DRIVER', email: 'conductor1@fleetmgm.demo', label: 'Conductor' },
]

export function findDemoAccount(email: string, password: string): DemoLoginAccount | undefined {
  if (password !== DEMO_PASSWORD) {
    return undefined
  }
  return DEMO_LOGIN_ACCOUNTS.find((account) => account.email === email)
}
