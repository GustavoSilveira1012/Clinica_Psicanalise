/** @type {import('tailwindcss').Config} */
export default {
  darkMode: "class",
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        // Identidade "Tinta & Âmbar" — tokens semânticos (fonte da verdade).
        paper: "#f4f4f2", // fundo do app
        surface: "#ffffff", // cards/superfícies elevadas
        ink: {
          DEFAULT: "#161d2b", // texto principal / primário escuro
          soft: "#3a4656", // texto secundário
          muted: "#6b7683", // texto terciário / placeholder
        },
        harbor: {
          // marca (índigo): registros, precisão, confiança
          50: "#eef2f7", 100: "#dbe3ee", 200: "#bccadd", 300: "#93a8c6",
          400: "#6883ab", 500: "#4a6790", 600: "#33507b", 700: "#2a4166",
          800: "#243654", 900: "#1e2c44", 950: "#18243a",
        },
        amber: {
          // acento (parcimônia): acolhimento
          50: "#fbf3e6", 100: "#f5e2c4", 200: "#eac98f", 300: "#dfb166",
          400: "#d29e4c", 500: "#c98a3c", 600: "#ac7230", 700: "#8a5a28",
          800: "#6e4822", 900: "#5a3b1e", 950: "#43301a",
        },
        success: { 50: "#e9f4ee", 500: "#2f7d5b", 600: "#276a4d", 700: "#1f5540" },
        warning: { 50: "#fdf3e2", 500: "#b7791f", 600: "#9a6419", 700: "#7c5014" },
        danger: { 50: "#fbeceb", 500: "#b4453a", 600: "#993a31", 700: "#7c2f28" },
        info: { 50: "#eef2f7", 500: "#4a6790", 600: "#33507b", 700: "#2a4166" },
      },
      boxShadow: {
        // Escala de elevação (substitui a sombra única) — hierarquia por profundidade.
        soft: "0 1px 2px rgba(22,29,43,0.04), 0 1px 3px rgba(22,29,43,0.06)",
        card: "0 1px 2px rgba(22,29,43,0.04), 0 1px 3px rgba(22,29,43,0.06)",
        raised: "0 6px 20px rgba(22,29,43,0.10)",
        overlay: "0 24px 60px rgba(22,29,43,0.20)",
      },
      fontFamily: {
        sans: ["IBM Plex Sans", "ui-sans-serif", "system-ui", "sans-serif"],
        display: ["Newsreader", "ui-serif", "Georgia", "Cambria", "serif"],
      },
    },
  },
  plugins: [],
};
