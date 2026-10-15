import { useTranslation } from 'react-i18next'
import { Info } from 'lucide-react'

export default function Disclaimer() {
  const { t } = useTranslation()

  return (
    <div
      id="persistent-disclaimer"
      data-testid="persistent-disclaimer"
      role="note"
      aria-label="Medical disclaimer"
      className="sticky top-0 z-40 w-full border-b border-sky-500/20 bg-sky-950/40 px-4 py-2 text-center text-xs font-medium text-sky-200 backdrop-blur-md shadow-sm"
    >
      <div className="mx-auto flex max-w-5xl items-center justify-center gap-2">
        <Info className="h-4 w-4 shrink-0 text-sky-400" />
        <p>
          {t(
            'disclaimer.text',
            'MediSense explains health information. It does not replace your doctor.'
          )}
        </p>
      </div>
    </div>
  )
}
