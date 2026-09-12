import { Navigate, Outlet, useLocation } from "react-router-dom";
import type { Permission } from "../types/domain";
import { useAuth } from "../../features/auth/AuthContext";
import { can } from "../lib/permissions";
import { Card, PageHeader } from "./ui";
import { LockKeyhole } from "lucide-react";

export function RequireAuth() {
  const { session } = useAuth();
  const location = useLocation();
  return session ? <Outlet /> : <Navigate to="/login" replace state={{ from: location.pathname }} />;
}

export function RequirePermission({ permission }: { permission: Permission }) {
  const { session } = useAuth();
  if (!can(session?.user ?? null, permission)) {
    return <div className="mx-auto max-w-2xl"><PageHeader eyebrow="Acesso contextual" title="Permissão necessária" description="Seu perfil não possui acesso a este espaço da clínica." /><Card className="flex items-center gap-4 p-6"><div className="rounded-xl bg-amber-50 p-3 text-amber-700 dark:bg-amber-950/40 dark:text-amber-300"><LockKeyhole /></div><p className="text-sm text-slate-600 dark:text-slate-300">O servidor também deve validar esta permissão antes de retornar dados clínicos ou financeiros.</p></Card></div>;
  }
  return <Outlet />;
}
