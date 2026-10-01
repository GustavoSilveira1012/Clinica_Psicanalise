import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { apiClient } from "../../shared/lib/api-client";
import { PublicFunnelLayout } from "./PublicFunnelLayout";

export interface PublicPlan {
  code: string;
  name: string;
  description: string | null;
  monthlyPrice: number;
  currency: string;
  features: { code: string; name: string; limitValue: number | null; unit: string | null }[];
}

export interface PublicPlansResponse { available: boolean; plans: PublicPlan[] }

export function PublicPlansPage() {
  const [catalog, setCatalog] = useState<PublicPlansResponse | null>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    let active = true;
    apiClient.request<PublicPlansResponse>("/public/plans", { skipAuthRefresh: true })
      .then(result => { if (active) setCatalog(result); })
      .catch(() => { if (active) setFailed(true); });
    return () => { active = false; };
  }, []);

  return <PublicFunnelLayout>
    <section className="mx-auto max-w-6xl px-5 pt-10 sm:px-8 lg:pt-20">
      <p className="text-sm font-semibold text-harbor-700 dark:text-harbor-300">Planos do PsicoGest</p>
      <h1 className="mt-3 max-w-3xl font-display text-4xl font-extrabold tracking-tight sm:text-5xl">Escolha com clareza, quando os planos estiverem disponíveis.</h1>
      <p className="mt-5 max-w-2xl text-base leading-7 text-slate-600 dark:text-slate-300">Estamos preparando a abertura comercial. A demonstração autoguiada já pode ser explorada sem cadastro e sem cobrança.</p>
      <div className="mt-9" aria-live="polite">
        {!catalog && !failed && <p role="status" className="rounded-2xl border border-slate-200 bg-white p-6 dark:border-slate-800 dark:bg-slate-900">Consultando disponibilidade dos planos…</p>}
        {failed && <p role="status" className="rounded-2xl border border-amber-200 bg-amber-50 p-6 text-amber-900 dark:border-amber-900 dark:bg-amber-950/30 dark:text-amber-200">Não foi possível consultar os planos agora. Nenhum preço ou contratação está disponível nesta página.</p>}
        {catalog && (!catalog.available || catalog.plans.length === 0) && <p role="status" className="rounded-2xl border border-harbor-200 bg-harbor-50 p-6 text-harbor-900 dark:border-harbor-900 dark:bg-harbor-950/30 dark:text-harbor-200">Os planos e preços ainda não foram publicados. O teste gratuito e a contratação online também não estão abertos.</p>}
        {catalog?.available && catalog.plans.length > 0 && <div className="grid gap-5 md:grid-cols-2 lg:grid-cols-3">{catalog.plans.map(plan => <article key={plan.code} className="rounded-2xl border border-slate-200 bg-white p-6 dark:border-slate-800 dark:bg-slate-900"><h2 className="font-display text-2xl font-bold">{plan.name}</h2><p className="mt-2 min-h-12 text-sm text-slate-600 dark:text-slate-300">{plan.description}</p><p className="mt-6 font-display text-3xl font-extrabold">{new Intl.NumberFormat("pt-BR", { style: "currency", currency: plan.currency }).format(plan.monthlyPrice)}<span className="ml-1 text-sm font-normal">/mês</span></p><ul className="mt-6 space-y-2 text-sm">{plan.features.map(feature => <li key={feature.code}>{feature.name}{feature.limitValue != null ? ` · ${feature.limitValue}${feature.unit ? ` ${feature.unit}` : ""}` : ""}</li>)}</ul><p className="mt-7 text-xs text-slate-500 dark:text-slate-400">Contratação online indisponível no momento.</p></article>)}</div>}
      </div>
      <div className="mt-9 flex flex-wrap gap-3"><Link to="/demo" className="inline-flex min-h-11 items-center rounded-xl bg-harbor-600 px-5 text-sm font-semibold text-white hover:bg-harbor-700">Ver demonstração</Link><Link to="/faq" className="inline-flex min-h-11 items-center rounded-xl border border-slate-300 px-5 text-sm font-semibold dark:border-slate-700">Tirar dúvidas</Link></div>
    </section>
  </PublicFunnelLayout>;
}
