import { Component, type ErrorInfo, type ReactNode } from "react";
import { AlertTriangle, RefreshCcw } from "lucide-react";
import { Button, Card } from "./ui";

interface Props { children: ReactNode }
interface State { hasError: boolean }

export class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false };
  static getDerivedStateFromError(): State { return { hasError: true }; }
  componentDidCatch(error: Error, info: ErrorInfo) { console.error("PsicoGest UI error", error, info.componentStack); }
  render() {
    if (!this.state.hasError) return this.props.children;
    return <main className="flex min-h-screen items-center justify-center bg-cream p-6 dark:bg-slate-950"><Card className="max-w-md p-8 text-center"><AlertTriangle className="mx-auto text-amber-600" size={32} /><h1 className="mt-4 font-display text-xl font-bold text-ink dark:text-white">Algo não carregou como esperado</h1><p className="mt-2 text-sm text-slate-500">Tente novamente. Nenhum conteúdo clínico é enviado para logs do navegador.</p><Button className="mt-6" onClick={() => { this.setState({ hasError: false }); window.location.reload(); }}><RefreshCcw size={16} /> Recarregar</Button></Card></main>;
  }
}
