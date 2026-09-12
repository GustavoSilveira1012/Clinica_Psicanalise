import { useMemo, useState, type ReactNode } from "react";
import { NavLink, useLocation, useNavigate } from "react-router-dom";
import type { LucideIcon } from "lucide-react";
import { Bell, CalendarDays, ChevronDown, CircleDollarSign, ClipboardList, FileText, LayoutDashboard, LogOut, Menu, Moon, Settings, ShieldCheck, Sparkles, Sun, Users, WalletCards, X } from "lucide-react";
import { useAuth } from "../../features/auth/AuthContext";
import type { Permission } from "../types/domain";
import { can, roleLabels } from "../lib/permissions";
import { cn } from "../lib/cn";
import { CommandPalette, type CommandPaletteItem } from "./CommandPalette";
import { IconButton } from "./ui";
import { useTheme } from "../providers/ThemeProvider";

interface NavItem {
  label: string;
  to: string;
  icon: LucideIcon;
  permission: Permission;
}

interface NavGroup {
  label: string;
  items: NavItem[];
}

const groups: NavGroup[] = [
  { label: "Visão geral", items: [{ label: "Dashboard", to: "/", icon: LayoutDashboard, permission: "dashboard:read" }] },
  { label: "Clínica", items: [{ label: "Agenda", to: "/agenda", icon: CalendarDays, permission: "clinical:read" }, { label: "Pacientes", to: "/patients", icon: Users, permission: "patients:read" }, { label: "Prontuários", to: "/clinical-records", icon: ClipboardList, permission: "clinical:read" }] },
  { label: "Operação", items: [{ label: "Pacotes", to: "/packages", icon: Sparkles, permission: "packages:read" }, { label: "Assinaturas", to: "/subscriptions", icon: WalletCards, permission: "packages:read" }] },
  { label: "Financeiro", items: [{ label: "Financeiro", to: "/finance", icon: CircleDollarSign, permission: "finance:read" }, { label: "Fiscal / NFS-e", to: "/fiscal", icon: FileText, permission: "fiscal:read" }] },
  { label: "Governança", items: [{ label: "Notificações", to: "/notifications", icon: Bell, permission: "notifications:read" }, { label: "LGPD & compliance", to: "/compliance", icon: ShieldCheck, permission: "privacy:read" }, { label: "Configurações", to: "/settings", icon: Settings, permission: "settings:manage" }] },
];

const commandDescriptions: Record<string, string> = {
  Dashboard: "Pulso da operação",
  Agenda: "Consultas e disponibilidade",
  Pacientes: "Cadastros e vínculos",
  Prontuários: "Registros clínicos protegidos",
  Pacotes: "Sessões e produtos clínicos",
  Assinaturas: "Ciclos e cobranças recorrentes",
  Financeiro: "Recebíveis e conciliação",
  "Fiscal / NFS-e": "Documentos fiscais",
  Notificações: "Entregas e preferências",
  "LGPD & compliance": "Privacidade e auditoria",
  Configurações: "Preferências do tenant",
};

