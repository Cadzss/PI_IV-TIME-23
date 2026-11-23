// Autor: Cadu Spadari
// Mesmas cores do index.css (:root) para classes Tailwind como bg-tinta e text-acento
/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        fundo: '#EEF2F6',
        tinta: '#102A43',
        acento: '#0F7B8A',
        linha: '#D9E2EC',
        risco: {
          alto: '#B42318',
          medio: '#B54708',
          baixo: '#027A48',
          alerta: '#FEF3F2',
        },
      },
      fontFamily: {
        sans: ['Manrope', 'Segoe UI', 'system-ui', 'sans-serif'],
      },
    },
  },
  plugins: [],
}