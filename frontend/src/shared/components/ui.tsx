import type { ButtonHTMLAttributes, InputHTMLAttributes, ReactNode, RefObject, SelectHTMLAttributes, TextareaHTMLAttributes } from "react";
import { useEffect, useId, useRef, useState } from "react";
import { AlertCircle, AlertTriangle, Check, Info, Loader2, Search, X } from "lucide-react";
import { cn } from "../lib/cn";
import { statusMeta } from "../lib/status";

type ButtonVariant = "primary" | "secondary" | "ghost" | "danger";

export function Button({ className, variant = "primary", children, ...props }: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: ButtonVariant }) {
  return (
    <button type="button" className={cn("inline-flex min-h-10 items-center justify-center gap-2 rounded-xl px-4 text-sm font-semibold transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-harbor-500 focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50", variant === "primary" && "bg-harbor-600 text-white shadow-sm hover:bg-harbor-700", variant === "secondary" && "border border-harbor-200 bg-white text-harbor-700 hover:bg-harbor-50 dark:border-harbor-700 dark:bg-slate-900 dark:text-harbor-200 dark:hover:bg-slate-800", variant === "ghost" && "text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-800", variant === "danger" && "bg-red-600 text-white hover:bg-red-700", className)} {...props}>
      {children}
    </button>
  );
}

export function IconButton({ label, className, children, ...props }: ButtonHTMLAttributes<HTMLButtonElement> & { label: string }) {
  return <button type="button" aria-label={label} title={label} className={cn("inline-flex h-10 w-10 items-center justify-center rounded-xl text-slate-500 dark:text-slate-400 transition hover:bg-slate-100 hover:text-slate-800 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-harbor-500 dark:hover:bg-slate-800 dark:hover:text-white", className)} {...props}>{children}</button>;
}

