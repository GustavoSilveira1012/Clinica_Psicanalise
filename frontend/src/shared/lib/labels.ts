const codeLabels: Record<string, string> = {
  APPOINTMENT_REMINDER: "Lembrete de consulta",
  RECEIVABLE_DUE: "Vencimento financeiro",
  PACKAGE_EXPIRING: "Pacote próximo do vencimento",
  NOTIFICATION_DELIVERY_SENT: "Notificação enviada",
  NOTIFICATION_PREFERENCE_UPDATED: "Preferência de notificação atualizada",
  NOTIFICATION_PROVIDER_CONFIGURATION_CHANGED: "Configuração de provedor alterada",
  NOTIFICATION_DELIVERY_FAILED: "Notificação com falha",
  NOTIFICATION_DELIVERY_SUPPRESSED: "Notificação suprimida",
  NOTIFICATION_CREATED: "Notificação criada",
  NOTIFICATION_EVENT_COLLISION: "Colisão de evento de notificação",
  NOTIFICATION_PROVIDER_AUTH_FAILURE: "Falha de autenticação do provedor",
  NOTIFICATION_WEBHOOK_SIGNATURE_INVALID: "Assinatura de webhook inválida",
  NOTIFICATION_MASS_SEND_DETECTED: "Envio em massa detectado",
  NOTIFICATION_DELIVERY_ANOMALY: "Anomalia de entrega",
  MEDICAL_RECORD_FINALIZED: "Prontuário finalizado",
};

export function humanizeCode(value: string) {
  return codeLabels[value] ?? value.toLowerCase().replaceAll("_", " ").replace(/^./, (letter) => letter.toUpperCase());
}

export function privacyRequestTypeLabel(value: string) {
  return { ACCESS: "acesso", EXPORT: "exportação", DELETION: "exclusão", CORRECTION: "correção" }[value] ?? humanizeCode(value).toLowerCase();
}

export function channelLabel(value: string) {
  return { WHATSAPP: "WhatsApp", EMAIL: "E-mail", SMS: "SMS", IN_APP: "No aplicativo" }[value] ?? value;
}
