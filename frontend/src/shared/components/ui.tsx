import type { ButtonHTMLAttributes, InputHTMLAttributes, ReactNode, RefObject, SelectHTMLAttributes, TextareaHTMLAttributes } from "react";
import { useEffect, useId, useRef } from "react";
import { AlertCircle, Check, Info, X } from "lucide-react";
import { cn } from "../lib/cn";

type ButtonVariant = "primary" | "secondary" | "ghost" | "danger";

export function Button({ className, variant = "primary", children, ...props }: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: ButtonVariant }) {
  return (
    <button type="button" className={cn("inline-flex min-h-10 items-center justify-center gap-2 rounded-xl px-4 text-sm font-semibold transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sage-500 focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50", variant === "primary" && "bg-sage-600 text-white shadow-sm hover:bg-sage-700", variant === "secondary" && "border border-sage-200 bg-white text-sage-700 hover:bg-sage-50 dark:border-sage-700 dark:bg-slate-900 dark:text-sage-200 dark:hover:bg-slate-800", variant === "ghost" && "text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-800", variant === "danger" && "bg-red-600 text-white hover:bg-red-700", className)} {...props}>
      {children}
    </button>
  );
}

export function IconButton({ label, className, children, ...props }: ButtonHTMLAttributes<HTMLButtonElement> & { label: string }) {
  return <button type="button" aria-label={label} title={label} className={cn("inline-flex h-10 w-10 items-center justify-center rounded-xl text-slate-500 transition hover:bg-slate-100 hover:text-slate-800 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sage-500 dark:hover:bg-slate-800 dark:hover:text-white", className)} {...props}>{children}</button>;
}

