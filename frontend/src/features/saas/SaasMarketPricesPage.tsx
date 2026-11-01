import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Button, Card, EmptyState, PageHeader, Skeleton } from "../../shared/components/ui";
import { loadSaasPriceFeed, type SaasVendor } from "../../shared/lib/saas-price-watch";

function displayDate(value: string): string {
  return new Intl.DateTimeFormat("pt-BR", { timeZone: "UTC" }).format(new Date(`${value}T00:00:00Z`));
}

function VendorCard({ vendor }: { vendor: SaasVendor }) {
  return <Card className="p-5">
    <h2 className="font-display text-lg font-bold text-ink dark:text-white">{vendor.name}</h2>
    <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">{vendor.category} · conferido em {displayDate(vendor.checked)}</p>
    <ul className="mt-4 space-y-3">
      {vendor.prices.slice(0, 5).map((price, index) => <li key={`${price.amount}-${price.unit}-${index}`} className="rounded-xl border border-slate-200 p-3 text-sm dark:border-slate-800">
        <p className="font-semibold text-ink dark:text-white">{price.plan || "Plano sem nome"}: {price.amount.toLocaleString("pt-BR")} {price.period || price.unit}</p>
        {price.quote && <p className="mt-1 text-xs text-slate-600 dark:text-slate-300">Trecho da fonte: “{price.quote}”</p>}
      </li>)}
    </ul>
    <a className="mt-4 inline-block text-sm font-semibold text-harbor-700 underline dark:text-harbor-300" href={vendor.pricingUrl} target="_blank" rel="noopener noreferrer">Conferir preço no fornecedor</a>
  </Card>;
}

export function SaasMarketPricesPage() {
  const [query, setQuery] = useState("");
  const [category, setCategory] = useState("");
  const [selected, setSelected] = useState<string[]>([]);
  const feed = useQuery({ queryKey: ["saas-price-watch"], queryFn: ({ signal }) => loadSaasPriceFeed(signal), staleTime: 15 * 60_000, retry: false });
  const categories = useMemo(() => [...new Set(feed.data?.vendors.map((vendor) => vendor.category) ?? [])].sort(), [feed.data]);
  const filtered = useMemo(() => (feed.data?.vendors ?? []).filter((vendor) =>
    (!category || vendor.category === category) && `${vendor.name} ${vendor.id}`.toLocaleLowerCase("pt-BR").includes(query.trim().toLocaleLowerCase("pt-BR"))
  ).sort((a, b) => a.name.localeCompare(b.name, "pt-BR")), [feed.data, query, category]);
  const compared = (feed.data?.vendors ?? []).filter((vendor) => selected.includes(vendor.id));
  const toggle = (id: string) => setSelected((current) => current.includes(id) ? current.filter((item) => item !== id) : current.length < 3 ? [...current, id] : current);

  return <div>
    <PageHeader eyebrow="Conta SaaS / pesquisa interna" title="Comparar preços de softwares" description="Referências públicas para decisões internas. Não altera os preços ou a cobrança do PsicoGest." />
    <p className="mb-5 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-950 dark:border-amber-900 dark:bg-amber-950/30 dark:text-amber-200">Os valores podem misturar moedas, períodos e preço por usuário. A fonte não informa uma moeda padronizada. Confira a página original antes de comparar ou decidir.</p>
    {feed.isPending && <div aria-label="Carregando preços" aria-busy="true"><Skeleton className="h-20" /><Skeleton className="mt-4 h-60" /></div>}
    {feed.isError && <EmptyState title="Preços externos indisponíveis" description="A fonte pública não respondeu ou bloqueou o acesso. Nenhum dado da clínica foi enviado." action={<Button onClick={() => void feed.refetch()}>Tentar novamente</Button>} />}
    {feed.data && <>
      <div className="mb-5 flex flex-col gap-3 rounded-2xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-900 sm:flex-row sm:items-end">
        <label className="flex-1 text-sm font-semibold">Buscar software<input type="search" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Nome do software" className="mt-2 w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-ink dark:border-slate-700 dark:bg-slate-950 dark:text-white" /></label>
        <label className="text-sm font-semibold">Categoria<select value={category} onChange={(event) => setCategory(event.target.value)} className="mt-2 w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-ink dark:border-slate-700 dark:bg-slate-950 dark:text-white"><option value="">Todas</option>{categories.map((item) => <option key={item} value={item}>{item}</option>)}</select></label>
      </div>
      <p className="mb-3 text-sm text-slate-600 dark:text-slate-300">{filtered.length} {filtered.length === 1 ? "software encontrado" : "softwares encontrados"} · catálogo atualizado em {displayDate(feed.data.updated)}</p>
      {filtered.length === 0 && <p className="mb-4 text-sm text-slate-600 dark:text-slate-300">Nenhum software corresponde à busca. Tente outro nome ou categoria.</p>}
      <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
        {filtered.slice(0, 24).map((vendor) => <button key={vendor.id} type="button" onClick={() => toggle(vendor.id)} aria-pressed={selected.includes(vendor.id)} disabled={!selected.includes(vendor.id) && selected.length >= 3} className="rounded-xl border border-slate-200 bg-white p-3 text-left text-sm transition hover:border-harbor-500 disabled:opacity-50 dark:border-slate-800 dark:bg-slate-900"><span className="font-semibold">{vendor.name}</span><span className="mt-1 block text-xs text-slate-500 dark:text-slate-400">{vendor.category} · {selected.includes(vendor.id) ? "Selecionado" : "Selecionar para comparar"}</span></button>)}
      </div>
      {filtered.length > 24 && <p className="mt-3 text-xs text-slate-500 dark:text-slate-400">Mostrando 24 resultados. Refine a busca para encontrar outros softwares.</p>}
      <section className="mt-8" aria-label="Comparação selecionada"><h2 className="font-display text-xl font-bold text-ink dark:text-white">Comparação selecionada ({compared.length}/3)</h2>{compared.length === 0 ? <p className="mt-3 text-sm text-slate-600 dark:text-slate-300">Selecione até três softwares acima.</p> : <div className="mt-4 grid gap-4 lg:grid-cols-3">{compared.map((vendor) => <VendorCard key={vendor.id} vendor={vendor} />)}</div>}</section>
      <p className="mt-8 text-xs text-slate-500 dark:text-slate-400">Dados: <a className="underline" href="https://saaspricealerts.com/" target="_blank" rel="noopener noreferrer">SaaS Price Watch</a>, licença CC BY 4.0. Consulta direta do navegador à fonte pública, sem dados clínicos.</p>
    </>}
  </div>;
}
