import type { ReactNode } from "react";
import { Link } from "react-router-dom";

export function PublicFunnelLayout({ children }: { children: ReactNode }) {
  return <main className="min-h-screen bg-paper text-ink dark:bg-slate-950 dark:text-white">
    <header className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-4 px-5 py-5 sm:px-8">
      <Link to="/" className="flex items-center gap-3 font-display text-lg font-extrabold" aria-label="PsicoGest início">
        <span className="grid h-10 w-10 place-items-center rounded-2xl bg-harbor-600 text-white">P</span>PsicoGest
      </Link>
      <nav className="flex flex-wrap items-center gap-4 text-sm font-semibold text-slate-600 dark:text-slate-300" aria-label="Navegação pública">
        <Link to="/demo" className="hover:text-harbor-700 dark:hover:text-harbor-300">Demonstração</Link>
        <Link to="/planos" className="hover:text-harbor-700 dark:hover:text-harbor-300">Planos</Link>
        <Link to="/faq" className="hover:text-harbor-700 dark:hover:text-harbor-300">Dúvidas</Link>
        <Link to="/login" className="hover:text-harbor-700 dark:hover:text-harbor-300">Entrar</Link>
      </nav>
    </header>
    {children}
    <footer className="mx-auto mt-16 flex max-w-6xl flex-wrap justify-between gap-3 border-t border-slate-200 px-5 py-6 text-sm text-slate-600 dark:border-slate-800 dark:text-slate-300 sm:px-8">
      <span>PsicoGest · demonstração com dados fictícios</span>
      <Link to="/faq" className="font-semibold text-harbor-700 dark:text-harbor-300">Dúvidas frequentes</Link>
    </footer>
  </main>;
}
