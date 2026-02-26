/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    '../src/main/resources/templates/**/*.html',
    '../src/main/resources/static/js/admin/**/*.js',
  ],
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: '#171717',
          hover: '#262626',
          light: '#404040',
        },
        secondary: {
          DEFAULT: '#525252',
          hover: '#404040',
        },
        cta: {
          DEFAULT: '#22C55E',
          hover: '#16A34A',
        },
        success: '#22C55E',
        danger: '#EF4444',
        warning: '#F59E0B',
        info: '#3B82F6',
        surface: '#FFFFFF',
        'bg-color': '#FAFAFA',
        'border-color': '#E5E5E5',
        'text-primary': '#171717',
        'text-secondary': '#525252',
        'text-muted': '#A3A3A3',
      },
      fontFamily: {
        sans: ['Inter', '-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'Roboto', 'sans-serif'],
        mono: ['SF Mono', 'Monaco', 'Cascadia Code', 'monospace'],
      },
      boxShadow: {
        'sm': '0 1px 2px rgba(0,0,0,0.05)',
        'md': '0 4px 6px rgba(0,0,0,0.1)',
        'lg': '0 10px 15px rgba(0,0,0,0.1)',
        'xl': '0 20px 25px rgba(0,0,0,0.15)',
      },
      spacing: {
        'xs': '0.25rem',
        'sm': '0.5rem',
        'md': '1rem',
        'lg': '1.5rem',
        'xl': '2rem',
        '2xl': '3rem',
        '3xl': '4rem',
      },
      borderRadius: {
        'DEFAULT': '8px',
        'lg': '12px',
        'xl': '16px',
      },
    },
  },
  plugins: [],
}
