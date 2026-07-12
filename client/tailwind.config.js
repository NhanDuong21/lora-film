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
        'brand-coral': '#D88174',
        'coral-500': '#D88174',
        'brand-yellow': '#EAB308',
      },
      fontFamily: {
        sans: ['Outfit', 'Inter', 'system-ui', 'sans-serif'],
      },
    },
  },
  plugins: [],
}
