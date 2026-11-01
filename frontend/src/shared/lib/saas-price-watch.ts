const FEED_URL = "https://saaspricealerts.com/api/vendors.json";
const MAX_FEED_CHARACTERS = 4_000_000;

export interface SaasPrice {
  amount: number;
  unit: string;
  period: string;
  plan: string | null;
  quote: string;
}

export interface SaasVendor {
  id: string;
  name: string;
  category: string;
  checked: string;
  pricingUrl: string;
  prices: SaasPrice[];
}

export interface SaasPriceFeed {
  updated: string;
  vendors: SaasVendor[];
}

function record(value: unknown): Record<string, unknown> | null {
  return value !== null && typeof value === "object" && !Array.isArray(value)
    ? value as Record<string, unknown>
    : null;
}

function shortText(value: unknown, limit: number): string {
  return typeof value === "string" ? value.trim().slice(0, limit) : "";
}

function date(value: unknown): string {
  const text = shortText(value, 10);
  return /^\d{4}-\d{2}-\d{2}$/.test(text) && !Number.isNaN(Date.parse(`${text}T00:00:00Z`)) ? text : "";
}

function httpsUrl(value: unknown): string {
  if (typeof value !== "string") return "";
  try {
    const url = new URL(value);
    return url.protocol === "https:" ? url.href : "";
  } catch {
    return "";
  }
}

export function parseSaasPriceFeed(value: unknown): SaasPriceFeed {
  const source = record(value);
  if (!source || source.license !== "CC BY 4.0" || !Array.isArray(source.vendors)) {
    throw new Error("O catálogo de preços retornou um formato inesperado.");
  }
  const updated = date(source.updated);
  if (!updated) throw new Error("O catálogo não informou uma data de atualização válida.");

  const vendors: SaasVendor[] = [];
  const seen = new Set<string>();
  for (const item of source.vendors.slice(0, 1000)) {
    const vendor = record(item);
    if (!vendor) continue;
    const id = shortText(vendor.vendor, 160);
    const name = shortText(vendor.name, 160);
    const pricingUrl = httpsUrl(vendor.pricing_url);
    const checked = date(vendor.checked);
    if (!id || !name || !pricingUrl || !checked || seen.has(id)) continue;
    seen.add(id);
    const prices: SaasPrice[] = [];
    if (Array.isArray(vendor.prices)) {
      for (const entry of vendor.prices.slice(0, 30)) {
        const price = record(entry);
        if (!price || price.kind !== "plan" || typeof price.amount !== "number"
            || !Number.isFinite(price.amount) || price.amount <= 0) continue;
        prices.push({
          amount: price.amount,
          unit: shortText(price.unit, 40),
          period: shortText(price.period, 80),
          plan: shortText(price.plan, 80) || null,
          quote: shortText(price.quote, 220),
        });
      }
    }
    if (prices.length === 0) continue;
    vendors.push({ id, name, category: shortText(vendor.category, 80) || "outros",
      checked, pricingUrl, prices });
  }
  return { updated, vendors };
}

export async function loadSaasPriceFeed(signal?: AbortSignal): Promise<SaasPriceFeed> {
  const response = await fetch(FEED_URL, { signal, referrerPolicy: "no-referrer" });
  if (!response.ok) throw new Error("A fonte externa de preços está indisponível.");
  const body = await response.text();
  if (body.length > MAX_FEED_CHARACTERS) throw new Error("O catálogo de preços excedeu o tamanho permitido.");
  return parseSaasPriceFeed(JSON.parse(body) as unknown);
}
