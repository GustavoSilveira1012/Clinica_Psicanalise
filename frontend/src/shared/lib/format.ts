export const currency = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
});

export function formatCurrency(value: number) {
  return currency.format(value);
}

export function formatDate(value: string, options?: Intl.DateTimeFormatOptions) {
  const date = /^\d{4}-\d{2}-\d{2}$/.test(value) ? new Date(`${value}T12:00:00`) : new Date(value);
  return new Intl.DateTimeFormat("pt-BR", options ?? { day: "2-digit", month: "short" }).format(date);
}

export function formatDateTime(value: string) {
  return new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit",
    month: "short",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

export function maskContact(value: string) {
  if (value.includes("@")) {
    const [local, domain] = value.split("@");
    return `${local.slice(0, 2)}•••@${domain}`;
  }
  return `${value.slice(0, 4)}••••${value.slice(-4)}`;
}

export function initials(name: string) {
  return name
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0])
    .join("")
    .toUpperCase();
}
