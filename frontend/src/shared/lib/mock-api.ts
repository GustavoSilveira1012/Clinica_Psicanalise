import type {
  Appointment,
  AuditEvent,
  BankTransaction,
  DashboardData,
  FiscalDocument,
  MedicalRecord,
  MedicalRecordRevision,
  NotificationDelivery,
  NotificationPreference,
  PackagePlan,
  Patient,
  Payment,
  PrivacyRequest,
  ProviderSettlement,
  RecordState,
  Receivable,
  SecuritySignal,
  Subscription,
} from "../types/domain";

const today = "2026-09-11T09:00:00-03:00";
const wait = (ms = 120) => new Promise<void>((resolve) => window.setTimeout(resolve, ms));

let appointments: Appointment[] = [
  { id: "apt-1", startAt: "2026-09-11T09:00:00-03:00", durationMinutes: 50, patientId: "pat-1", patientName: "Marina Duarte", professionalName: "Dra. Helena Costa", type: "ONLINE", status: "COMPLETED" },
  { id: "apt-2", startAt: "2026-09-11T11:00:00-03:00", durationMinutes: 50, patientId: "pat-2", patientName: "Rafael Nogueira", professionalName: "Dra. Helena Costa", type: "IN_PERSON", status: "CONFIRMED" },
  { id: "apt-3", startAt: "2026-09-11T14:30:00-03:00", durationMinutes: 50, patientId: "pat-3", patientName: "Camila Sato", professionalName: "Dr. André Lima", type: "ONLINE", status: "PENDING" },
  { id: "apt-4", startAt: "2026-09-11T16:00:00-03:00", durationMinutes: 50, patientId: "pat-4", patientName: "Lucas Almeida", professionalName: "Dra. Helena Costa", type: "IN_PERSON", status: "CONFIRMED" },
  { id: "apt-5", startAt: "2026-09-12T10:00:00-03:00", durationMinutes: 50, patientId: "pat-5", patientName: "Joana Ribeiro", professionalName: "Dr. André Lima", type: "ONLINE", status: "CONFIRMED" },
];

let patients: Patient[] = [
  { id: "pat-1", name: "Marina Duarte", firstName: "Marina", email: "marina.duarte@example.com", phone: "+55 11 99876-2231", status: "ACTIVE", lastAppointment: "2026-09-11T09:00:00-03:00", nextAppointment: "2026-09-18T09:00:00-03:00", tags: ["Acompanhamento", "Online"], consentStatus: "CURRENT" },
  { id: "pat-2", name: "Rafael Nogueira", firstName: "Rafael", email: "rafael.nogueira@example.com", phone: "+55 11 99762-4482", status: "ACTIVE", lastAppointment: "2026-09-04T11:00:00-03:00", nextAppointment: "2026-09-11T11:00:00-03:00", tags: ["Particular"], consentStatus: "CURRENT" },
  { id: "pat-3", name: "Camila Sato", firstName: "Camila", email: "camila.sato@example.com", phone: "+55 11 99610-8834", status: "ACTIVE", lastAppointment: "2026-08-28T14:30:00-03:00", nextAppointment: "2026-09-11T14:30:00-03:00", tags: ["Online", "Novo vínculo"], consentStatus: "REVIEW" },
  { id: "pat-4", name: "Lucas Almeida", firstName: "Lucas", email: "lucas.almeida@example.com", phone: "+55 11 99550-1902", status: "ACTIVE", lastAppointment: "2026-09-04T16:00:00-03:00", nextAppointment: "2026-09-11T16:00:00-03:00", tags: ["Acompanhamento"], consentStatus: "CURRENT" },
  { id: "pat-5", name: "Joana Ribeiro", firstName: "Joana", email: "joana.ribeiro@example.com", phone: "+55 11 99409-3320", status: "INACTIVE", lastAppointment: "2026-06-12T10:00:00-03:00", tags: ["Pausa"], consentStatus: "CURRENT" },
];

