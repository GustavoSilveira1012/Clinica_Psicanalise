import { Component, type ErrorInfo, type ReactNode } from "react";
import { AlertTriangle, RefreshCcw } from "lucide-react";
import { Button, Card } from "./ui";
import { newCorrelationId } from "../lib/correlation";

interface Props { children: ReactNode }
interface State { hasError: boolean; correlationId: string | null }

export class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false, correlationId: null };
  static getDerivedStateFromError(): State { return { hasError: true, correlationId: newCorrelationId() }; }
  componentDidCatch(error: Error, info: ErrorInfo) { console.error("PsicoGest UI error", this.state.correlationId, error, info.componentStack); }
  render() {
    if (!this.state.hasError) return this.props.children;
    return <main className="flex min-h-screen items-center justify-center bg-paper p-6 dark:bg-slate-950"><Card className="max-w-md p-8 text-center"><AlertTriangle className="mx-auto text-amber-600" size={32} /><h1 className="mt-4 font-display text-xl font-bold text-ink dark:text-white">Algo não carregou como esperado</h1><p className="mt-2 text-sm text-slate-500 dark:text-slate-400">Tente novamente. Nenhum conteúdo clínico é enviado para logs do navegador.</p><Button className="mt-6" onClick={() => { this.setState({ hasError: false, correlationId: null }); window.location.reload(); }}><RefreshCcw size={16} /> Recarregar</Button>{this.state.correlationId && <p className="mt-6 text-xs text-slate-500 dark:text-slate-400 dark:text-slate-500">Código de referência: <span className="font-mono">{this.state.correlationId}</span></p>}</Card></main>;
  }
}
