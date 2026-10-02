import { useRef, useState, type ReactNode } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowDownLeft, ArrowUpRight, Banknote, CircleDollarSign, FileDown, Link2, RefreshCcw, WalletCards } from "lucide-react";
import { dataSource } from "../../shared/lib/app-api";
import { formatCurrency, formatDate } from "../../shared/lib/format";
import type { Receivable } from "../../shared/types/domain";
import { paymentCollectionSchema, receivableSchema, type PaymentCollectionFormValues, type PaymentCollectionValues, type ReceivableFormValues, type ReceivableValues } from "../../shared/lib/validators";
import { Button, Card, EmptyState, Input, Modal, PageHeader, SearchInput, Select, Skeleton, StatusBadge, TabPanel, Tabs } from "../../shared/components/ui";
import { useToast } from "../../shared/providers/ToastProvider";

type Tab = "receivables" | "payments" | "reconciliation" | "settlements" | "refunds-credit";
const tabs: { id: Tab; label: string }[] = [{ id: "receivables", label: "A receber" }, { id: "payments", label: "Pagamentos" }, { id: "refunds-credit", label: "Estornos e crédito" }, { id: "reconciliation", label: "Conciliação bancária" }, { id: "settlements", label: "Repasse de profissionais" }];
function dateInDays(days: number) { const value = new Date(); value.setDate(value.getDate() + days); return value.toISOString().slice(0, 10); }

