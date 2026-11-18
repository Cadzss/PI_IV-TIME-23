// Autor: Cadu Spadari
/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        papel: '#F4F1EB',
        marinho: '#1C3144',
        destaque: '#C46B4A',
        risco: {
          alto: '#C23B3B',
          medio: '#C9841A',
          baixo: '#2E8B6A',
          alerta: '#FDECEA',
        },
      },
    },
  },
  plugins: [],
}