const records: MedicalRecord[] = [
  { id: "rec-1", patientId: "pat-1", title: "Acompanhamento clínico", state: "DRAFT", updatedAt: "2026-09-10T17:24:00-03:00", author: "Dra. Helena Costa", contentPreview: "Registro privado disponível para equipe clínica autorizada.", revisionCount: 4, addendumCount: 1 },
  { id: "rec-2", patientId: "pat-2", title: "Acompanhamento clínico", state: "FINALIZED", updatedAt: "2026-09-04T18:02:00-03:00", author: "Dra. Helena Costa", contentPreview: "Registro finalizado. Conteúdo protegido por autorização contextual.", revisionCount: 7, addendumCount: 0 },
  { id: "rec-3", patientId: "pat-3", title: "Acompanhamento clínico", state: "DRAFT", updatedAt: "2026-09-03T16:40:00-03:00", author: "Dr. André Lima", contentPreview: "Registro privado disponível para equipe clínica autorizada.", revisionCount: 2, addendumCount: 0 },
];

const revisions: MedicalRecordRevision[] = [
  { id: "rev-1", label: "Revisão 4", createdAt: "2026-09-10T17:24:00-03:00", author: "Dra. Helena Costa", state: "DRAFT", reason: "Atualização do atendimento" },
  { id: "rev-2", label: "Revisão 3", createdAt: "2026-09-04T18:02:00-03:00", author: "Dra. Helena Costa", state: "FINALIZED", reason: "Finalização do registro" },
  { id: "rev-3", label: "Revisão 2", createdAt: "2026-08-28T18:08:00-03:00", author: "Dra. Helena Costa", state: "FINALIZED", reason: "Atualização do atendimento" },
];

const receivables: Receivable[] = [
  { id: "recb-1", patientName: "Rafael Nogueira", description: "Sessão · 04/09", dueDate: "2026-09-12", amount: 280, status: "OPEN", origin: "APPOINTMENT" },
  { id: "recb-2", patientName: "Camila Sato", description: "Sessão · 28/08", dueDate: "2026-09-05", amount: 280, status: "OVERDUE", origin: "APPOINTMENT" },
  { id: "recb-3", patientName: "Marina Duarte", description: "Pacote Essencial", dueDate: "2026-09-15", amount: 920, status: "OPEN", origin: "PACKAGE" },
  { id: "recb-4", patientName: "Lucas Almeida", description: "Sessão · 04/09", dueDate: "2026-09-04", amount: 280, status: "PAID", origin: "APPOINTMENT" },
  { id: "recb-5", patientName: "Joana Ribeiro", description: "Assinatura mensal", dueDate: "2026-08-25", amount: 680, status: "CANCELLED", origin: "SUBSCRIPTION" },
];

const payments: Payment[] = [
  { id: "pay-1", patientName: "Lucas Almeida", receivedAt: "2026-09-10T10:14:00-03:00", amount: 280, method: "PIX", status: "CONFIRMED" },
  { id: "pay-2", patientName: "Marina Duarte", receivedAt: "2026-09-09T09:21:00-03:00", amount: 920, method: "CARD", status: "CONFIRMED" },
  { id: "pay-3", patientName: "Rafael Nogueira", receivedAt: "2026-09-08T17:55:00-03:00", amount: 280, method: "TRANSFER", status: "PENDING" },
  { id: "pay-4", patientName: "Bruna Teixeira", receivedAt: "2026-09-07T12:03:00-03:00", amount: 280, method: "PIX", status: "REFUNDED" },
];

const bankTransactions: BankTransaction[] = [
  { id: "bank-1", occurredAt: "2026-09-10", description: "PIX recebido · L. Almeida", amount: 280, status: "PENDING" },
  { id: "bank-2", occurredAt: "2026-09-09", description: "Pagamento cartão · M. Duarte", amount: 920, status: "RECONCILED" },
  { id: "bank-3", occurredAt: "2026-09-08", description: "Tarifa bancária", amount: -18.5, status: "IGNORED" },
];

const settlements: ProviderSettlement[] = [
  { id: "set-1", providerName: "Dra. Helena Costa", period: "01–15 set 2026", gross: 6840, fees: 0, net: 6840, status: "READY" },
  { id: "set-2", providerName: "Dr. André Lima", period: "01–15 set 2026", gross: 3920, fees: 0, net: 3920, status: "OPEN" },
];

const fiscalDocuments: FiscalDocument[] = [
  { id: "nf-1", number: "NFS-e 004821", patientName: "Marina Duarte", issuedAt: "2026-09-09", amount: 920, status: "AUTHORIZED" },
  { id: "nf-2", number: "NFS-e 004820", patientName: "Lucas Almeida", issuedAt: "2026-09-08", amount: 280, status: "AUTHORIZED" },
  { id: "nf-3", number: "Lote 241", patientName: "Rafael Nogueira", issuedAt: "2026-09-10", amount: 280, status: "PROCESSING" },
];