export function FinancePage() {
  const demoMode = import.meta.env.DEV && import.meta.env.VITE_DEMO_MODE === "true";
  const [tab, setTab] = useState<Tab>("receivables");
  const [query, setQuery] = useState("");
  const [newLaunchOpen, setNewLaunchOpen] = useState(false);
  const [collectionReceivable, setCollectionReceivable] = useState<Receivable | null>(null);
  const queryClient = useQueryClient();
  const toast = useToast();
  const collectionKey = useRef<{ fingerprint: string; value: string } | null>(null);
  const receivables = useQuery({ queryKey: ["receivables"], queryFn: dataSource.getReceivables });
  const payments = useQuery({ queryKey: ["payments"], queryFn: dataSource.getPayments });
  const transactions = useQuery({ queryKey: ["bank-transactions"], queryFn: dataSource.getBankTransactions });
  const settlements = useQuery({ queryKey: ["settlements"], queryFn: dataSource.getSettlements });
  const defaultReceivable = { patientName: "", description: "", dueDate: dateInDays(15), amount: 280, origin: "APPOINTMENT" as const };
  const form = useForm<ReceivableFormValues, unknown, ReceivableValues>({ resolver: zodResolver(receivableSchema), defaultValues: defaultReceivable });
  const collectionForm = useForm<PaymentCollectionFormValues, unknown, PaymentCollectionValues>({ resolver: zodResolver(paymentCollectionSchema), defaultValues: { amount: 0, method: "PIX" } });
  const create = useMutation({ mutationFn: dataSource.createReceivable, onSuccess: () => { queryClient.invalidateQueries({ queryKey: ["receivables"] }); queryClient.invalidateQueries({ queryKey: ["dashboard"] }); setNewLaunchOpen(false); form.reset(defaultReceivable); toast(demoMode ? "Lançamento de demonstração criado." : "Lançamento criado como aberto e registrado na auditoria."); }, onError: (error) => toast(error instanceof Error ? error.message : "Não foi possível criar o lançamento.", "error") });
  const collect = useMutation({ mutationFn: dataSource.collectPayment, onSuccess: () => { void queryClient.invalidateQueries({ queryKey: ["receivables"] }); void queryClient.invalidateQueries({ queryKey: ["payments"] }); void queryClient.invalidateQueries({ queryKey: ["dashboard"] }); setCollectionReceivable(null); collectionForm.reset({ amount: 0, method: "PIX" }); collectionKey.current = null; toast("Pagamento confirmado e alocado à cobrança."); }, onError: (error) => toast(error instanceof Error ? error.message : "Não foi possível registrar o pagamento.", "error") });
  const pending = receivables.isPending || payments.isPending || transactions.isPending || settlements.isPending;
  const filter = (value: string) => value.toLowerCase().includes(query.toLowerCase());

  const receivableItems = receivables.data ?? [];
  const paymentItems = payments.data ?? [];
  const overdueTotal = receivableItems.filter((item) => item.status === "OVERDUE").reduce((total, item) => total + (item.outstandingAmount ?? item.amount), 0);
  const openTotal = receivableItems.filter((item) => item.status === "OPEN" || item.status === "OVERDUE").reduce((total, item) => total + (item.outstandingAmount ?? item.amount), 0);
  const receivedTotal = paymentItems.filter((item) => item.status === "CONFIRMED").reduce((total, item) => total + item.amount, 0);

  function openCollection(receivable: Receivable) {
    if (!receivable.patientId) {
      toast("Não foi possível identificar o paciente desta cobrança.", "error");
      return;
    }
    const outstanding = receivable.outstandingAmount ?? receivable.amount;
    collectionForm.reset({ amount: outstanding, method: "PIX" });
    setCollectionReceivable(receivable);
  }

  function submitCollection(values: PaymentCollectionValues) {
    if (!collectionReceivable?.patientId) return;
    const outstanding = collectionReceivable.outstandingAmount ?? collectionReceivable.amount;
    if (values.amount > outstanding) {
      toast("O valor informado excede o saldo em aberto.", "error");
      return;
    }
    const fingerprint = JSON.stringify([collectionReceivable.id, collectionReceivable.patientId, values.amount, values.method]);
    if (!collectionKey.current || collectionKey.current.fingerprint !== fingerprint) {
      collectionKey.current = { fingerprint, value: crypto.randomUUID() };
    }
    collect.mutate({
      idempotencyKey: collectionKey.current.value,
      patientId: collectionReceivable.patientId,
      patientName: collectionReceivable.patientName,
      receivableId: collectionReceivable.id,
      amount: values.amount,
      method: values.method,
    });
  }

  return <div>
    <PageHeader eyebrow="Financeiro" title="Visão financeira" description="Recebíveis, pagamentos, conciliação e repasses em um só lugar — sem misturar dados clínicos." actions={<><Button variant="secondary" disabled={!demoMode} onClick={() => toast("A exportação segura será liberada quando o endpoint autorizado estiver configurado.")}><FileDown size={16} /> Exportar relatório</Button><Button onClick={() => setNewLaunchOpen(true)}><CircleDollarSign size={16} /> Novo lançamento</Button></>} />
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4"><Metric label="A receber em aberto" value={receivables.isError ? "—" : formatCurrency(openTotal)} change={receivables.isError ? "Indisponível" : `${receivableItems.length} títulos`} icon={<WalletCards size={19} />} /><Metric label="Recebido" value={payments.isError ? "—" : formatCurrency(receivedTotal)} change={payments.isError ? "Indisponível" : `${paymentItems.length} pagamentos`} icon={<ArrowDownLeft size={19} />} /><Metric label="Em atraso" value={receivables.isError ? "—" : formatCurrency(overdueTotal)} change={receivables.isError ? "Indisponível" : `${receivableItems.filter((item) => item.status === "OVERDUE").length} títulos`} icon={<ArrowUpRight size={19} />} /><Metric label="A conciliar" value={transactions.isError ? "—" : String(transactions.data?.length ?? 0).padStart(2, "0")} change={transactions.isError ? "Endpoint não configurado" : "Movimentos pendentes"} icon={<RefreshCcw size={19} />} /></div>
    <Card className="mt-6 overflow-hidden"><div className="flex flex-col gap-4 border-b border-slate-100 p-5 dark:border-slate-800"><div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between"><div><h2 className="font-display text-lg font-bold text-ink dark:text-white">Operação financeira</h2><p className="mt-1 text-sm text-slate-500 dark:text-slate-400">Dados administrativos com trilha de auditoria.</p></div><SearchInput className="sm:w-64" value={query} onChange={setQuery} label="Buscar lançamento" placeholder="Buscar…" /></div><Tabs tabs={tabs} value={tab} onChange={setTab} /></div><TabPanel id={tab} active>{pending ? <div className="space-y-3 p-5">{[1, 2, 3, 4].map((item) => <Skeleton key={item} className="h-14" />)}</div> : <FinanceTable tab={tab} demoMode={demoMode} receivables={receivables.data ?? []} payments={payments.data ?? []} transactions={transactions.data ?? []} settlements={settlements.data ?? []} filter={filter} onCollect={openCollection} />}</TabPanel></Card>
    <div className="mt-6 grid gap-4 lg:grid-cols-2"><Card className="p-5"><div className="flex items-center gap-3"><div className="rounded-xl bg-harbor-50 p-2.5 text-harbor-700 dark:bg-harbor-950/40 dark:text-harbor-300"><Banknote size={19} /></div><div><h3 className="font-bold text-ink dark:text-white">Fluxo de caixa</h3><p className="mt-1 text-sm text-slate-500 dark:text-slate-400">Resumo calculado a partir dos lançamentos disponíveis.</p></div></div><div className="mt-5 h-2 rounded-full bg-slate-100 dark:bg-slate-800"><div className="h-2 w-full rounded-full bg-harbor-500" /></div><div className="mt-2 flex justify-between text-xs text-slate-500 dark:text-slate-400"><span>{receivables.isError ? "Dados indisponíveis" : `${receivableItems.length} recebíveis carregados`}</span><span>{receivables.isError ? "—" : formatCurrency(openTotal)}</span></div></Card><Card className="p-5"><div className="flex items-center gap-3"><div className="rounded-xl bg-blue-50 p-2.5 text-blue-700 dark:bg-blue-950/40 dark:text-blue-300"><Link2 size={19} /></div><div><h3 className="font-bold text-ink dark:text-white">Integrações financeiras</h3><p className="mt-1 text-sm text-slate-500 dark:text-slate-400">Conciliação bancária requer conta e importação configuradas.</p></div></div><Button variant="secondary" className="mt-5 w-full" disabled onClick={() => undefined}>Gerenciar contas bancárias</Button></Card></div>
    <Modal open={newLaunchOpen} onClose={() => setNewLaunchOpen(false)} title="Novo lançamento" description="O lançamento será persistido no backend e ficará disponível para conciliação e auditoria."><form className="space-y-4" onSubmit={form.handleSubmit((values) => create.mutate(values))} noValidate><Input label="Paciente ou responsável" placeholder="Nome exato do paciente cadastrado" error={form.formState.errors.patientName?.message} {...form.register("patientName")} /><Input label="Descrição" placeholder="Ex.: Sessão · setembro" error={form.formState.errors.description?.message} {...form.register("description")} /><div className="grid gap-4 sm:grid-cols-2"><Input label="Vencimento" type="date" error={form.formState.errors.dueDate?.message} {...form.register("dueDate")} /><Input label="Valor" type="number" min="0" step="0.01" error={form.formState.errors.amount?.message} {...form.register("amount")} /></div><Select label="Origem" {...form.register("origin")}><option value="APPOINTMENT">Consulta</option><option value="PACKAGE">Pacote</option><option value="SUBSCRIPTION">Assinatura</option></Select><div className="flex justify-end gap-2 border-t border-slate-100 pt-4 dark:border-slate-800"><Button variant="secondary" type="button" onClick={() => setNewLaunchOpen(false)}>Cancelar</Button><Button type="submit" disabled={create.isPending}>{create.isPending ? "Salvando…" : "Criar lançamento"}</Button></div></form></Modal>
    <Modal open={collectionReceivable !== null} onClose={() => { if (!collect.isPending) setCollectionReceivable(null); }} title="Registrar pagamento" description="O pagamento será confirmado e alocado integralmente a esta cobrança em uma única operação auditada.">
      {collectionReceivable && <form className="space-y-4" onSubmit={collectionForm.handleSubmit(submitCollection)} noValidate>
        <div className="rounded-xl bg-slate-50 p-4 text-sm dark:bg-slate-900"><p className="font-bold text-ink dark:text-white">{collectionReceivable.patientName}</p><p className="mt-1 text-slate-500 dark:text-slate-400">{collectionReceivable.description}</p><p className="mt-2 text-xs text-slate-500 dark:text-slate-400">Saldo em aberto: {formatCurrency(collectionReceivable.outstandingAmount ?? collectionReceivable.amount)}</p></div>
        <div className="grid gap-4 sm:grid-cols-2"><Input label="Valor recebido" type="number" min="0.01" max={collectionReceivable.outstandingAmount ?? collectionReceivable.amount} step="0.01" error={collectionForm.formState.errors.amount?.message} {...collectionForm.register("amount")} /><Select label="Forma de pagamento" error={collectionForm.formState.errors.method?.message} {...collectionForm.register("method")}><option value="PIX">PIX</option><option value="CARD">Cartão</option><option value="TRANSFER">Transferência</option><option value="CASH">Dinheiro</option></Select></div>
        <p className="text-xs text-slate-500 dark:text-slate-400">A confirmação usa uma chave idempotente e valida o paciente, a clínica e os saldos no servidor.</p>
        <div className="flex justify-end gap-2 border-t border-slate-100 pt-4 dark:border-slate-800"><Button variant="secondary" type="button" disabled={collect.isPending} onClick={() => setCollectionReceivable(null)}>Cancelar</Button><Button type="submit" disabled={collect.isPending}>{collect.isPending ? "Registrando…" : "Confirmar pagamento"}</Button></div>
      </form>}
    </Modal>
  </div>;
}

