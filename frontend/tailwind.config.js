/** @type {import('tailwindcss').Config} */
export default {
  darkMode: "class",
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        ink: "#17221d",
        cream: "#f8f7f2",
        sage: {
          50: "#f0f6f0",
          100: "#dcebdd",
          200: "#bfd7c2",
          500: "#4a8161",
          600: "#396a4d",
          700: "#2c523e",
        },
        coral: "#dc725a",
        gold: "#d29a45",
      },
      boxShadow: {
        soft: "0 18px 50px rgba(31, 50, 39, 0.08)",
      },
      fontFamily: {
        sans: ["Inter", "ui-sans-serif", "system-ui", "sans-serif"],
        display: ["Manrope", "Inter", "ui-sans-serif", "sans-serif"],
      },
    },
  },
  plugins: [],
};