const packagePlans: PackagePlan[] = [
  { id: "plan-1", name: "Essencial", sessions: 4, price: 920, activeSubscriptions: 12, status: "ACTIVE" },
  { id: "plan-2", name: "Acompanhamento", sessions: 8, price: 1680, activeSubscriptions: 7, status: "ACTIVE" },
  { id: "plan-3", name: "Intensivo", sessions: 12, price: 2280, activeSubscriptions: 2, status: "DRAFT" },
];

const subscriptions: Subscription[] = [
  { id: "sub-1", patientName: "Marina Duarte", planName: "Essencial", cycle: "Mensal", nextCharge: "2026-10-01", status: "ACTIVE" },
  { id: "sub-2", patientName: "Rafael Nogueira", planName: "Acompanhamento", cycle: "Mensal", nextCharge: "2026-09-18", status: "ACTIVE" },
  { id: "sub-3", patientName: "Joana Ribeiro", planName: "Essencial", cycle: "Mensal", nextCharge: "2026-08-25", status: "PAST_DUE" },
];

const deliveries: NotificationDelivery[] = [
  { id: "not-1", createdAt: "2026-09-11T08:45:00-03:00", recipientLabel: "Marina D•••", channel: "WHATSAPP", eventType: "APPOINTMENT_REMINDER", status: "SENT", providerMessageId: "wamid.…82f" },
  { id: "not-2", createdAt: "2026-09-11T08:02:00-03:00", recipientLabel: "Rafael N•••", channel: "EMAIL", eventType: "RECEIVABLE_DUE", status: "PENDING" },
  { id: "not-3", createdAt: "2026-09-10T18:21:00-03:00", recipientLabel: "Camila S•••", channel: "WHATSAPP", eventType: "APPOINTMENT_REMINDER", status: "FAILED" },
  { id: "not-4", createdAt: "2026-09-10T16:10:00-03:00", recipientLabel: "Joana R•••", channel: "EMAIL", eventType: "PACKAGE_EXPIRING", status: "SUPPRESSED" },
];

let preferences: NotificationPreference[] = [
  { id: "appointment", label: "Confirmação e lembrete de consulta", description: "Mensagens operacionais para reduzir faltas.", enabled: true, required: true },
  { id: "financial", label: "Avisos financeiros", description: "Vencimentos, pagamentos e documentos fiscais.", enabled: true, required: true },
  { id: "package", label: "Uso de pacote e assinatura", description: "Saldo de sessões e ciclos próximos do vencimento.", enabled: true, required: false },
];

const privacyRequests: PrivacyRequest[] = [
  { id: "privacy-1", subjectLabel: "Paciente P••• · acesso", type: "ACCESS", receivedAt: "2026-09-09", status: "IN_PROGRESS" },
  { id: "privacy-2", subjectLabel: "Paciente R••• · correção", type: "CORRECTION", receivedAt: "2026-09-05", status: "OPEN" },
];

const auditEvents: AuditEvent[] = [
  { id: "audit-1", action: "NOTIFICATION_DELIVERY_SENT", actor: "sistema", occurredAt: "2026-09-11T08:45:00-03:00", resource: "delivery not-1", severity: "INFO" },
  { id: "audit-2", action: "NOTIFICATION_PREFERENCE_UPDATED", actor: "Helena C.", occurredAt: "2026-09-10T17:04:00-03:00", resource: "preference package", severity: "INFO" },
  { id: "audit-3", action: "MEDICAL_RECORD_FINALIZED", actor: "Helena C.", occurredAt: "2026-09-10T16:40:00-03:00", resource: "record rec-2", severity: "WARNING" },
  { id: "audit-4", action: "NOTIFICATION_PROVIDER_CONFIGURATION_CHANGED", actor: "Helena C.", occurredAt: "2026-09-08T09:10:00-03:00", resource: "provider whatsapp", severity: "WARNING" },
];

const securitySignals: SecuritySignal[] = [
  { id: "signal-1", type: "NOTIFICATION_DELIVERY_ANOMALY", description: "Aumento pontual de falhas no WhatsApp", detectedAt: "2026-09-10T18:24:00-03:00", status: "OPEN", severity: "MEDIUM" },
  { id: "signal-2", type: "NOTIFICATION_EVENT_COLLISION", description: "Evento duplicado deduplicado pelo idempotency key", detectedAt: "2026-09-07T11:12:00-03:00", status: "REVIEWED", severity: "LOW" },
];

