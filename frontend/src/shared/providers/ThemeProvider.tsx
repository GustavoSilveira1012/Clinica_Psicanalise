import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from "react";

type Theme = "light" | "dark";
const ThemeContext = createContext<{ theme: Theme; toggleTheme: () => void } | undefined>(undefined);

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setTheme] = useState<Theme>(() => (localStorage.getItem("psicogest-theme") as Theme | null) ?? "light");
  useEffect(() => { document.documentElement.classList.toggle("dark", theme === "dark"); localStorage.setItem("psicogest-theme", theme); }, [theme]);
  const value = useMemo(() => ({ theme, toggleTheme: () => setTheme((current) => current === "dark" ? "light" : "dark") }), [theme]);
  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

export function useTheme() { const context = useContext(ThemeContext); if (!context) throw new Error("useTheme precisa estar dentro de ThemeProvider"); return context; }
