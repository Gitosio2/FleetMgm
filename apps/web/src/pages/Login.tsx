import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useLogin } from '@fleetmgm/hooks'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Checkbox } from '@/components/ui/checkbox'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'

export function Login() {
  const navigate = useNavigate()
  const login = useLogin()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    login.mutate(
      { email, password },
      { onSuccess: () => navigate('/', { replace: true }) },
    )
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-surface-dim px-4">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>Secure Sign In</CardTitle>
          <CardDescription>Please enter your institutional credentials.</CardDescription>
        </CardHeader>
        <CardContent>
          <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="email">Email address</Label>
              <Input
                id="email"
                type="email"
                autoComplete="email"
                placeholder="operator@enterprise.com"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                required
              />
            </div>

            <div className="flex flex-col gap-1.5">
              <Label htmlFor="password">Password</Label>
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
                Remember this device for 30 days
              </Label>
            </div>

            {login.isError && (
              <p role="alert" className="text-sm text-error">
                Invalid credentials
              </p>
            )}

            <Button type="submit" size="lg" disabled={login.isPending}>
              Login
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  )
}
