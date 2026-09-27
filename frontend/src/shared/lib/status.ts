// Fonte única de rótulo/tom por status de domínio (Agenda, Prontuário, Financeiro,
// Fiscal, Notificações, Assinaturas, severidade). Consumido pelo <StatusBadge/>.
// Não inventa dados de API: apenas traduz os enums que o backend já envia.

export type StatusTone = "neutral" | "info" | "success" | "warning" | "danger" | "accent";

export interface StatusMeta {
  label: string;
  tone: StatusTone;
  /** Explicação curta (tooltip); opcional. */
  description?: string;
}

const STATUS: Record<string, StatusMeta> = {
  // Agenda / consultas
  SCHEDULED: { label: "Agendada", tone: "info" },
  CONFIRMED: { label: "Confirmada", tone: "success" },
  PENDING: { label: "Pendente", tone: "warning" },
  IN_PROGRESS: { label: "Em andamento", tone: "info" },
  COMPLETED: { label: "Concluída", tone: "success" },
  CANCELLED: { label: "Cancelada", tone: "neutral" },
  CANCELED: { label: "Cancelada", tone: "neutral" },
  NO_SHOW: { label: "Não compareceu", tone: "danger" },
  RESCHEDULED: { label: "Remarcada", tone: "warning" },
  ONLINE: { label: "Online", tone: "info" },
  IN_PERSON: { label: "Presencial", tone: "info" },

  // Pacientes / genéricos
  ACTIVE: { label: "Ativo", tone: "success" },
  INACTIVE: { label: "Inativo", tone: "neutral" },
  ARCHIVED: { label: "Arquivado", tone: "neutral" },
  DISCHARGED: { label: "Alta", tone: "neutral" },

  // Prontuário
  DRAFT: { label: "Rascunho", tone: "warning" },
  REVIEW: { label: "Em revisão", tone: "warning" },
  REVIEWED: { label: "Revisado", tone: "success" },
  FINALIZED: { label: "Finalizado", tone: "success" },
  AMENDED: { label: "Com adendo", tone: "info" },

  // Financeiro (pacientes)
  OPEN: { label: "Em aberto", tone: "warning" },
  PAID: { label: "Pago", tone: "success" },
  PARTIAL: { label: "Parcial", tone: "warning" },
  OVERDUE: { label: "Em atraso", tone: "danger" },
  REFUNDED: { label: "Estornado", tone: "neutral" },
  RECONCILED: { label: "Conciliado", tone: "success" },

  // Fiscal (NFS-e)
  AUTHORIZED: { label: "Autorizada", tone: "success" },
  REJECTED: { label: "Rejeitada", tone: "danger" },
  PROCESSING: { label: "Processando", tone: "warning" },
  ISSUED: { label: "Emitida", tone: "success" },

  // Notificações
  QUEUED: { label: "Na fila", tone: "neutral" },
  SENT: { label: "Enviada", tone: "success" },
  DELIVERED: { label: "Entregue", tone: "success" },
  READ: { label: "Lida", tone: "success" },
  FAILED: { label: "Falhou", tone: "danger" },
  SUPPRESSED: { label: "Suprimida", tone: "neutral" },

  // Assinatura SaaS (billing da conta — separado do financeiro clínico)
  TRIALING: { label: "Em teste", tone: "info" },
  CURRENT: { label: "Em dia", tone: "success" },
  PAST_DUE: { label: "Pagamento pendente", tone: "danger" },
  PAUSED: { label: "Pausada", tone: "warning" },

  // Severidade / prioridade
  CRITICAL: { label: "Crítico", tone: "danger" },
  ERROR: { label: "Erro", tone: "danger" },
  HIGH: { label: "Alta", tone: "danger" },
  WARNING: { label: "Atenção", tone: "warning" },
  MEDIUM: { label: "Média", tone: "warning" },
  INFO: { label: "Informativo", tone: "info" },
  LOW: { label: "Baixa", tone: "neutral" },
};

/** Humaniza um enum desconhecido: "SOME_VALUE" → "Some value". */
function humanize(value: string): string {
  const text = value.trim().toLowerCase().replaceAll("_", " ").replaceAll("-", " ");
  return text.charAt(0).toUpperCase() + text.slice(1);
}

/**
 * Resolve o metadado de um status. Sempre retorna algo renderizável:
 * valores fora do mapa caem em tom neutro com rótulo humanizado (fallback seguro),
 * evitando exibir enums crus como "EXAMPLE" ao usuário. Avisa em dev.
 */
export function statusMeta(value: string | null | undefined): StatusMeta {
  if (!value) return { label: "—", tone: "neutral" };
  const key = value.trim().toUpperCase();
  const found = STATUS[key];
  if (found) return found;
  if (import.meta.env.DEV) {
    console.warn(`[status] sem mapeamento para "${value}" — usando fallback neutro.`);
  }
  return { label: humanize(value), tone: "neutral" };
}

/** Só o rótulo, para onde não cabe um badge. */
export function statusLabel(value: string | null | undefined): string {
  return statusMeta(value).label;
}
