import { Check, CreditCard, Download, LockKeyhole, Receipt, ShieldCheck } from "lucide-react";
import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useNavigate, useParams } from "react-router-dom";
import { Badge, Button, Card, EmptyState, Notice, PageHeader, Skeleton, StatusBadge } from "../../shared/components/ui";
import { formatCurrency, formatDate } from "../../shared/lib/format";
import { getSaasBilling } from "../../shared/lib/saas-api";
import { useAuth } from "../auth/AuthContext";

const demoPlans = [
  { name: "Start", price: "R$ 0", description: "Para começar com o essencial", features: ["Até 100 pacientes", "2 membros da equipe", "Agenda e prontuário protegidos"] },
  { name: "Growth", price: "R$ 249/mês", description: "Para clínicas em crescimento", features: ["Até 500 pacientes", "10 membros da equipe", "WhatsApp e relatórios avançados"] },
  { name: "Scale", price: "R$ 599/mês", description: "Para redes e operações maiores", features: ["Até 5.000 pacientes", "50 membros da equipe", "Governança e suporte auditado"] },
];

const demoBilling = {
  organizationId: "demo",
  planCode: "GROWTH",
  planName: "Growth",
  status: "TRIALING" as const,
  monthlyPrice: 249,
  trialEndsAt: "2026-09-30T23:59:59-03:00",
  currentPeriodEnd: "2026-10-30",
  invoices: [],
};

