import { useState } from "react";
import { ArrowLeft, ArrowRight, CalendarDays, ClipboardList, ShieldCheck } from "lucide-react";
import { Link } from "react-router-dom";
import { PublicFunnelLayout } from "./PublicFunnelLayout";

const steps = [
  {
    title: "Uma agenda clara para a equipe",
    description: "Veja horários, confirmações e pendências em um único lugar, sem precisar alternar entre planilhas.",
    icon: CalendarDays,
    label: "Agenda fictícia",
    detail: "Hoje · 2 atendimentos de exemplo",
    rows: ["09:00 · Atendimento confirmado", "11:00 · Aguardando confirmação"],
  },
  {
    title: "Contexto do paciente organizado",
    description: "Informações administrativas e clínicas têm permissões distintas. Cada profissional vê apenas o necessário para seu trabalho.",
    icon: ClipboardList,
    label: "Ficha fictícia",
    detail: "Dados de exemplo, sem identificação real",
    rows: ["Cadastro e contatos", "Histórico de atendimentos"],
  },
  {
    title: "Acesso acompanhado de segurança",
    description: "Controle de acesso, registros de auditoria e revisão de prontuário fazem parte do fluxo de trabalho.",
    icon: ShieldCheck,
    label: "Controles demonstrativos",
    detail: "Visão ilustrativa",
    rows: ["Permissões por função", "Ações importantes registradas"],
  },
] as const;

export function AsyncDemoPage() {
  const [step, setStep] = useState(0);
  const current = steps[step];
  const Icon = current.icon;

  return <PublicFunnelLayout>
    <section className="mx-auto grid max-w-6xl gap-8 px-5 pt-10 sm:px-8 lg:grid-cols-[0.9fr_1.1fr] lg:items-center lg:pt-20">
      <div>
        <p className="text-sm font-semibold text-harbor-700 dark:text-harbor-300">Demonstração autoguiada · cerca de 3 minutos</p>
        <h1 className="mt-3 font-display text-4xl font-extrabold tracking-tight sm:text-5xl">Conheça o PsicoGest no seu tempo.</h1>
        <p className="mt-5 text-base leading-7 text-slate-600 dark:text-slate-300">Explore três partes da rotina da clínica. Esta prévia é interativa e usa apenas exemplos fictícios; não cria conta nem grava dados.</p>
        <div className="mt-8 flex gap-2" role="group" aria-label="Etapas da demonstração">
          {steps.map((item, index) => <button key={item.title} type="button" onClick={() => setStep(index)} aria-current={step === index ? "step" : undefined} aria-label={`Etapa ${index + 1}: ${item.title}`} className={`h-2 flex-1 rounded-full ${step === index ? "bg-harbor-600" : "bg-slate-200 dark:bg-slate-700"}`} />)}
        </div>
        <p className="mt-6 text-sm font-semibold text-harbor-700 dark:text-harbor-300">Etapa {step + 1} de {steps.length}</p>
        <h2 className="mt-2 font-display text-2xl font-bold">{current.title}</h2>
        <p className="mt-3 min-h-20 text-sm leading-6 text-slate-600 dark:text-slate-300">{current.description}</p>
        <div className="mt-6 flex flex-wrap gap-3">
          {step > 0 && <button type="button" onClick={() => setStep(step - 1)} className="inline-flex min-h-11 items-center gap-2 rounded-xl border border-slate-300 px-4 text-sm font-semibold dark:border-slate-700"><ArrowLeft size={16} /> Voltar</button>}
          {step < steps.length - 1 ? <button type="button" onClick={() => setStep(step + 1)} className="inline-flex min-h-11 items-center gap-2 rounded-xl bg-harbor-600 px-5 text-sm font-semibold text-white hover:bg-harbor-700">Próxima etapa <ArrowRight size={16} /></button> : <Link to="/planos" className="inline-flex min-h-11 items-center gap-2 rounded-xl bg-harbor-600 px-5 text-sm font-semibold text-white hover:bg-harbor-700">Ver disponibilidade dos planos <ArrowRight size={16} /></Link>}
        </div>
      </div>
      <div aria-live="polite" className="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-xl dark:border-slate-800 dark:bg-slate-900 sm:p-8">
        <span className="inline-flex rounded-2xl bg-harbor-50 p-3 text-harbor-700 dark:bg-harbor-950 dark:text-harbor-300"><Icon size={28} /></span>
        <p className="mt-6 text-xs font-bold uppercase tracking-[0.18em] text-harbor-700 dark:text-harbor-300">{current.label}</p>
        <h3 className="mt-2 font-display text-2xl font-extrabold">Clínica Exemplo</h3>
        <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">{current.detail}</p>
        <div className="mt-7 space-y-3">{current.rows.map(row => <div key={row} className="rounded-2xl border border-slate-200 bg-slate-50 p-4 text-sm font-semibold dark:border-slate-700 dark:bg-slate-800">{row}</div>)}</div>
        <p className="mt-7 text-xs text-slate-500 dark:text-slate-400">Prévia ilustrativa. Acesso clínico real depende de liberação do piloto.</p>
      </div>
    </section>
  </PublicFunnelLayout>;
}
