import { useState } from "react";
import { Navigate, Outlet, useLocation } from "react-router-dom";
import type { Permission } from "../types/domain";
import { useAuth } from "../../features/auth/AuthContext";
import { can } from "../lib/permissions";
import { Card, PageHeader } from "./ui";
import { LockKeyhole } from "lucide-react";
import { newCorrelationId } from "../lib/correlation";

export function RequireAuth() {
  const { session, isInitializing } = useAuth();
  const location = useLocation();
  if (isInitializing) return <div className="grid min-h-screen place-items-center bg-paper text-sm text-slate-500 dark:bg-slate-950 dark:text-slate-300">Validando sua sessão…</div>;
  return session ? <Outlet /> : <Navigate to="/login" replace state={{ from: location.pathname }} />;
}

export function RequirePermission({ permission }: { permission: Permission }) {
  const { session } = useAuth();
  if (!can(session?.user ?? null, permission)) return <PermissionDenied />;
  return <Outlet />;
}

function PermissionDenied() {
  const [correlationId] = useState(newCorrelationId);
  return <div className="mx-auto max-w-2xl"><PageHeader eyebrow="Acesso contextual" title="Permissão necessária" description="Seu perfil não possui acesso a este espaço da clínica." /><Card className="flex flex-col gap-4 p-6"><div className="flex items-center gap-4"><div className="rounded-xl bg-amber-50 p-3 text-amber-700 dark:bg-amber-950/40 dark:text-amber-300"><LockKeyhole /></div><p className="text-sm text-slate-600 dark:text-slate-300">O servidor também deve validar esta permissão antes de retornar dados clínicos ou financeiros.</p></div><p className="text-xs text-slate-500 dark:text-slate-400 dark:text-slate-500">Código de referência: <span className="font-mono">{correlationId}</span></p></Card></div>;
}