export function Card({ className, children, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return <div className={cn("rounded-2xl border border-slate-200/80 bg-white shadow-soft dark:border-slate-800 dark:bg-slate-900 dark:shadow-none", className)} {...props}>{children}</div>;
}

const badgeTones = {
  // Tons semânticos (preferir estes) — mapeados por lib/status.ts.
  neutral: "bg-slate-100 text-slate-600 ring-slate-500/20 dark:bg-slate-800 dark:text-slate-300",
  info: "bg-blue-50 text-blue-700 ring-blue-600/20 dark:bg-blue-950/40 dark:text-blue-300",
  success: "bg-emerald-50 text-emerald-700 ring-emerald-600/20 dark:bg-emerald-950/40 dark:text-emerald-300",
  warning: "bg-amber-50 text-amber-700 ring-amber-600/20 dark:bg-amber-950/40 dark:text-amber-300",
  danger: "bg-red-50 text-red-700 ring-red-600/20 dark:bg-red-950/40 dark:text-red-300",
  accent: "bg-amber-50 text-amber-800 ring-amber-600/20 dark:bg-amber-950/40 dark:text-amber-200",
  // Aliases legados por cor — mantidos para compatibilidade de call sites.
  green: "bg-emerald-50 text-emerald-700 ring-emerald-600/20 dark:bg-emerald-950/40 dark:text-emerald-300",
  amber: "bg-amber-50 text-amber-700 ring-amber-600/20 dark:bg-amber-950/40 dark:text-amber-300",
  red: "bg-red-50 text-red-700 ring-red-600/20 dark:bg-red-950/40 dark:text-red-300",
  blue: "bg-blue-50 text-blue-700 ring-blue-600/20 dark:bg-blue-950/40 dark:text-blue-300",
  slate: "bg-slate-100 text-slate-600 ring-slate-500/20 dark:bg-slate-800 dark:text-slate-300",
  purple: "bg-violet-50 text-violet-700 ring-violet-600/20 dark:bg-violet-950/40 dark:text-violet-300",
} as const;

export function Badge({ children, tone = "slate", className, title }: { children: ReactNode; tone?: keyof typeof badgeTones; className?: string; title?: string }) {
  return <span title={title} className={cn("inline-flex items-center rounded-full px-2.5 py-1 text-xs font-semibold ring-1 ring-inset", badgeTones[tone], className)}>{children}</span>;
}

export function StatusBadge({ value, className }: { value: string | null | undefined; className?: string }) {
  const meta = statusMeta(value);
  return <Badge tone={meta.tone} title={meta.description} className={className}>{meta.label}</Badge>;
}

export function Input({ label, error, hint, leadingIcon, className, id, ...props }: InputHTMLAttributes<HTMLInputElement> & { label?: string; error?: string; hint?: string; leadingIcon?: ReactNode }) {
  const generatedId = useId().replaceAll(":", "");
  const inputId = id ?? props.name ?? `field-${generatedId}`;
  const errorId = `${inputId}-error`;
  const hintId = `${inputId}-hint`;
  const describedBy = [props["aria-describedby"], error ? errorId : hint ? hintId : undefined].filter(Boolean).join(" ") || undefined;
  return <label className="block space-y-1.5" htmlFor={inputId}>
    {label && <span className="text-sm font-semibold text-slate-700 dark:text-slate-200">{label}</span>}
    <span className="relative block">{leadingIcon && <span className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-500 dark:text-slate-400" aria-hidden="true">{leadingIcon}</span>}<input id={inputId} className={cn("block min-h-11 w-full rounded-xl border border-slate-200 bg-white px-3.5 text-sm text-slate-900 outline-none transition placeholder:text-slate-500 dark:placeholder:text-slate-400 focus:border-harbor-500 focus:ring-2 focus:ring-harbor-500/20 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100", Boolean(leadingIcon) && "pl-10", error && "border-red-400 focus:border-red-500 focus:ring-red-500/20", className)} {...props} aria-invalid={error ? true : undefined} aria-describedby={describedBy} /></span>
    {error ? <span id={errorId} className="text-xs font-medium text-red-600 dark:text-red-400" role="alert">{error}</span> : hint ? <span id={hintId} className="text-xs text-slate-500 dark:text-slate-400">{hint}</span> : null}
  </label>;
}

export function Select({ label, error, className, id, children, ...props }: SelectHTMLAttributes<HTMLSelectElement> & { label?: string; error?: string }) {
  const generatedId = useId().replaceAll(":", "");
  const selectId = id ?? props.name ?? `field-${generatedId}`;
  const errorId = `${selectId}-error`;
  const describedBy = [props["aria-describedby"], error ? errorId : undefined].filter(Boolean).join(" ") || undefined;
  return <label className="block space-y-1.5" htmlFor={selectId}>
    {label && <span className="text-sm font-semibold text-slate-700 dark:text-slate-200">{label}</span>}
    <select id={selectId} className={cn("block min-h-11 w-full rounded-xl border border-slate-200 bg-white px-3.5 text-sm text-slate-900 outline-none transition focus:border-harbor-500 focus:ring-2 focus:ring-harbor-500/20 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100", error && "border-red-400 focus:border-red-500 focus:ring-red-500/20", className)} {...props} aria-invalid={error ? true : undefined} aria-describedby={describedBy}>{children}</select>
    {error && <span id={errorId} className="text-xs font-medium text-red-600 dark:text-red-400" role="alert">{error}</span>}
  </label>;
}

export function Textarea({ label, error, className, id, ...props }: TextareaHTMLAttributes<HTMLTextAreaElement> & { label?: string; error?: string }) {
  const generatedId = useId().replaceAll(":", "");
  const textareaId = id ?? props.name ?? `field-${generatedId}`;
  const errorId = `${textareaId}-error`;
  const describedBy = [props["aria-describedby"], error ? errorId : undefined].filter(Boolean).join(" ") || undefined;
  return <label className="block space-y-1.5" htmlFor={textareaId}>
    {label && <span className="text-sm font-semibold text-slate-700 dark:text-slate-200">{label}</span>}
    <textarea id={textareaId} className={cn("block min-h-32 w-full resize-y rounded-xl border border-slate-200 bg-white px-3.5 py-3 text-sm text-slate-900 outline-none transition focus:border-harbor-500 focus:ring-2 focus:ring-harbor-500/20 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100", error && "border-red-400 focus:border-red-500 focus:ring-red-500/20", className)} {...props} aria-invalid={error ? true : undefined} aria-describedby={describedBy} />
    {error && <span id={errorId} className="text-xs font-medium text-red-600 dark:text-red-400" role="alert">{error}</span>}
  </label>;
}

export function PageHeader({ eyebrow, title, description, actions }: { eyebrow?: string; title: string; description?: string; actions?: ReactNode }) {
  return <div className="mb-7 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
    <div>{eyebrow && <p className="mb-1.5 text-sm font-medium text-harbor-600 dark:text-harbor-300">{eyebrow}</p>}<h1 className="font-display text-3xl font-semibold tracking-tight text-ink dark:text-white">{title}</h1>{description && <p className="mt-2 max-w-2xl text-sm text-ink-soft dark:text-slate-400">{description}</p>}</div>
    {actions && <div className="flex flex-wrap items-center gap-2">{actions}</div>}
  </div>;
}

export function SectionHeader({ title, description, action }: { title: string; description?: string; action?: ReactNode }) {
  return <div className="mb-4 flex items-start justify-between gap-4"><div><h2 className="font-display text-lg font-bold text-ink dark:text-white">{title}</h2>{description && <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">{description}</p>}</div>{action}</div>;
}

export function StatCard({ label, value, change, trend, icon }: { label: string; value: string; change: string; trend: "up" | "down" | "neutral"; icon: ReactNode }) {
  return <Card className="p-5"><div className="flex items-start justify-between"><div className="rounded-xl bg-harbor-50 p-2.5 text-harbor-600 dark:bg-harbor-950/40 dark:text-harbor-300">{icon}</div><span className={cn("text-xs font-bold", trend === "up" ? "text-emerald-700 dark:text-emerald-400" : trend === "down" ? "text-amber-700 dark:text-amber-300" : "text-slate-500 dark:text-slate-400")}>{change}</span></div><p className="mt-5 text-sm text-slate-500 dark:text-slate-400">{label}</p><p className="mt-1 font-display text-2xl font-extrabold text-ink dark:text-white">{value}</p></Card>;
}

export function EmptyState({ title, description, action, icon }: { title: string; description: string; action?: ReactNode; icon?: ReactNode }) {
  return <div className="flex min-h-52 flex-col items-center justify-center rounded-2xl border border-dashed border-slate-300 px-6 text-center dark:border-slate-700"><div className="mb-3 rounded-full bg-slate-100 p-3 text-slate-500 dark:text-slate-400 dark:bg-slate-800">{icon ?? <Info size={20} />}</div><h3 className="font-semibold text-ink dark:text-white">{title}</h3><p className="mt-1 max-w-md text-sm text-slate-500 dark:text-slate-400">{description}</p>{action && <div className="mt-4">{action}</div>}</div>;
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

export function ConfirmDialog({ open, title, description, confirmLabel = "Confirmar", cancelLabel = "Cancelar", tone = "danger", loading = false, confirmWord, onConfirm, onClose }: { open: boolean; title: string; description: ReactNode; confirmLabel?: string; cancelLabel?: string; tone?: "danger" | "primary"; loading?: boolean; confirmWord?: string; onConfirm: () => void; onClose: () => void }) {
  const [typed, setTyped] = useState("");
  useEffect(() => { if (!open) setTyped(""); }, [open]);
  const needsWord = Boolean(confirmWord);
  const canConfirm = !loading && (!needsWord || typed.trim().toUpperCase() === confirmWord!.toUpperCase());
  return <Modal open={open} title={title} onClose={onClose} size="md">
    <div className="space-y-5">
      <div className="flex gap-3 text-sm text-ink-soft dark:text-slate-300"><span className={cn("mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-full", tone === "danger" ? "bg-danger-50 text-danger-600" : "bg-harbor-50 text-harbor-600")}><AlertTriangle size={18} /></span><div className="space-y-1 leading-relaxed">{description}</div></div>
      {needsWord && <Input label={`Para confirmar, digite ${confirmWord}`} value={typed} onChange={(event) => setTyped(event.target.value)} autoComplete="off" autoCapitalize="characters" />}
      <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
        <Button variant="secondary" onClick={onClose} disabled={loading}>{cancelLabel}</Button>
        <Button variant={tone === "danger" ? "danger" : "primary"} onClick={onConfirm} disabled={!canConfirm}>{loading && <Loader2 className="animate-spin" size={16} />}{confirmLabel}</Button>
      </div>
    </div>
  </Modal>;
}

export function SearchInput({ value, onChange, placeholder = "Buscar", label, className, onClear }: { value: string; onChange: (value: string) => void; placeholder?: string; label?: string; className?: string; onClear?: () => void }) {
  return <div className={cn("relative", className)}>
    <Search className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-500 dark:text-slate-400" size={18} aria-hidden="true" />
    <input type="search" aria-label={label ?? placeholder} value={value} onChange={(event) => onChange(event.target.value)} placeholder={placeholder} className="block min-h-11 w-full rounded-xl border border-slate-200 bg-white pl-10 pr-10 text-sm text-slate-900 outline-none transition placeholder:text-slate-500 dark:placeholder:text-slate-400 focus:border-harbor-500 focus:ring-2 focus:ring-harbor-500/20 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100" />
    {value && <button type="button" aria-label="Limpar busca" onClick={() => { onChange(""); onClear?.(); }} className="absolute right-2.5 top-1/2 -translate-y-1/2 rounded-lg p-1 text-slate-500 dark:text-slate-400 transition hover:bg-slate-100 hover:text-slate-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-harbor-500 dark:hover:bg-slate-800"><X size={16} /></button>}
  </div>;
}

export function Tabs<T extends string>({ tabs, value, onChange, className }: { tabs: { id: T; label: string; icon?: ReactNode }[]; value: T; onChange: (id: T) => void; className?: string }) {
  const onKeyDown = (event: React.KeyboardEvent) => {
    const index = tabs.findIndex((tab) => tab.id === value);
    if (event.key === "ArrowRight" || event.key === "ArrowDown") { event.preventDefault(); onChange(tabs[(index + 1) % tabs.length].id); }
    else if (event.key === "ArrowLeft" || event.key === "ArrowUp") { event.preventDefault(); onChange(tabs[(index - 1 + tabs.length) % tabs.length].id); }
    else if (event.key === "Home") { event.preventDefault(); onChange(tabs[0].id); }
    else if (event.key === "End") { event.preventDefault(); onChange(tabs[tabs.length - 1].id); }
  };
  return <div role="tablist" onKeyDown={onKeyDown} className={cn("flex flex-wrap gap-1 rounded-xl border border-slate-200 bg-slate-50 p-1 dark:border-slate-800 dark:bg-slate-900", className)}>
    {tabs.map((tab) => { const active = tab.id === value; return <button key={tab.id} type="button" role="tab" id={`tab-${tab.id}`} aria-selected={active} aria-controls={`panel-${tab.id}`} tabIndex={active ? 0 : -1} onClick={() => onChange(tab.id)} className={cn("inline-flex min-h-9 flex-1 items-center justify-center gap-2 rounded-lg px-3 text-sm font-semibold transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-harbor-500", active ? "bg-white text-harbor-700 shadow-sm dark:bg-slate-800 dark:text-harbor-200" : "text-ink-soft hover:text-ink dark:text-slate-400 dark:hover:text-slate-200")}>{tab.icon}{tab.label}</button>; })}
  </div>;
}

export function TabPanel({ id, active, children, className }: { id: string; active: boolean; children: ReactNode; className?: string }) {
  if (!active) return null;
  return <div role="tabpanel" id={`panel-${id}`} aria-labelledby={`tab-${id}`} tabIndex={0} className={cn("focus-visible:outline-none", className)}>{children}</div>;
}
