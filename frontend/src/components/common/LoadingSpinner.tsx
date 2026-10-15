import { cn } from '@/utils/cn'

interface Props {
  size?: 'sm' | 'md' | 'lg'
  className?: string
}

export default function LoadingSpinner({ size = 'md', className }: Props) {
  const sizeClasses = { sm: 'h-4 w-4', md: 'h-8 w-8', lg: 'h-12 w-12' }
  return (
    <div
      className={cn(
        'animate-spin rounded-full border-2 border-surface-600 border-t-primary-500',
        sizeClasses[size],
        className
      )}
      role="status"
      aria-label="Loading"
    />
  )
}
