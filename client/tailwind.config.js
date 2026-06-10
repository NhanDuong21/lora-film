/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        'brand-dark': '#050506',
        'brand-coral': '#ff7a1a',
        'coral-500': '#ff7a1a',
        'brand-gray': '#1A1A1D',
        'brand-yellow': '#EAB308',
      },
      fontFamily: {
        sans: ['Outfit', 'Inter', 'system-ui', 'sans-serif'],
      },
    },
  },
  plugins: [],
}
