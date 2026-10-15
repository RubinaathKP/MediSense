import { ArrowUpRight, ArrowDownRight, Minus } from 'lucide-react'
import { cn } from '@/utils/cn'

export interface MetricCardProps {
  name: string
  currentValue: number // percentage (e.g. 96.5 for 96.5%)
  targetValue: number  // percentage threshold (e.g. 95)
  isUpperBound?: boolean // true for hallucination_rate (lower is better, e.g. <= 2%)
  trend?: 'up' | 'down' | 'stable'
  unit?: string
}

export default function MetricCard({
  name,
  currentValue,
  targetValue,
  isUpperBound = false,
  trend = 'stable',
  unit = '%',
}: MetricCardProps) {
  // Check if target is met
  const isMeetingTarget = isUpperBound
    ? currentValue <= targetValue
    : currentValue >= targetValue

  const TrendIcon =
    trend === 'up'
      ? ArrowUpRight
      : trend === 'down'
      ? ArrowDownRight
      : Minus

  const trendColor =
    trend === 'stable'
      ? 'text-surface-400'
      : (trend === 'up' && !isUpperBound) || (trend === 'down' && isUpperBound)
      ? 'text-emerald-400'
      : 'text-rose-400'

  return (
    <div
      data-testid={`metric-card-${name.toLowerCase().replace(/[^a-z0-9]/g, '-')}`}
      className="flex flex-col justify-between rounded-2xl border border-surface-700/60 bg-surface-800/40 p-4 shadow-sm transition-all hover:border-surface-600 hover:bg-surface-800/60"
    >
      <div className="flex items-start justify-between gap-2">
        <span className="text-xs font-semibold text-surface-400 uppercase tracking-wider leading-snug">
          {name}
        </span>

        <div className={cn('flex items-center gap-0.5 text-xs font-mono font-bold', trendColor)}>
          <TrendIcon className="h-3.5 w-3.5" />
          <span className="capitalize">{trend}</span>
        </div>
      </div>

      <div className="my-3 flex items-baseline gap-1">
        <span
          data-testid="metric-current-value"
          className={cn(
            'text-2xl md:text-3xl font-mono font-extrabold tracking-tight',
            isMeetingTarget ? 'text-emerald-400' : 'text-rose-500'
          )}
        >
          {currentValue.toFixed(1)}
        </span>
        <span
          className={cn(
            'text-sm font-mono font-semibold',
            isMeetingTarget ? 'text-emerald-400/80' : 'text-rose-500/80'
          )}
        >
          {unit}
        </span>
      </div>

      <div className="flex items-center justify-between border-t border-surface-700/40 pt-2 text-[11px] text-surface-400">
        <span>Target:</span>
        <span className="font-mono font-semibold text-slate-200">
          {isUpperBound ? `≤ ${targetValue}${unit}` : `≥ ${targetValue}${unit}`}
        </span>
      </div>
    </div>
  )
}
