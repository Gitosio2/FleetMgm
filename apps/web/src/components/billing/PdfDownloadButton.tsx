import { Download } from 'lucide-react'
import { useDownloadInvoicePdf } from '@fleetmgm/hooks'
import { Button } from '@/components/ui/button'

type PdfDownloadButtonProps = {
  invoiceId: string
  invoiceNumber: string
}

export function PdfDownloadButton({ invoiceId, invoiceNumber }: PdfDownloadButtonProps) {
  const downloadPdf = useDownloadInvoicePdf()

  return (
    <div className="flex flex-col items-start gap-1">
      <Button
        variant="ghost"
        size="sm"
        aria-label="Descargar PDF"
        title="Descargar PDF"
        disabled={downloadPdf.isPending}
        onClick={() => downloadPdf.mutate({ id: invoiceId, invoiceNumber })}
      >
        <Download className="size-4" />
      </Button>
      {downloadPdf.isError && (
        <p role="alert" className="text-sm text-error">
          No se pudo descargar el PDF.
        </p>
      )}
    </div>
  )
}
