/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        'brand-dark': '#111111',
        'brand-gray': '#1A1A1D',
        'brand-orange': '#FF7A00',
        'brand-yellow': '#EAB308',
        zinc: {
          305: '#b5b5ba',
          450: '#8b8b93',
          550: '#64646e',
          650: '#46464e',
          850: '#202024',
        }
      },
      fontFamily: {
        sans: ['Outfit', 'Inter', 'system-ui', 'sans-serif'],
      },
    },
  },
  plugins: [],
}
