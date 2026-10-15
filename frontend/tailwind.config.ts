import type { Config } from 'tailwindcss'

const config: Config = {
  content: [
    './index.html',
    './src/**/*.{js,ts,jsx,tsx}',
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        // MediSense primary — teal-blue health palette
        primary: {
          50:  '#f0fdfa',
          100: '#ccfbf1',
          200: '#99f6e4',
          300: '#5eead4',
          400: '#2dd4bf',
          500: '#14b8a6',
          600: '#0d9488',  // Brand primary
          700: '#0f766e',
          800: '#115e59',
          900: '#134e4a',
          950: '#042f2e',
        },
        // Health status tokens — mapped semantically
        'health-safe': {
          DEFAULT: '#16a34a',  // green-600
          light:   '#dcfce7',  // green-100
          border:  '#86efac',  // green-300
        },
        'health-borderline': {
          DEFAULT: '#d97706',  // amber-600
          light:   '#fef3c7',  // amber-100
          border:  '#fcd34d',  // amber-300
        },
        'health-abnormal': {
          DEFAULT: '#dc2626',  // red-600
          light:   '#fee2e2',  // red-100
          border:  '#fca5a5',  // red-300
        },
        // Surface tokens for glassmorphism dark mode
        surface: {
          900: '#0f172a',   // slate-900
          800: '#1e293b',   // slate-800
          700: '#334155',   // slate-700
          600: '#475569',   // slate-600
          glass: 'rgba(30, 41, 59, 0.8)',
        },
        // Accent tones
        accent: {
          cyan:    '#06b6d4',  // cyan-500
          indigo:  '#6366f1',  // indigo-500
          violet:  '#8b5cf6',  // violet-500
        },
      },

      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'sans-serif'],
        tamil: ['Noto Sans Tamil', 'system-ui', 'sans-serif'],
        mono: ['JetBrains Mono', 'monospace'],
      },

      borderRadius: {
        xl:  '0.75rem',
        '2xl': '1rem',
        '3xl': '1.5rem',
      },

      boxShadow: {
        glow:       '0 0 20px rgba(13, 148, 136, 0.3)',
        'glow-lg':  '0 0 40px rgba(13, 148, 136, 0.4)',
        glass:      '0 8px 32px rgba(0, 0, 0, 0.3)',
        card:       '0 4px 24px rgba(0, 0, 0, 0.15)',
      },

      backgroundImage: {
        'gradient-mesh':    'radial-gradient(ellipse at 30% 20%, rgba(13, 148, 136, 0.15) 0%, transparent 50%), radial-gradient(ellipse at 70% 80%, rgba(99, 102, 241, 0.1) 0%, transparent 50%)',
        'gradient-primary': 'linear-gradient(135deg, #0d9488, #0f766e)',
        'gradient-card':    'linear-gradient(145deg, rgba(30, 41, 59, 0.9), rgba(15, 23, 42, 0.9))',
      },

      animation: {
        'fade-in':   'fadeIn 0.3s ease-out',
        'slide-up':  'slideUp 0.4s ease-out',
        'pulse-slow': 'pulse 3s cubic-bezier(0.4, 0, 0.6, 1) infinite',
        'spin-slow':  'spin 3s linear infinite',
      },

      keyframes: {
        fadeIn: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' },
        },
        slideUp: {
          '0%':   { opacity: '0', transform: 'translateY(16px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
      },

      backdropBlur: {
        xs: '2px',
      },
    },
  },
  plugins: [],
}

export default config