export const mockApi = {
  async getDashboard(): Promise<DashboardData> {
    await wait();
    return {
      stats: [
        { label: "Consultas hoje", value: "08", change: "+12%", trend: "up", icon: "calendar" },
        { label: "Pacientes ativos", value: "148", change: "+8 este mês", trend: "up", icon: "users" },
        { label: "A receber", value: "R$ 8.420", change: "12 pendências", trend: "neutral", icon: "wallet" },
        { label: "Registros para revisar", value: "04", change: "2 prioritários", trend: "down", icon: "clipboard" },
      ],
      upcoming: appointments.slice(0, 4),
      weeklyRevenue: [{ label: "Seg", value: 2800 }, { label: "Ter", value: 4200 }, { label: "Qua", value: 3500 }, { label: "Qui", value: 5100 }, { label: "Sex", value: 4380 }, { label: "Sáb", value: 1900 }],
      openAlerts: 2,
    };
  },
  async getAppointments(): Promise<Appointment[]> { await wait(); return [...appointments]; },
  async createAppointment(input: Omit<Appointment, "id" | "status">): Promise<Appointment> {
    await wait();
    const created = { ...input, id: `apt-${Date.now()}`, status: "PENDING" as const };
    appointments = [...appointments, created];
    return created;
  },
  async getPatients(): Promise<Patient[]> { await wait(); return [...patients]; },
  async createPatient(input: Pick<Patient, "name" | "email" | "phone">): Promise<Patient> {
    await wait();
    const created: Patient = { ...input, id: `pat-${Date.now()}`, firstName: input.name.split(" ")[0], status: "ACTIVE", lastAppointment: today, tags: ["Novo cadastro"], consentStatus: "REVIEW" };
    patients = [created, ...patients];
    return created;
  },
  async getPatient(id: string): Promise<Patient | undefined> { await wait(); return patients.find((patient) => patient.id === id); },
  async getRecords(patientId: string): Promise<MedicalRecord[]> { await wait(); return records.filter((record) => record.patientId === patientId); },
  async getRevisions(_recordId: string): Promise<MedicalRecordRevision[]> { await wait(); return [...revisions]; },
  async saveRecord(recordId: string, contentPreview: string, state: RecordState): Promise<MedicalRecord> {
    await wait();
    const index = records.findIndex((record) => record.id === recordId);
    const current = records[index] ?? records[0];
    const updated = { ...current, contentPreview, state, updatedAt: today, revisionCount: current.revisionCount + 1 };
    if (index >= 0) records[index] = updated;
    return updated;
  },
  async getReceivables(): Promise<Receivable[]> { await wait(); return [...receivables]; },
  async getPayments(): Promise<Payment[]> { await wait(); return [...payments]; },
  async getBankTransactions(): Promise<BankTransaction[]> { await wait(); return [...bankTransactions]; },
  async getSettlements(): Promise<ProviderSettlement[]> { await wait(); return [...settlements]; },
  async getFiscalDocuments(): Promise<FiscalDocument[]> { await wait(); return [...fiscalDocuments]; },
  async getPackagePlans(): Promise<PackagePlan[]> { await wait(); return [...packagePlans]; },
  async getSubscriptions(): Promise<Subscription[]> { await wait(); return [...subscriptions]; },
  async getDeliveries(): Promise<NotificationDelivery[]> { await wait(); return [...deliveries]; },
  async getPreferences(): Promise<NotificationPreference[]> { await wait(); return [...preferences]; },
  async updatePreference(id: string, enabled: boolean): Promise<NotificationPreference> {
    await wait();
    preferences = preferences.map((preference) => preference.id === id && !preference.required ? { ...preference, enabled } : preference);
    return preferences.find((preference) => preference.id === id) ?? preferences[0];
  },
  async getPrivacyRequests(): Promise<PrivacyRequest[]> { await wait(); return [...privacyRequests]; },
  async getAuditEvents(): Promise<AuditEvent[]> { await wait(); return [...auditEvents]; },
  async getSecuritySignals(): Promise<SecuritySignal[]> { await wait(); return [...securitySignals]; },
};

export type DataSource = typeof mockApi;

export const dataSource: DataSource = mockApi;
