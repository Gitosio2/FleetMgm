import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'

export function Dashboard() {
  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="font-display text-3xl font-semibold">Fleet Overview</h1>
        <p className="text-on-surface-variant">
          Real-time performance and operational status of your logistics network.
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Coming soon</CardTitle>
          <CardDescription>
            Fleet metrics land progressively as each feature milestone ships.
          </CardDescription>
        </CardHeader>
        <CardContent />
      </Card>
    </div>
  )
}
