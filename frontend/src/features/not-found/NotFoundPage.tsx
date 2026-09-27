import { useState } from "react";
import { ArrowLeft, Compass } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { Button, Card, PageHeader } from "../../shared/components/ui";
import { newCorrelationId } from "../../shared/lib/correlation";

export function NotFoundPage() {
  const navigate = useNavigate();
  const [correlationId] = useState(newCorrelationId);

  return <div className="mx-auto max-w-2xl"><PageHeader eyebrow="Navegação" title="Página não encontrada" description="O endereço pode ter mudado ou não está disponível para este contexto." /><Card className="flex flex-col items-center p-8 text-center sm:p-12"><span className="grid h-14 w-14 place-items-center rounded-2xl bg-harbor-50 text-harbor-700 dark:bg-harbor-950/40 dark:text-harbor-200"><Compass size={26} /></span><p className="mt-5 text-sm text-slate-500 dark:text-slate-400">Volte para uma área conhecida da sua operação.</p><Button className="mt-6" onClick={() => navigate("/dashboard")}><ArrowLeft size={16} /> Ir para o dashboard</Button><p className="mt-6 text-xs text-slate-500 dark:text-slate-400">Código de referência: <span className="font-mono">{correlationId}</span></p></Card></div>;
}
