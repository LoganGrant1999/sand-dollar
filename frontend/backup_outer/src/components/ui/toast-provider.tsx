import toast, { Toaster } from 'react-hot-toast'

interface ToastProviderProps {
  position?: 'top-left' | 'top-center' | 'top-right' | 'bottom-left' | 'bottom-center' | 'bottom-right'
}

export function ToastProvider({ 
  position = "top-right"
}: ToastProviderProps) {
  return (
    <Toaster
      position={position}
      toastOptions={{
        duration: 4000,
        style: {
          background: 'var(--color-panel-dark)',
          color: 'var(--color-text-primary)',
          border: '1px solid hsl(var(--border))',
          borderRadius: '1rem',
          padding: '16px',
          boxShadow: '0px 2px 10px rgba(0,0,0,0.4)',
          fontSize: '14px',
          fontWeight: '500',
        },
        success: {
          iconTheme: {
            primary: 'var(--color-success)',
            secondary: 'var(--color-bg-dark)',
          },
        },
        error: {
          iconTheme: {
            primary: 'var(--color-error)',
            secondary: 'var(--color-bg-dark)',
          },
        },
        loading: {
          iconTheme: {
            primary: 'var(--color-accent-blue)',
            secondary: 'var(--color-bg-dark)',
          },
        },
      }}
    />
  )
}

// Enhanced toast functions with consistent styling
export const showToast = {
  success: (message: string) => 
    toast.success(message, {
      className: 'border border-[var(--color-success)] bg-[var(--color-panel-dark)] text-[var(--color-text-primary)]',
    }),
  
  error: (message: string) => 
    toast.error(message, {
      className: 'border border-[var(--color-error)] bg-[var(--color-panel-dark)] text-[var(--color-text-primary)]',
    }),
  
  loading: (message: string) => 
    toast.loading(message, {
      className: 'border border-[var(--color-accent-blue)] bg-[var(--color-panel-dark)] text-[var(--color-text-primary)]',
    }),
  
  info: (message: string) => 
    toast(message, {
      icon: 'ℹ️',
      className: 'border border-[var(--color-accent-blue)] bg-[var(--color-panel-dark)] text-[var(--color-text-primary)]',
    }),
  
  warning: (message: string) => 
    toast(message, {
      icon: '⚠️',
      className: 'border border-[var(--color-warning)] bg-[var(--color-panel-dark)] text-[var(--color-text-primary)]',
    }),
}
