/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        marinho: '#1F3864',
        risco: { alto: '#C00000', medio: '#E36C09', baixo: '#538135' },
      },
    },
  },
  plugins: [],
}