export function Card({ className, children, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return <div className={cn("rounded-2xl border border-slate-200/80 bg-white shadow-soft dark:border-slate-800 dark:bg-slate-900 dark:shadow-none", className)} {...props}>{children}</div>;
}

const badgeTones = {
  green: "bg-emerald-50 text-emerald-700 ring-emerald-600/20 dark:bg-emerald-950/40 dark:text-emerald-300",
  amber: "bg-amber-50 text-amber-700 ring-amber-600/20 dark:bg-amber-950/40 dark:text-amber-300",
  red: "bg-red-50 text-red-700 ring-red-600/20 dark:bg-red-950/40 dark:text-red-300",
  blue: "bg-blue-50 text-blue-700 ring-blue-600/20 dark:bg-blue-950/40 dark:text-blue-300",
  slate: "bg-slate-100 text-slate-600 ring-slate-500/20 dark:bg-slate-800 dark:text-slate-300",
  purple: "bg-violet-50 text-violet-700 ring-violet-600/20 dark:bg-violet-950/40 dark:text-violet-300",
} as const;

export function Badge({ children, tone = "slate", className }: { children: ReactNode; tone?: keyof typeof badgeTones; className?: string }) {
  return <span className={cn("inline-flex items-center rounded-full px-2.5 py-1 text-xs font-semibold ring-1 ring-inset", badgeTones[tone], className)}>{children}</span>;
}

export function StatusBadge({ value }: { value: string }) {
  const normalized = value.toUpperCase();
  const labels: Record<string, string> = {
    CONFIRMED: "Confirmada", PENDING: "Pendente", COMPLETED: "Concluída", CANCELLED: "Cancelada", NO_SHOW: "Não compareceu",
    ACTIVE: "Ativo", INACTIVE: "Inativo", CURRENT: "Em dia", REVIEW: "Revisar", DRAFT: "Rascunho", FINALIZED: "Finalizado",
    OPEN: "Em aberto", PAID: "Pago", OVERDUE: "Em atraso", SENT: "Enviado", FAILED: "Falhou", SUPPRESSED: "Suprimido",
    PROCESSING: "Processando", AUTHORIZED: "Autorizada", REJECTED: "Rejeitada", READY: "Pronto", PAST_DUE: "Em atraso", PAUSED: "Pausada",
    RECONCILED: "Conciliada", IGNORED: "Ignorada", REFUNDED: "Estornado", CRITICAL: "Crítico", WARNING: "Atenção", INFO: "Informativo",
    HIGH: "Alto", MEDIUM: "Médio", LOW: "Baixo", IN_PERSON: "Presencial", ONLINE: "Online",
  };
  const tone = normalized.includes("FAILED") || normalized.includes("OVERDUE") || normalized.includes("REJECTED") || normalized.includes("CRITICAL") || normalized === "PAST_DUE" ? "red" : normalized.includes("PENDING") || normalized.includes("DRAFT") || normalized.includes("REVIEW") || normalized.includes("WARNING") || normalized.includes("PROCESSING") || normalized === "OPEN" || normalized === "MEDIUM" ? "amber" : normalized.includes("FINALIZED") || normalized.includes("COMPLETED") || normalized.includes("PAID") || normalized.includes("ACTIVE") || normalized.includes("AUTHORIZED") || normalized.includes("SENT") || normalized.includes("CONFIRMED") || normalized.includes("READY") || normalized.includes("CURRENT") || normalized.includes("RECONCILED") ? "green" : normalized.includes("SUPPRESSED") || normalized.includes("CANCELLED") || normalized.includes("IGNORED") ? "slate" : "blue";
  return <Badge tone={tone}>{labels[normalized] ?? value.replaceAll("_", " ")}</Badge>;
}

export function Input({ label, error, hint, className, id, ...props }: InputHTMLAttributes<HTMLInputElement> & { label?: string; error?: string; hint?: string }) {
  const generatedId = useId().replaceAll(":", "");
  const inputId = id ?? props.name ?? `field-${generatedId}`;
  const errorId = `${inputId}-error`;
  const hintId = `${inputId}-hint`;
  const describedBy = [props["aria-describedby"], error ? errorId : hint ? hintId : undefined].filter(Boolean).join(" ") || undefined;
  return <label className="block space-y-1.5" htmlFor={inputId}>
    {label && <span className="text-sm font-semibold text-slate-700 dark:text-slate-200">{label}</span>}
    <input id={inputId} className={cn("block min-h-11 w-full rounded-xl border border-slate-200 bg-white px-3.5 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-sage-500 focus:ring-2 focus:ring-sage-500/20 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100", error && "border-red-400 focus:border-red-500 focus:ring-red-500/20", className)} {...props} aria-invalid={error ? true : undefined} aria-describedby={describedBy} />
    {error ? <span id={errorId} className="text-xs font-medium text-red-600" role="alert">{error}</span> : hint ? <span id={hintId} className="text-xs text-slate-500">{hint}</span> : null}
  </label>;
}

export function Select({ label, error, className, id, children, ...props }: SelectHTMLAttributes<HTMLSelectElement> & { label?: string; error?: string }) {
  const generatedId = useId().replaceAll(":", "");
  const selectId = id ?? props.name ?? `field-${generatedId}`;
  const errorId = `${selectId}-error`;
  const describedBy = [props["aria-describedby"], error ? errorId : undefined].filter(Boolean).join(" ") || undefined;
  return <label className="block space-y-1.5" htmlFor={selectId}>
    {label && <span className="text-sm font-semibold text-slate-700 dark:text-slate-200">{label}</span>}
    <select id={selectId} className={cn("block min-h-11 w-full rounded-xl border border-slate-200 bg-white px-3.5 text-sm text-slate-900 outline-none transition focus:border-sage-500 focus:ring-2 focus:ring-sage-500/20 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100", error && "border-red-400 focus:border-red-500 focus:ring-red-500/20", className)} {...props} aria-invalid={error ? true : undefined} aria-describedby={describedBy}>{children}</select>
    {error && <span id={errorId} className="text-xs font-medium text-red-600" role="alert">{error}</span>}
  </label>;
}

export function Textarea({ label, error, className, id, ...props }: TextareaHTMLAttributes<HTMLTextAreaElement> & { label?: string; error?: string }) {
  const generatedId = useId().replaceAll(":", "");
  const textareaId = id ?? props.name ?? `field-${generatedId}`;
  const errorId = `${textareaId}-error`;
  const describedBy = [props["aria-describedby"], error ? errorId : undefined].filter(Boolean).join(" ") || undefined;
  return <label className="block space-y-1.5" htmlFor={textareaId}>
    {label && <span className="text-sm font-semibold text-slate-700 dark:text-slate-200">{label}</span>}
    <textarea id={textareaId} className={cn("block min-h-32 w-full resize-y rounded-xl border border-slate-200 bg-white px-3.5 py-3 text-sm text-slate-900 outline-none transition focus:border-sage-500 focus:ring-2 focus:ring-sage-500/20 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100", error && "border-red-400 focus:border-red-500 focus:ring-red-500/20", className)} {...props} aria-invalid={error ? true : undefined} aria-describedby={describedBy} />
    {error && <span id={errorId} className="text-xs font-medium text-red-600" role="alert">{error}</span>}
  </label>;
}

export function PageHeader({ eyebrow, title, description, actions }: { eyebrow?: string; title: string; description?: string; actions?: ReactNode }) {
  return <div className="mb-7 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
    <div><p className="mb-1 text-xs font-bold uppercase tracking-[0.18em] text-sage-600 dark:text-sage-300">{eyebrow ?? "PsicoGest"}</p><h1 className="font-display text-3xl font-extrabold tracking-tight text-ink dark:text-white">{title}</h1>{description && <p className="mt-2 max-w-2xl text-sm text-slate-500 dark:text-slate-400">{description}</p>}</div>
    {actions && <div className="flex flex-wrap items-center gap-2">{actions}</div>}
  </div>;
}

export function SectionHeader({ title, description, action }: { title: string; description?: string; action?: ReactNode }) {
  return <div className="mb-4 flex items-start justify-between gap-4"><div><h2 className="font-display text-lg font-bold text-ink dark:text-white">{title}</h2>{description && <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">{description}</p>}</div>{action}</div>;
}

export function StatCard({ label, value, change, trend, icon }: { label: string; value: string; change: string; trend: "up" | "down" | "neutral"; icon: ReactNode }) {
  return <Card className="p-5"><div className="flex items-start justify-between"><div className="rounded-xl bg-sage-50 p-2.5 text-sage-600 dark:bg-sage-950/40 dark:text-sage-300">{icon}</div><span className={cn("text-xs font-bold", trend === "up" ? "text-emerald-600" : trend === "down" ? "text-amber-600" : "text-slate-500")}>{change}</span></div><p className="mt-5 text-sm text-slate-500 dark:text-slate-400">{label}</p><p className="mt-1 font-display text-2xl font-extrabold text-ink dark:text-white">{value}</p></Card>;
}

export function EmptyState({ title, description, action, icon }: { title: string; description: string; action?: ReactNode; icon?: ReactNode }) {
  return <div className="flex min-h-52 flex-col items-center justify-center rounded-2xl border border-dashed border-slate-300 px-6 text-center dark:border-slate-700"><div className="mb-3 rounded-full bg-slate-100 p-3 text-slate-500 dark:bg-slate-800">{icon ?? <Info size={20} />}</div><h3 className="font-semibold text-ink dark:text-white">{title}</h3><p className="mt-1 max-w-md text-sm text-slate-500 dark:text-slate-400">{description}</p>{action && <div className="mt-4">{action}</div>}</div>;
}

export function Skeleton({ className }: { className?: string }) { return <div className={cn("animate-pulse rounded-xl bg-slate-200 dark:bg-slate-800", className)} aria-hidden="true" />; }

export function Modal({ open, title, description, onClose, children, size = "md", initialFocusRef }: { open: boolean; title: string; description?: string; onClose: () => void; children: ReactNode; size?: "md" | "lg"; initialFocusRef?: RefObject<HTMLElement | null> }) {
  const contentRef = useRef<HTMLDivElement>(null);
  const onCloseRef = useRef(onClose);
  const titleId = useId().replaceAll(":", "");
  onCloseRef.current = onClose;
  useEffect(() => {
    if (!open) return;
    const previousActiveElement = document.activeElement as HTMLElement | null;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    (initialFocusRef?.current ?? contentRef.current)?.focus();
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") onCloseRef.current();
      if (event.key !== "Tab" || !contentRef.current) return;
      const focusable = Array.from(contentRef.current.querySelectorAll<HTMLElement>("button, [href], input, select, textarea, [tabindex]:not([tabindex=\"-1\"])"));
      if (!focusable.length) return;
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
      else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
    };
    document.addEventListener("keydown", onKeyDown);
    return () => { document.removeEventListener("keydown", onKeyDown); document.body.style.overflow = previousOverflow; previousActiveElement?.focus(); };
  }, [initialFocusRef, open]);
  if (!open) return null;
  return <div className="fixed inset-0 z-50 flex items-end justify-center bg-slate-950/40 p-0 backdrop-blur-sm sm:items-center sm:p-6" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}><div ref={contentRef} tabIndex={-1} role="dialog" aria-modal="true" aria-labelledby={titleId} className={cn("max-h-[92vh] w-full overflow-y-auto rounded-t-3xl bg-white p-6 shadow-2xl outline-none dark:bg-slate-900 sm:rounded-3xl", size === "lg" ? "max-w-3xl" : "max-w-xl")}><div className="mb-6 flex items-start justify-between gap-4"><div><h2 id={titleId} className="font-display text-xl font-bold text-ink dark:text-white">{title}</h2>{description && <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">{description}</p>}</div><IconButton label="Fechar" onClick={onClose}><X size={18} /></IconButton></div>{children}</div></div>;
}

export function Notice({ title, children, tone = "info" }: { title: string; children: ReactNode; tone?: "info" | "warning" | "success" }) {
  const styles = { info: "border-blue-200 bg-blue-50 text-blue-800 dark:border-blue-900 dark:bg-blue-950/30 dark:text-blue-200", warning: "border-amber-200 bg-amber-50 text-amber-800 dark:border-amber-900 dark:bg-amber-950/30 dark:text-amber-200", success: "border-emerald-200 bg-emerald-50 text-emerald-800 dark:border-emerald-900 dark:bg-emerald-950/30 dark:text-emerald-200" };
  const Icon = tone === "warning" ? AlertCircle : tone === "success" ? Check : Info;
  return <div className={cn("flex gap-3 rounded-2xl border p-4 text-sm", styles[tone])} role="status"><Icon className="mt-0.5 shrink-0" size={18} /><div><p className="font-bold">{title}</p><p className="mt-1 opacity-90">{children}</p></div></div>;
}
