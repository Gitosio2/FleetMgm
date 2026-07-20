import { LoginForm } from '@/components/auth/LoginForm'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { ThemeToggle } from '@/components/layout/ThemeToggle'

export function Login() {
  return (
    <div className="relative flex min-h-screen items-center justify-center bg-surface-dim px-4">
      <ThemeToggle className="absolute right-4 top-4" />
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>Inicio de sesión seguro</CardTitle>
          <CardDescription>Ingresa tus credenciales institucionales.</CardDescription>
        </CardHeader>
        <CardContent>
          <LoginForm />
        </CardContent>
      </Card>
    </div>
  )
}
