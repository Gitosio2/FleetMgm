import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useLogin } from '@fleetmgm/hooks'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Checkbox } from '@/components/ui/checkbox'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { ThemeToggle } from '@/components/layout/ThemeToggle'
import { DEMO_LOGIN_ACCOUNTS, DEMO_PASSWORD } from '@/lib/demo-login'

export function Login() {
  const navigate = useNavigate()
  const login = useLogin()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  function completeLogin(credentials: { email: string; password: string }) {
    login.mutate(credentials, {
      onSuccess: () => navigate('/dashboard', { replace: true }),
    })
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    completeLogin({ email, password })
  }

  function handleDemoLogin(demoEmail: string) {
    completeLogin({ email: demoEmail, password: DEMO_PASSWORD })
  }

  return (
    <div className="relative flex min-h-screen items-center justify-center bg-surface-dim px-4">
      <ThemeToggle className="absolute right-4 top-4" />
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>Inicio de sesión seguro</CardTitle>
          <CardDescription>Ingresa tus credenciales institucionales.</CardDescription>
        </CardHeader>
        <CardContent>
          <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="email">Correo electrónico</Label>
              <Input
                id="email"
                type="email"
                autoComplete="email"
                placeholder="operador@empresa.com"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                required
              />
            </div>

            <div className="flex flex-col gap-1.5">
              <Label htmlFor="password">Contraseña</Label>
              <Input
                id="password"
                type="password"
                autoComplete="current-password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                required
              />
            </div>

            <div className="flex items-center gap-2">
              <Checkbox id="remember-device" />
              <Label htmlFor="remember-device" className="normal-case font-normal">
                Recordar este dispositivo durante 30 días
              </Label>
            </div>

            {login.isError && (
              <p role="alert" className="text-sm text-error">
                Credenciales inválidas
              </p>
            )}

            <Button type="submit" size="lg" disabled={login.isPending}>
              Iniciar sesión
            </Button>
          </form>

          {/* Intentional demo shortcut: embeds seeded credentials in the client for live demos. */}
          <div className="mt-6 flex flex-col gap-3 border-t border-outline-variant/40 pt-5">
            <p className="text-xs text-on-surface-variant">Acceso rápido para demo</p>
            <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
              {DEMO_LOGIN_ACCOUNTS.map((account) => (
                <Button
                  key={account.role}
                  type="button"
                  variant="outline"
                  size="sm"
                  disabled={login.isPending}
                  onClick={() => handleDemoLogin(account.email)}
                >
                  {account.label}
                </Button>
              ))}
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
