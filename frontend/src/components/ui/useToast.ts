/**
 * Toast notification state and helper methods.
 *
 * Requirements:
 *  - Error toasts: red, auto-dismiss 5s
 *  - Warning toasts: amber, auto-dismiss 8s
 *  - Success toasts: green, auto-dismiss 3s
 *  - Rate limit toasts: amber, NOT auto-dismissed (user needs to read it)
 */
import { useState, useEffect } from 'react'

export type ToastVariant = 'default' | 'success' | 'warning' | 'error' | 'rateLimit'

export interface ToastItem {
  id: string
  title?: string
  description?: string
  variant: ToastVariant
  duration?: number // duration in ms, or 0 / Infinity for persistent
}

type ToastListener = (toasts: ToastItem[]) => void

let memoryToasts: ToastItem[] = []
const listeners: ToastListener[] = []

function notify() {
  listeners.forEach(listener => listener([...memoryToasts]))
}

export const toast = {
  create: (options: { title?: string; description?: string; variant?: ToastVariant; duration?: number }) => {
    const id = `toast-${Date.now()}-${Math.random().toString(36).substring(2, 7)}`
    const variant = options.variant || 'default'

    let defaultDuration = 5000
    if (variant === 'success') defaultDuration = 3000
    if (variant === 'warning') defaultDuration = 8000
    if (variant === 'error') defaultDuration = 5000
    if (variant === 'rateLimit') defaultDuration = 0 // NOT auto-dismissed

    const duration = options.duration !== undefined ? options.duration : defaultDuration

    const item: ToastItem = {
      id,
      title: options.title,
      description: options.description,
      variant,
      duration,
    }

    memoryToasts = [item, ...memoryToasts].slice(0, 5) // max 5 active
    notify()

    if (duration > 0) {
      setTimeout(() => {
        toast.dismiss(id)
      }, duration)
    }

    return id
  },

  success: (props: { title?: string; description?: string; duration?: number }) => {
    return toast.create({ ...props, variant: 'success' })
  },

  warning: (props: { title?: string; description?: string; duration?: number }) => {
    return toast.create({ ...props, variant: 'warning' })
  },

  error: (props: { title?: string; description?: string; duration?: number }) => {
    return toast.create({ ...props, variant: 'error' })
  },

  rateLimit: (props: { title?: string; description?: string; duration?: number }) => {
    return toast.create({ ...props, variant: 'rateLimit', duration: props.duration ?? 0 })
  },

  dismiss: (id: string) => {
    memoryToasts = memoryToasts.filter(t => t.id !== id)
    notify()
  },

  clearAll: () => {
    memoryToasts = []
    notify()
  },
}

export function useToast() {
  const [toasts, setToasts] = useState<ToastItem[]>(memoryToasts)

  useEffect(() => {
    listeners.push(setToasts)
    return () => {
      const idx = listeners.indexOf(setToasts)
      if (idx !== -1) listeners.splice(idx, 1)
    }
  }, [])

  return {
    toasts,
    toast,
    dismiss: toast.dismiss,
  }
}

export default useToast
