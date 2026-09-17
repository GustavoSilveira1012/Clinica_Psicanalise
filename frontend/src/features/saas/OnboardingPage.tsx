import { ArrowRight, Check, Circle, LifeBuoy, MessageSquareText, ShieldCheck, UsersRound } from "lucide-react";
import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate, useParams } from "react-router-dom";
import { Button, Card, EmptyState, Notice, PageHeader, Skeleton } from "../../shared/components/ui";
import { getOnboarding, updateOnboarding, type OnboardingSummary } from "../../shared/lib/saas-api";
import { useAuth } from "../auth/AuthContext";

const steps: Array<{ key: Exclude<OnboardingSummary["currentStep"], "COMPLETE" | "CLINIC"> | "PROFILE"; title: string; description: string; icon: typeof ShieldCheck }> = [
  { key: "PROFILE", title: "Perfil da organização", description: "Nome, slug e fuso horário", icon: ShieldCheck },
  { key: "TEAM", title: "Convide sua equipe", description: "Papéis claros e acesso mínimo", icon: UsersRound },
  { key: "NOTIFICATIONS", title: "Canais de contato", description: "Configure os canais operacionais", icon: MessageSquareText },
  { key: "FIRST_APPOINTMENT", title: "Primeiro atendimento", description: "Agende uma consulta com segurança", icon: LifeBuoy },
];

export function OnboardingPage() {
  const { organizationSlug } = useParams();
  const { session } = useAuth();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const demoMode = import.meta.env.DEV && import.meta.env.VITE_DEMO_MODE === "true";
  const organizationId = session?.user.tenant.id;
  const [demoCompleted, setDemoCompleted] = useState<string[]>(["PROFILE"]);
  const [notice, setNotice] = useState<string | null>(null);
  const onboarding = useQuery({ queryKey: ["onboarding", organizationId], queryFn: () => getOnboarding(organizationId ?? ""), enabled: !demoMode && Boolean(organizationId && organizationId !== "unselected") });
  const update = useMutation({ mutationFn: (step: OnboardingSummary["currentStep"]) => updateOnboarding(organizationId ?? "", step), onSuccess: (value) => { queryClient.setQueryData(["onboarding", organizationId], value); setNotice("Progresso salvo no backend da organização."); } });
  const completed = useMemo(() => new Set(demoMode ? demoCompleted : onboarding.data?.completedSteps ?? []), [demoCompleted, demoMode, onboarding.data?.completedSteps]);
  const finished = !demoMode && onboarding.data?.status === "COMPLETED";
  const next = steps.find((step) => !completed.has(step.key));
  const complete = (key: string) => {
    if (demoMode) { setDemoCompleted((current) => current.includes(key) ? current : [...current, key]); setNotice("Progresso salvo apenas no cenário demonstrativo."); return; }
    update.mutate(key as OnboardingSummary["currentStep"]);
  };
  const finish = () => { if (demoMode) { setDemoCompleted(steps.map((step) => step.key)); setNotice("Onboarding concluído apenas no cenário demonstrativo."); return; } update.mutate("COMPLETE"); };

  if (!demoMode && onboarding.isPending) return <><Skeleton className="h-10 w-72" /><Skeleton className="mt-6 h-96" /></>;
  if (!demoMode && onboarding.isError) return <div><PageHeader eyebrow="Primeiros passos" title="Prepare sua organização" /><EmptyState title="Onboarding indisponível" description="Não foi possível consultar o progresso desta organização." action={<Button onClick={() => onboarding.refetch()}>Tentar novamente</Button>} /></div>;
  if (!demoMode && !organizationId) return <EmptyState title="Organização não selecionada" description="Selecione uma organização ativa para continuar a configuração." />;

  const completionPercent = finished ? 100 : Math.round((completed.size / steps.length) * 100);
  return <div className="mx-auto max-w-4xl"><PageHeader eyebrow="Primeiros passos" title="Prepare sua organização" description="Uma configuração guiada para começar com segurança, sem misturar o contexto SaaS com a rotina clínica." />{notice && <div className="mb-6"><Notice title={demoMode ? "Demonstração local" : "Progresso salvo"} tone={demoMode ? "info" : "success"}>{notice}</Notice></div>}<Card className="overflow-hidden"><div className="border-b border-slate-100 bg-sage-50/50 p-6 dark:border-slate-800 dark:bg-sage-950/20"><div className="flex items-center justify-between"><div><p className="text-xs font-bold uppercase tracking-[0.18em] text-sage-700 dark:text-sage-300">{completed.size} de {steps.length} concluídas</p><h2 className="mt-2 font-display text-xl font-bold text-ink dark:text-white">{finished ? "Organização preparada" : "Operação pronta para crescer"}</h2></div><span className="font-display text-3xl font-extrabold text-sage-700 dark:text-sage-300">{completionPercent}%</span></div><div className="mt-5 h-2 rounded-full bg-white dark:bg-slate-800"><div className="h-2 rounded-full bg-sage-500 transition-all" style={{ width: `${completionPercent}%` }} /></div></div><div className="divide-y divide-slate-100 dark:divide-slate-800">{steps.map((step, index) => { const Icon = step.icon; const done = completed.has(step.key); const active = next?.key === step.key; return <div key={step.key} className="flex items-center gap-4 p-5 sm:p-6"><div className={done ? "grid h-11 w-11 shrink-0 place-items-center rounded-2xl bg-sage-100 text-sage-700 dark:bg-sage-900 dark:text-sage-200" : "grid h-11 w-11 shrink-0 place-items-center rounded-2xl bg-slate-100 text-slate-400 dark:bg-slate-800"}>{done ? <Check size={20} /> : <Icon size={20} />}</div><div className="min-w-0 flex-1"><p className="font-bold text-ink dark:text-white">{index + 1}. {step.title}</p><p className="mt-1 text-sm text-slate-500">{step.description}</p></div>{done ? <span className="text-xs font-bold text-emerald-600">Concluída</span> : <Button variant={active ? "primary" : "secondary"} onClick={() => complete(step.key)} disabled={!active || update.isPending}>{active ? (update.isPending ? "Salvando…" : "Configurar etapa") : <><Circle size={14} /> Aguardando</>}</Button>}</div>; })}</div><div className="flex flex-wrap justify-end gap-2 border-t border-slate-100 p-5 dark:border-slate-800"><Button variant="secondary" onClick={() => navigate(organizationSlug ? `/app/${organizationSlug}/dashboard` : "/dashboard")}>Voltar ao dashboard</Button>{completed.size === steps.length && !finished && <Button onClick={finish} disabled={update.isPending}><Check size={16} /> Concluir onboarding</Button>}{finished && <Button onClick={() => navigate(organizationSlug ? `/app/${organizationSlug}/billing` : "/billing")}><ArrowRight size={16} /> Ver plano e limites</Button>}</div></Card></div>;
}
