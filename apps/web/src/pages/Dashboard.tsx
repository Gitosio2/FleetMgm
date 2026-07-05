import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'

export function Dashboard() {
  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="font-display text-3xl font-semibold">Resumen de la flota</h1>
        <p className="text-on-surface-variant">
          Rendimiento en tiempo real y estado operativo de tu red logística.
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Próximamente</CardTitle>
          <CardDescription>
            Las métricas de la flota se irán habilitando a medida que se completen los próximos hitos.
          </CardDescription>
        </CardHeader>
        <CardContent />
      </Card>
    </div>
  )
}