function Metric({ label, value, change, icon }: { label: string; value: string; change: string; icon: ReactNode }) { return <Card className="p-5"><div className="flex items-start justify-between"><span className="rounded-xl bg-harbor-50 p-2.5 text-harbor-600 dark:bg-harbor-950/40 dark:text-harbor-300">{icon}</span><span className="text-xs font-bold text-slate-500 dark:text-slate-400">{change}</span></div><p className="mt-5 text-sm text-slate-500 dark:text-slate-400">{label}</p><p className="mt-1 font-display text-2xl font-extrabold text-ink dark:text-white">{value}</p></Card>; }

function FinanceTable({ tab, demoMode, receivables, payments, transactions, settlements, filter, onCollect }: { tab: Tab; demoMode: boolean; receivables: Awaited<ReturnType<typeof dataSource.getReceivables>>; payments: Awaited<ReturnType<typeof dataSource.getPayments>>; transactions: Awaited<ReturnType<typeof dataSource.getBankTransactions>>; settlements: Awaited<ReturnType<typeof dataSource.getSettlements>>; filter: (value: string) => boolean; onCollect: (receivable: Receivable) => void }) {
  if (tab === "receivables") { const rows = receivables.filter((item) => filter(`${item.patientName} ${item.description}`)); return <div className="divide-y divide-slate-100 dark:divide-slate-800">{rows.map((item) => <div key={item.id} className="grid gap-3 p-5 md:grid-cols-[1.5fr_1fr_150px_120px_auto] md:items-center"><div><p className="text-sm font-bold text-ink dark:text-white">{item.patientName}</p><p className="mt-1 text-xs text-slate-500 dark:text-slate-400">{item.description} · {item.origin === "PACKAGE" ? "Pacote" : item.origin === "SUBSCRIPTION" ? "Assinatura" : "Consulta"}</p></div><div><p className="text-xs text-slate-500 dark:text-slate-400">Vencimento</p><p className="mt-1 text-sm font-semibold text-ink dark:text-white">{formatDate(item.dueDate, { day: "2-digit", month: "short", year: "numeric" })}</p></div><div className="md:text-right"><p className="text-xs text-slate-500 dark:text-slate-400">Saldo</p><p className="mt-1 text-sm font-bold text-ink dark:text-white">{formatCurrency(item.outstandingAmount ?? item.amount)}</p></div><div className="md:text-right"><StatusBadge value={item.status} /></div>{(item.status === "OPEN" || item.status === "OVERDUE") && <Button variant="secondary" className="w-full md:w-auto" disabled={!item.patientId} onClick={() => onCollect(item)}><Banknote size={15} /> Receber</Button>}</div>)}{rows.length === 0 && <div className="p-5"><EmptyState title="Nenhum recebível encontrado" description="Ajuste a busca ou crie um novo lançamento." /></div>}</div>; }
  if (tab === "payments") return <div className="divide-y divide-slate-100 dark:divide-slate-800">{payments.filter((item) => filter(`${item.patientName} ${item.method}`)).map((item) => <div key={item.id} className="grid gap-3 p-5 md:grid-cols-[1.4fr_1fr_150px_120px] md:items-center"><div><p className="text-sm font-bold text-ink dark:text-white">{item.patientName}</p><p className="mt-1 text-xs text-slate-500 dark:text-slate-400">Recebido em {formatDate(item.receivedAt)} · {item.method}</p></div><div className="flex items-center gap-2 text-sm text-slate-500 dark:text-slate-400"><Banknote size={15} /> {item.method === "PIX" ? "PIX" : item.method === "CARD" ? "Cartão" : item.method === "TRANSFER" ? "Transferência" : "Dinheiro"}</div><p className="text-sm font-bold text-ink md:text-right dark:text-white">{formatCurrency(item.amount)}</p><div className="md:text-right"><StatusBadge value={item.status} /></div></div>)}</div>;
  if (tab === "refunds-credit" && !demoMode) return <div className="p-5"><EmptyState title="Estornos e crédito indisponíveis" description="O ambiente real ainda não expõe a consulta consolidada de estornos e saldo credor. Nenhum dado fictício é exibido." /></div>;
  if (tab === "refunds-credit") return <div className="divide-y divide-slate-100 dark:divide-slate-800"><div className="grid gap-3 p-5 md:grid-cols-[1.4fr_1fr_150px_120px] md:items-center"><div><p className="text-sm font-bold text-ink dark:text-white">Bruna Teixeira</p><p className="mt-1 text-xs text-slate-500 dark:text-slate-400">Estorno de pagamento · 07 set 2026</p></div><p className="text-sm text-slate-500 dark:text-slate-400">Motivo: cancelamento</p><p className="text-sm font-bold text-red-600 dark:text-red-400 md:text-right">− R$ 280,00</p><div className="md:text-right"><StatusBadge value="REFUNDED" /></div></div><div className="grid gap-3 p-5 md:grid-cols-[1.4fr_1fr_150px_120px] md:items-center"><div><p className="text-sm font-bold text-ink dark:text-white">Marina Duarte</p><p className="mt-1 text-xs text-slate-500 dark:text-slate-400">Saldo credor · ajuste de pacote</p></div><p className="text-sm text-slate-500 dark:text-slate-400">Disponível para próxima cobrança</p><p className="text-sm font-bold text-success-600 dark:text-emerald-400 md:text-right">+ R$ 120,00</p><div className="md:text-right"><StatusBadge value="OPEN" /></div></div></div>;
  if (tab === "reconciliation") return <div className="divide-y divide-slate-100 dark:divide-slate-800">{transactions.map((item) => <div key={item.id} className="grid gap-3 p-5 md:grid-cols-[1fr_150px_140px_120px] md:items-center"><div><p className="text-sm font-bold text-ink dark:text-white">{item.description}</p><p className="mt-1 text-xs text-slate-500 dark:text-slate-400">Movimento em {formatDate(item.occurredAt)}</p></div><p className={"text-sm font-bold md:text-right " + (item.amount < 0 ? "text-red-600 dark:text-red-400" : "text-ink dark:text-white")}>{formatCurrency(item.amount)}</p><div className="flex items-center gap-2 text-xs text-slate-500 dark:text-slate-400"><span className="h-2 w-2 rounded-full bg-harbor-500" /> Conta principal</div><div className="md:text-right"><StatusBadge value={item.status} /></div></div>)}</div>;
  return <div className="divide-y divide-slate-100 dark:divide-slate-800">{settlements.map((item) => <div key={item.id} className="grid gap-3 p-5 md:grid-cols-[1.3fr_1fr_180px_120px] md:items-center"><div><p className="text-sm font-bold text-ink dark:text-white">{item.providerName}</p><p className="mt-1 text-xs text-slate-500 dark:text-slate-400">{item.period}</p></div><div><p className="text-xs text-slate-500 dark:text-slate-400">Bruto / taxas</p><p className="mt-1 text-sm font-semibold text-ink dark:text-white">{formatCurrency(item.gross)} · {formatCurrency(item.fees)}</p></div><p className="text-sm font-bold text-ink md:text-right dark:text-white">{formatCurrency(item.net)}</p><div className="md:text-right"><StatusBadge value={item.status} /></div></div>)}</div>;
}
