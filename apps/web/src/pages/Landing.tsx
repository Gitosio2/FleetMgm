import { Link } from 'react-router-dom'
import { Truck } from 'lucide-react'
import { Button } from '@/components/ui/button'
import dashboardPreview from '@/assets/landing-dashboard-preview.png'

export function Landing() {
  return (
    <div className="min-h-screen bg-surface-container-low text-on-surface">
      <header className="mx-auto flex max-w-[1360px] items-center justify-between px-6 py-5 sm:px-16">
        <div className="flex items-center gap-2.5">
          <span className="flex size-8 items-center justify-center rounded-lg bg-primary text-on-primary">
            <Truck className="size-4" />
          </span>
          <span className="font-display text-lg font-bold tracking-tight">FleetMgm</span>
        </div>
        <nav className="flex items-center gap-6 sm:gap-9">
          <a href="#historia" className="hidden text-sm font-medium text-on-surface-variant sm:inline">
            Sobre el proyecto
          </a>
          <Button asChild>
            <Link to="/login">Acceder al login</Link>
          </Button>
        </nav>
      </header>

      <section className="mx-auto grid max-w-[1360px] items-center gap-10 px-6 py-10 sm:gap-14 sm:px-16 sm:py-16 lg:grid-cols-[1.1fr_0.9fr]">
        <div>
          <div className="mb-6 inline-flex items-center gap-2 rounded-full bg-secondary-container/10 px-3.5 py-1.5 font-body text-xs font-semibold text-secondary">
            GESTIÓN DE FLOTAS Y FACTURACIÓN
          </div>
          <h1 className="mb-5 font-display text-4xl font-bold tracking-tight sm:text-5xl">
            Controla tu flota y factura sin fricción, todo en un solo lugar.
          </h1>
          <p className="mb-8 max-w-lg text-lg leading-relaxed text-on-surface-variant">
            Diseñado para flotas de PYMES que necesitan visibilidad, orden y trazabilidad en la gestión.
          </p>
          <div className="flex flex-wrap gap-3.5">
            <Button size="lg" asChild>
              <Link to="/login">Acceder al login</Link>
            </Button>
            <Button size="lg" variant="outline" asChild>
              <a href="#historia">Conoce el proyecto</a>
            </Button>
          </div>
        </div>
        <div className="overflow-hidden rounded-2xl border border-outline-variant/40 bg-surface-container-lowest shadow-lg">
          <img src={dashboardPreview} alt="Panel de control FleetMgm" className="block w-full" />
        </div>
      </section>

      <section id="historia" className="mx-auto max-w-[840px] px-6 py-20 sm:px-16 sm:py-28">
        <div className="mb-4 text-center font-body text-sm font-semibold text-secondary">
          POR QUÉ EXISTE FLEETMGM
        </div>
        <h2 className="mb-8 text-center font-display text-3xl font-bold tracking-tight">
          De un problema real a una plataforma
        </h2>
        <div className="space-y-5 text-[17px] leading-relaxed text-on-surface-variant">
          <p>
            Antes de convertirse en un TFM, este proyecto surge como respuesta a un problema real que viví de
            cerca. Trabajé como administrativo en una PYME del sector de transporte y logística, y desde dentro
            pude ver cómo se gestionaba el día a día de la flota: facturación en formularios de Access guardados
            en un NAS, GPS en múltiples apps, trabajos anotados en papel, mantenimientos en hojas de Excel, al
            igual que las mercancías que llevan los camiones. Ninguna se integraba ni intercambiaba datos
            automáticamente con las demás. Todo ello anotado a mano, disperso, y solo accesible desde el
            ordenador de la oficina.
          </p>
          <p>
            FleetMgm nace como la respuesta que a mí me habría gustado tener entonces: una única plataforma que
            centraliza vehículos, personal, trabajos, mantenimiento, facturación y GPS, pensada para pymes que no
            necesitan (ni pueden permitirse) un ERP corporativo, y accesible desde cualquier lugar — no solo
            desde la oficina. <em>(App mobile en proceso)</em>
          </p>
        </div>
      </section>

      <section className="bg-primary px-6 py-16 text-center text-on-primary sm:px-16">
        <h2 className="mb-3 font-display text-3xl font-bold tracking-tight">Empieza a gestionar tu flota hoy</h2>
        <p className="mb-8 text-on-primary/70">Ingresa a tu panel y toma el control de tu operación.</p>
        <Button size="lg" className="bg-surface-container-lowest text-on-surface hover:opacity-90" asChild>
          <Link to="/login">Acceder al login</Link>
        </Button>
      </section>

      <footer className="mx-auto flex max-w-[1360px] items-center justify-between px-6 py-7 text-sm text-on-surface-variant sm:px-16">
        <span>© 2026 FleetMgm</span>
        <span className="font-body">Gestión de flotas y facturación</span>
      </footer>
    </div>
  )
}