export function AppShell({ children }: { children: ReactNode }) {
  const [mobileOpen, setMobileOpen] = useState(false);
  const { session, logout } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const location = useLocation();
  const navigate = useNavigate();

  const visibleGroups = useMemo(() => groups.map((group) => ({ ...group, items: group.items.filter((item) => can(session?.user ?? null, item.permission)) })).filter((group) => group.items.length > 0), [session?.user]);
  const allItems = groups.flatMap((group) => group.items);
  const currentItem = allItems.find((item) => item.to !== "/" && location.pathname.startsWith(item.to));
  const currentGroup = groups.find((group) => group.items.some((item) => item.to === currentItem?.to))?.label ?? "Visão geral";
  const pageTitle = location.pathname.includes("/clinical-record") ? "Registro clínico" : location.pathname === "/" ? "Visão geral" : currentItem?.label ?? "PsicoGest";
  const tenantName = session?.user.tenant.name ?? "Clínica";
  const commandItems: CommandPaletteItem[] = [...visibleGroups.flatMap((group) => group.items.map((item) => ({ label: item.label, description: commandDescriptions[item.label] ?? "Abrir módulo", group: group.label, to: item.to, keywords: [item.permission] }))), { label: "Perfil e segurança", description: "Conta, MFA e sessões", group: "Minha conta", to: "/profile", keywords: ["mfa", "sessão", "usuário"] }];

  const closeMobile = () => setMobileOpen(false);

  return (
    <div className="flex min-h-screen bg-cream text-ink dark:bg-slate-950 dark:text-white">
      {mobileOpen && <button type="button" aria-label="Fechar navegação" className="fixed inset-0 z-30 bg-slate-950/30 lg:hidden" onClick={closeMobile} />}
      <aside className={cn("fixed inset-y-0 left-0 z-40 flex w-72 shrink-0 flex-col border-r border-slate-200 bg-white px-4 py-5 transition-transform dark:border-slate-800 dark:bg-slate-950 lg:static lg:translate-x-0", mobileOpen ? "translate-x-0" : "-translate-x-full")}>
        <div className="flex items-center justify-between px-2">
          <button type="button" className="flex items-center gap-3 text-left" onClick={() => navigate("/")} aria-label={`Ir para o dashboard da ${tenantName}`}>
            <span aria-hidden="true" className="grid h-10 w-10 place-items-center rounded-2xl bg-sage-600 font-display text-lg font-extrabold text-white">P</span>
            <span><span className="block font-display text-lg font-extrabold tracking-tight text-ink dark:text-white">PsicoGest</span><span className="block max-w-[170px] truncate text-[11px] font-semibold uppercase tracking-widest text-slate-400">{tenantName}</span></span>
          </button>
          <IconButton label="Fechar menu" className="lg:hidden" onClick={closeMobile}><X size={18} aria-hidden="true" /></IconButton>
        </div>

        <nav className="mt-8 flex-1 space-y-6 overflow-y-auto px-1" aria-label="Navegação principal">
          {visibleGroups.map((group) => <div key={group.label}><p className="mb-2 px-3 text-[10px] font-bold uppercase tracking-[0.18em] text-slate-400">{group.label}</p><div className="space-y-1">{group.items.map((item) => { const Icon = item.icon; return <NavLink key={item.to} to={item.to} end={item.to === "/"} onClick={closeMobile} className={({ isActive }) => cn("flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-semibold transition", isActive ? "bg-sage-50 text-sage-700 dark:bg-sage-950/50 dark:text-sage-200" : "text-slate-500 hover:bg-slate-100 hover:text-slate-800 dark:text-slate-400 dark:hover:bg-slate-900 dark:hover:text-slate-100")}><Icon aria-hidden="true" size={18} strokeWidth={1.8} /><span>{item.label}</span></NavLink>; })}</div></div>)}
        </nav>

        <div className="rounded-2xl bg-ink p-4 text-white"><div className="flex items-center gap-2 text-sage-200"><Sparkles aria-hidden="true" size={16} /><span className="text-xs font-bold uppercase tracking-wider">Plano {session?.user.tenant.plan ?? "Growth"}</span></div><p className="mt-2 text-sm font-semibold">Operação protegida</p><p className="mt-1 text-xs leading-5 text-slate-300">Dados clínicos e financeiros separados por contexto.</p></div>
      </aside>

      <div className="min-w-0 flex-1">
        <header className="sticky top-0 z-20 flex h-[76px] items-center justify-between border-b border-slate-200/80 bg-cream/90 px-4 backdrop-blur-md dark:border-slate-800 dark:bg-slate-950/90 sm:px-8">
          <div className="flex min-w-0 items-center gap-3">
            <IconButton label="Abrir menu" className="lg:hidden" onClick={() => setMobileOpen(true)}><Menu size={20} aria-hidden="true" /></IconButton>
            <div className="min-w-0"><p className="hidden truncate text-xs font-medium text-slate-400 sm:block">{tenantName} / {currentGroup}</p><p className="truncate text-sm font-bold text-ink dark:text-white">{pageTitle}</p></div>
          </div>
          <div className="flex items-center gap-1 sm:gap-2">
            <CommandPalette items={commandItems} />
            <IconButton label={theme === "dark" ? "Ativar modo claro" : "Ativar modo escuro"} onClick={toggleTheme}>{theme === "dark" ? <Sun size={18} aria-hidden="true" /> : <Moon size={18} aria-hidden="true" />}</IconButton>
            <IconButton label="Notificações" onClick={() => navigate("/notifications")}><Bell size={18} aria-hidden="true" /></IconButton>
            <button type="button" className="ml-1 flex items-center gap-2 rounded-xl px-2 py-1.5 text-left transition hover:bg-white/70 dark:hover:bg-slate-900" onClick={() => navigate("/profile")} aria-label="Abrir perfil e segurança">
              <span className="grid h-9 w-9 place-items-center rounded-full bg-sage-200 text-sm font-bold text-sage-700 dark:bg-sage-800 dark:text-sage-100">{session?.user.initials}</span>
              <span className="hidden sm:block"><span className="block text-sm font-bold text-ink dark:text-white">{session?.user.name}</span><span className="block text-xs text-slate-500">{session ? roleLabels[session.user.role] : ""}</span></span>
              <ChevronDown className="hidden text-slate-400 sm:block" size={15} aria-hidden="true" />
            </button>
            <IconButton label="Sair" onClick={logout}><LogOut size={17} aria-hidden="true" /></IconButton>
          </div>
        </header>
        <main className="mx-auto max-w-[1600px] px-4 py-7 sm:px-8 lg:px-10">{children}</main>
      </div>
    </div>
  );
}