export function BillingPage() {
  const { organizationSlug } = useParams();
  const { session } = useAuth();
  const navigate = useNavigate();
  const demoMode = import.meta.env.DEV && import.meta.env.VITE_DEMO_MODE === "true";
  const organizationId = session?.user.tenant.id;
  const [notice, setNotice] = useState<string | null>(null);
  const billing = useQuery({
    queryKey: ["saas-billing", organizationId],
    queryFn: () => getSaasBilling(organizationId ?? ""),
    enabled: !demoMode && Boolean(organizationId && organizationId !== "unselected"),
  });
  const summary = demoMode ? demoBilling : billing.data;
  const planName = summary?.planName ?? "Plano não carregado";
  const trialLabel = summary?.trialEndsAt ? formatDate(summary.trialEndsAt, { day: "2-digit", month: "long", year: "numeric" }) : "—";

  if (!demoMode && billing.isPending) return <><Skeleton className="h-10 w-72" /><Skeleton className="mt-6 h-64" /><Skeleton className="mt-6 h-52" /></>;
  if (!demoMode && billing.isError) return <div><PageHeader eyebrow="Conta SaaS" title="Plano e cobrança" description="Assinatura e faturas da plataforma, separadas do financeiro da clínica." /><EmptyState title="Cobrança SaaS indisponível" description="Não foi possível consultar a assinatura desta organização. Nenhuma troca de plano ou cobrança foi iniciada." action={<Button onClick={() => billing.refetch()}>Tentar novamente</Button>} /></div>;
  if (!summary) return <EmptyState title="Organização não selecionada" description="Selecione uma organização ativa para consultar o plano e os limites." />;

  return <div>
    <PageHeader eyebrow="Conta SaaS" title="Plano e cobrança" description="Assinatura, limites e faturas do PsicoGest. Este espaço é separado do financeiro da clínica." actions={<Button variant="secondary" onClick={() => navigate(organizationSlug ? `/app/${organizationSlug}/settings` : "/settings")}><LockKeyhole size={16} /> Gerenciar acesso</Button>} />
    {demoMode && <div className="mb-6"><Notice title="Ambiente de demonstração">Os valores desta tela são exemplos locais. O ambiente real consulta a assinatura da organização e não cria cobranças sem um provedor configurado.</Notice></div>}
    {notice && <div className="mb-6"><Notice title="Ação não executada" tone="warning">{notice}</Notice></div>}
    <div className="grid gap-4 lg:grid-cols-[1.2fr_0.8fr]">
      <Card className="overflow-hidden"><div className="flex items-start justify-between gap-4 border-b border-slate-100 p-6 dark:border-slate-800"><div><p className="text-xs font-bold uppercase tracking-[0.18em] text-sage-600">Assinatura atual</p><h2 className="mt-2 font-display text-2xl font-extrabold text-ink dark:text-white">Plano {planName}</h2><p className="mt-1 text-sm text-slate-500">{formatCurrency(Number(summary.monthlyPrice))}/mês · cobrança SaaS separada dos recebíveis dos pacientes</p></div><StatusBadge value={summary.status} /></div><div className="grid gap-4 p-6 sm:grid-cols-3"><div><p className="text-xs text-slate-400">Status</p><p className="mt-1 font-bold text-ink dark:text-white">{summary.status === "TRIALING" ? "Período de teste" : "Assinatura ativa"}</p></div><div><p className="text-xs text-slate-400">Teste até</p><p className="mt-1 font-bold text-ink dark:text-white">{trialLabel}</p></div><div><p className="text-xs text-slate-400">Próximo período</p><p className="mt-1 font-bold text-ink dark:text-white">{summary.currentPeriodEnd ? formatDate(summary.currentPeriodEnd) : "—"}</p></div></div><div className="flex flex-wrap gap-2 border-t border-slate-100 p-6 dark:border-slate-800"><Button disabled onClick={() => setNotice("A troca de plano exige um billing provider configurado no backend.")}>Alterar plano</Button><Button variant="secondary" disabled onClick={() => setNotice("O cancelamento exige um billing provider configurado no backend.")}>Cancelar ao fim do período</Button></div></Card>
      <Card className="p-6"><div className="flex items-center gap-3"><span className="rounded-xl bg-sage-50 p-2.5 text-sage-700 dark:bg-sage-950/40 dark:text-sage-300"><CreditCard size={18} /></span><div><h2 className="font-display text-lg font-bold text-ink dark:text-white">Método de pagamento</h2><p className="mt-1 text-xs text-slate-500">Nenhum dado completo é armazenado no PsicoGest.</p></div></div><div className="mt-6"><EmptyState title="Portal de pagamento não conectado" description="Configure o provedor de billing para adicionar ou atualizar o método com segurança." /></div><Button variant="secondary" className="mt-4 w-full" disabled onClick={() => setNotice("O portal seguro será aberto quando o provedor de billing estiver configurado.")}><ShieldCheck size={16} /> Abrir portal seguro</Button></Card>
    </div>
    <Card className="mt-6 p-6"><div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between"><div><p className="text-xs font-bold uppercase tracking-[0.18em] text-sage-600">Limites</p><h2 className="mt-1 font-display text-xl font-bold text-ink dark:text-white">Capacidade da organização</h2></div><p className="text-xs text-slate-500">Downgrade bloqueia novas criações, nunca apaga histórico.</p></div><div className="mt-6"><EmptyState title="Uso detalhado ainda não exposto" description="Os limites comerciais estão registrados no backend. A medição por recurso será exibida assim que o endpoint de consumo estiver disponível." /></div></Card>
    {demoMode && <section className="mt-6"><div className="mb-4"><p className="text-xs font-bold uppercase tracking-[0.18em] text-sage-600">Catálogo demonstrativo</p><h2 className="mt-1 font-display text-xl font-bold text-ink dark:text-white">Planos disponíveis no produto</h2></div><div className="grid gap-4 lg:grid-cols-3">{demoPlans.map((plan) => <Card key={plan.name} className={plan.name === planName ? "border-sage-300 ring-2 ring-sage-100 dark:border-sage-700 dark:ring-sage-950" : ""}><div className="p-5"><div className="flex items-center justify-between"><h3 className="font-display text-lg font-bold text-ink dark:text-white">{plan.name}</h3>{plan.name === planName && <Badge tone="green">Atual</Badge>}</div><p className="mt-3 font-display text-2xl font-extrabold text-ink dark:text-white">{plan.price}</p><p className="mt-1 text-sm text-slate-500">{plan.description}</p><ul className="mt-5 space-y-3">{plan.features.map((feature) => <li key={feature} className="flex items-start gap-2 text-sm text-slate-600 dark:text-slate-300"><Check className="mt-0.5 shrink-0 text-sage-600" size={15} />{feature}</li>)}</ul><Button variant={plan.name === planName ? "secondary" : "primary"} className="mt-6 w-full" onClick={() => setNotice(plan.name === planName ? "Este já é o plano ativo no cenário demonstrativo." : `No ambiente real, a solicitação do plano ${plan.name} dependerá do billing provider configurado.`)}>{plan.name === planName ? "Plano atual" : "Ver plano"}</Button></div></Card>)}</div></section>}
    <Card className="mt-6 overflow-hidden"><div className="flex items-center justify-between border-b border-slate-100 p-5 dark:border-slate-800"><div><h2 className="font-display text-lg font-bold text-ink dark:text-white">Faturas SaaS</h2><p className="mt-1 text-sm text-slate-500">Cobranças da plataforma, sem misturar com recebíveis de pacientes.</p></div><Receipt className="text-slate-400" size={20} /></div>{summary.invoices.length === 0 ? <div className="p-5"><EmptyState title="Nenhuma fatura emitida" description={demoMode ? "O período de teste do cenário demonstrativo ainda está ativo." : "As faturas aparecerão aqui quando o billing provider emitir uma cobrança."} /></div> : <div className="divide-y divide-slate-100 dark:divide-slate-800">{summary.invoices.map((invoice) => <div key={invoice.id} className="grid gap-3 p-5 text-sm sm:grid-cols-[1fr_120px_120px_40px] sm:items-center"><div><p className="font-bold text-ink dark:text-white">{invoice.id}</p><p className="mt-1 text-xs text-slate-500">Vencimento em {formatDate(invoice.dueAt)}</p></div><StatusBadge value={invoice.status} /><p className="font-bold text-ink dark:text-white">{formatCurrency(Number(invoice.amount))}</p>{invoice.hostedInvoiceUrl ? <a href={invoice.hostedInvoiceUrl} target="_blank" rel="noreferrer" aria-label="Abrir fatura" className="text-sage-700"><Download size={17} /></a> : <span className="text-slate-300"><Download size={17} /></span>}</div>)}</div>}</Card>
  </div>;
}
