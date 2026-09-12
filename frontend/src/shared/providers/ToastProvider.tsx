import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from "react";
import { AlertCircle, CheckCircle2, X } from "lucide-react";
import { IconButton } from "../components/ui";

interface Toast { id: number; message: string; tone: "success" | "error" }
const ToastContext = createContext<{ toast: (message: string, tone?: Toast["tone"]) => void } | undefined>(undefined);

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);
  const toast = useCallback((message: string, tone: Toast["tone"] = "success") => {
    const id = Date.now();
    setToasts((current) => [...current, { id, message, tone }]);
    window.setTimeout(() => setToasts((current) => current.filter((item) => item.id !== id)), 4000);
  }, []);
  const value = useMemo(() => ({ toast }), [toast]);
  return <ToastContext.Provider value={value}>{children}<div className="fixed bottom-5 right-5 z-[60] flex w-[min(360px,calc(100vw-2rem))] flex-col gap-2" aria-live="polite">{toasts.map((item) => <div key={item.id} className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-white p-4 text-sm font-semibold text-ink shadow-2xl dark:border-slate-700 dark:bg-slate-900 dark:text-white" role={item.tone === "error" ? "alert" : "status"}>{item.tone === "error" ? <AlertCircle className="text-red-500" size={18} /> : <CheckCircle2 className="text-emerald-500" size={18} />}<span className="flex-1">{item.message}</span><IconButton label="Dispensar aviso" onClick={() => setToasts((current) => current.filter((toastItem) => toastItem.id !== item.id))}><X size={15} /></IconButton></div>)}</div></ToastContext.Provider>;
}

export function useToast() { const context = useContext(ToastContext); if (!context) throw new Error("useToast precisa estar dentro de ToastProvider"); return context.toast; }
