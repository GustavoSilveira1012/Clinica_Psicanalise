import type {
  Appointment,
  CalendarBlock,
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

const DEMO_STORAGE_KEY = "psicogest-demo-state-v1";
const SEED_DATE = "2026-09-11";

function saoPauloDate(date = new Date()) {
  const parts = new Intl.DateTimeFormat("en-US", { timeZone: "America/Sao_Paulo", year: "numeric", month: "2-digit", day: "2-digit" }).formatToParts(date);
  const value = (type: string) => parts.find((part) => part.type === type)?.value ?? "";
  return value("year") + "-" + value("month") + "-" + value("day");
}

const currentDemoDate = saoPauloDate();
const today = currentDemoDate + "T09:00:00-03:00";
const seedDay = new Date(SEED_DATE + "T00:00:00-03:00").getTime();
const currentDay = new Date(currentDemoDate + "T00:00:00-03:00").getTime();
const seedOffsetMs = currentDay - seedDay;
const wait = (ms = 120) => new Promise<void>((resolve) => window.setTimeout(resolve, ms));

function shiftSeedTimestamp(value: string) {
  return new Date(new Date(value).getTime() + seedOffsetMs).toISOString();
}

function shiftSeedDate(value: string) {
  return shiftSeedTimestamp(value + "T12:00:00-03:00").slice(0, 10);
}

let appointments: Appointment[] = [
  { id: "apt-1", startAt: "2026-09-11T09:00:00-03:00", durationMinutes: 50, patientId: "pat-1", patientName: "Marina Duarte", professionalName: "Dra. Helena Costa", type: "ONLINE", status: "COMPLETED" },
  { id: "apt-2", startAt: "2026-09-11T11:00:00-03:00", durationMinutes: 50, patientId: "pat-2", patientName: "Rafael Nogueira", professionalName: "Dra. Helena Costa", type: "IN_PERSON", status: "CONFIRMED" },
  { id: "apt-3", startAt: "2026-09-11T14:30:00-03:00", durationMinutes: 50, patientId: "pat-3", patientName: "Camila Sato", professionalName: "Dr. André Lima", type: "ONLINE", status: "PENDING" },
  { id: "apt-4", startAt: "2026-09-11T16:00:00-03:00", durationMinutes: 50, patientId: "pat-4", patientName: "Lucas Almeida", professionalName: "Dra. Helena Costa", type: "IN_PERSON", status: "CONFIRMED" },
  { id: "apt-5", startAt: "2026-09-12T10:00:00-03:00", durationMinutes: 50, patientId: "pat-5", patientName: "Joana Ribeiro", professionalName: "Dr. André Lima", type: "ONLINE", status: "CONFIRMED" },
];

let calendarBlocks: CalendarBlock[] = [
  { id: "block-1", startAt: "2026-09-11T12:00:00-03:00", durationMinutes: 60, professionalName: "Dra. Helena Costa", reason: "Intervalo da equipe" },
];

let patients: Patient[] = [
  { id: "pat-1", name: "Marina Duarte", firstName: "Marina", email: "marina.duarte@example.com", phone: "+55 11 99876-2231", status: "ACTIVE", lastAppointment: "2026-09-11T09:00:00-03:00", nextAppointment: "2026-09-18T09:00:00-03:00", tags: ["Acompanhamento", "Online"], consentStatus: "CURRENT" },
  { id: "pat-2", name: "Rafael Nogueira", firstName: "Rafael", email: "rafael.nogueira@example.com", phone: "+55 11 99762-4482", status: "ACTIVE", lastAppointment: "2026-09-04T11:00:00-03:00", nextAppointment: "2026-09-11T11:00:00-03:00", tags: ["Particular"], consentStatus: "CURRENT" },
  { id: "pat-3", name: "Camila Sato", firstName: "Camila", email: "camila.sato@example.com", phone: "+55 11 99610-8834", status: "ACTIVE", lastAppointment: "2026-08-28T14:30:00-03:00", nextAppointment: "2026-09-11T14:30:00-03:00", tags: ["Online", "Novo vínculo"], consentStatus: "REVIEW" },
  { id: "pat-4", name: "Lucas Almeida", firstName: "Lucas", email: "lucas.almeida@example.com", phone: "+55 11 99550-1902", status: "ACTIVE", lastAppointment: "2026-09-04T16:00:00-03:00", nextAppointment: "2026-09-11T16:00:00-03:00", tags: ["Acompanhamento"], consentStatus: "CURRENT" },
  { id: "pat-5", name: "Joana Ribeiro", firstName: "Joana", email: "joana.ribeiro@example.com", phone: "+55 11 99409-3320", status: "INACTIVE", lastAppointment: "2026-06-12T10:00:00-03:00", tags: ["Pausa"], consentStatus: "CURRENT" },
];

let records: MedicalRecord[] = [
  { id: "rec-1", patientId: "pat-1", title: "Acompanhamento clínico", state: "DRAFT", updatedAt: "2026-09-10T17:24:00-03:00", author: "Dra. Helena Costa", contentPreview: "Registro privado disponível para equipe clínica autorizada.", revisionCount: 4, addendumCount: 1 },
  { id: "rec-2", patientId: "pat-2", title: "Acompanhamento clínico", state: "FINALIZED", updatedAt: "2026-09-04T18:02:00-03:00", author: "Dra. Helena Costa", contentPreview: "Registro finalizado. Conteúdo protegido por autorização contextual.", revisionCount: 7, addendumCount: 0 },
  { id: "rec-3", patientId: "pat-3", title: "Acompanhamento clínico", state: "DRAFT", updatedAt: "2026-09-03T16:40:00-03:00", author: "Dr. André Lima", contentPreview: "Registro privado disponível para equipe clínica autorizada.", revisionCount: 2, addendumCount: 0 },
];

let revisions: MedicalRecordRevision[] = [
  { id: "rev-1", label: "Revisão 4", createdAt: "2026-09-10T17:24:00-03:00", author: "Dra. Helena Costa", state: "DRAFT", reason: "Atualização do atendimento" },
  { id: "rev-2", label: "Revisão 3", createdAt: "2026-09-04T18:02:00-03:00", author: "Dra. Helena Costa", state: "FINALIZED", reason: "Finalização do registro" },
  { id: "rev-3", label: "Revisão 2", createdAt: "2026-08-28T18:08:00-03:00", author: "Dra. Helena Costa", state: "FINALIZED", reason: "Atualização do atendimento" },
];

let receivables: Receivable[] = [
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

let fiscalDocuments: FiscalDocument[] = [
  { id: "nf-1", number: "NFS-e 004821", patientName: "Marina Duarte", issuedAt: "2026-09-09", amount: 920, status: "AUTHORIZED" },
  { id: "nf-2", number: "NFS-e 004820", patientName: "Lucas Almeida", issuedAt: "2026-09-08", amount: 280, status: "AUTHORIZED" },
  { id: "nf-3", number: "Lote 241", patientName: "Rafael Nogueira", issuedAt: "2026-09-10", amount: 280, status: "PROCESSING" },
];

let packagePlans: PackagePlan[] = [
  { id: "plan-1", name: "Essencial", sessions: 4, price: 920, activeSubscriptions: 12, status: "ACTIVE" },
  { id: "plan-2", name: "Acompanhamento", sessions: 8, price: 1680, activeSubscriptions: 7, status: "ACTIVE" },
  { id: "plan-3", name: "Intensivo", sessions: 12, price: 2280, activeSubscriptions: 2, status: "DRAFT" },
];

let subscriptions: Subscription[] = [
  { id: "sub-1", patientName: "Marina Duarte", planName: "Essencial", cycle: "Mensal", nextCharge: "2026-10-01", status: "ACTIVE" },
  { id: "sub-2", patientName: "Rafael Nogueira", planName: "Acompanhamento", cycle: "Mensal", nextCharge: "2026-09-18", status: "ACTIVE" },
  { id: "sub-3", patientName: "Joana Ribeiro", planName: "Essencial", cycle: "Mensal", nextCharge: "2026-08-25", status: "PAST_DUE" },
];

let deliveries: NotificationDelivery[] = [
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

interface DemoState {
  appointments: Appointment[];
  calendarBlocks: CalendarBlock[];
  patients: Patient[];
  records: MedicalRecord[];
  revisions: MedicalRecordRevision[];
  receivables: Receivable[];
  fiscalDocuments: FiscalDocument[];
  packagePlans: PackagePlan[];
  subscriptions: Subscription[];
  deliveries: NotificationDelivery[];
  preferences: NotificationPreference[];
}

function clone<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T;
}

function shiftSeedData() {
  if (seedOffsetMs === 0) return;
  appointments = appointments.map((item) => ({ ...item, startAt: shiftSeedTimestamp(item.startAt) }));
  calendarBlocks = calendarBlocks.map((item) => ({ ...item, startAt: shiftSeedTimestamp(item.startAt) }));
  patients = patients.map((item) => ({ ...item, lastAppointment: shiftSeedTimestamp(item.lastAppointment), nextAppointment: item.nextAppointment ? shiftSeedTimestamp(item.nextAppointment) : undefined }));
  records = records.map((item) => ({ ...item, updatedAt: shiftSeedTimestamp(item.updatedAt) }));
  revisions = revisions.map((item) => ({ ...item, createdAt: shiftSeedTimestamp(item.createdAt) }));
  receivables = receivables.map((item) => ({ ...item, dueDate: shiftSeedDate(item.dueDate) }));
  fiscalDocuments = fiscalDocuments.map((item) => ({ ...item, issuedAt: shiftSeedDate(item.issuedAt) }));
  packagePlans = packagePlans.map((item) => ({ ...item }));
  subscriptions = subscriptions.map((item) => ({ ...item, nextCharge: shiftSeedDate(item.nextCharge) }));
  deliveries = deliveries.map((item) => ({ ...item, createdAt: shiftSeedTimestamp(item.createdAt) }));
}

shiftSeedData();

const initialDemoState: DemoState = {
  appointments: clone(appointments),
  calendarBlocks: clone(calendarBlocks),
  patients: clone(patients),
  records: clone(records),
  revisions: clone(revisions),
  receivables: clone(receivables),
  fiscalDocuments: clone(fiscalDocuments),
  packagePlans: clone(packagePlans),
  subscriptions: clone(subscriptions),
  deliveries: clone(deliveries),
  preferences: clone(preferences),
};

function applyDemoState(state: DemoState) {
  appointments = state.appointments;
  calendarBlocks = state.calendarBlocks;
  patients = state.patients;
  records = state.records;
  revisions = state.revisions;
  receivables = state.receivables;
  fiscalDocuments = state.fiscalDocuments;
  packagePlans = state.packagePlans;
  subscriptions = state.subscriptions;
  deliveries = state.deliveries;
  preferences = state.preferences;
}

function persistDemoState() {
  if (typeof window === "undefined") return;
  try {
    window.localStorage.setItem(DEMO_STORAGE_KEY, JSON.stringify({
      appointments,
      calendarBlocks,
      patients,
      records,
      revisions,
      receivables,
      fiscalDocuments,
      packagePlans,
      subscriptions,
      deliveries,
      preferences,
    } satisfies DemoState));
  } catch {
    // A private browsing context may deny localStorage. In-memory demo still works.
  }
}

function hydrateDemoState() {
  if (typeof window === "undefined") return;
  const raw = window.localStorage.getItem(DEMO_STORAGE_KEY);
  if (!raw) return;
  try {
    const stored = JSON.parse(raw) as Partial<DemoState>;
    applyDemoState({
      appointments: Array.isArray(stored.appointments) ? stored.appointments : initialDemoState.appointments,
      calendarBlocks: Array.isArray(stored.calendarBlocks) ? stored.calendarBlocks : initialDemoState.calendarBlocks,
      patients: Array.isArray(stored.patients) ? stored.patients : initialDemoState.patients,
      records: Array.isArray(stored.records) ? stored.records : initialDemoState.records,
      revisions: Array.isArray(stored.revisions) ? stored.revisions : initialDemoState.revisions,
      receivables: Array.isArray(stored.receivables) ? stored.receivables : initialDemoState.receivables,
      fiscalDocuments: Array.isArray(stored.fiscalDocuments) ? stored.fiscalDocuments : initialDemoState.fiscalDocuments,
      packagePlans: Array.isArray(stored.packagePlans) ? stored.packagePlans : initialDemoState.packagePlans,
      subscriptions: Array.isArray(stored.subscriptions) ? stored.subscriptions : initialDemoState.subscriptions,
      deliveries: Array.isArray(stored.deliveries) ? stored.deliveries : initialDemoState.deliveries,
      preferences: Array.isArray(stored.preferences) ? stored.preferences : initialDemoState.preferences,
    });
  } catch {
    window.localStorage.removeItem(DEMO_STORAGE_KEY);
  }
}

hydrateDemoState();

export const mockApi = {
  async getDashboard(): Promise<DashboardData> {
    await wait();
    const appointmentsToday = appointments.filter((appointment) => appointment.startAt.slice(0, 10) === today.slice(0, 10) && appointment.status !== "CANCELLED").length;
    const activePatientCount = patients.filter((patient) => patient.status === "ACTIVE").length;
    const openReceivableAmount = receivables.filter((item) => item.status === "OPEN" || item.status === "OVERDUE").reduce((total, item) => total + item.amount, 0);
    const reviewRecordCount = records.filter((record) => record.state === "DRAFT").length;
    const upcoming = [...appointments].filter((appointment) => appointment.status !== "CANCELLED").sort((a, b) => new Date(a.startAt).getTime() - new Date(b.startAt).getTime()).slice(0, 4);
    return {
      stats: [
        { label: "Consultas hoje", value: String(appointmentsToday).padStart(2, "0"), change: "+12%", trend: "up", icon: "calendar" },
        { label: "Pacientes ativos", value: String(activePatientCount), change: "+8 este mês", trend: "up", icon: "users" },
        { label: "A receber", value: new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL", maximumFractionDigits: 0 }).format(openReceivableAmount), change: String(receivables.filter((item) => item.status === "OPEN" || item.status === "OVERDUE").length) + " pendências", trend: "neutral", icon: "wallet" },
        { label: "Registros para revisar", value: String(reviewRecordCount).padStart(2, "0"), change: "Acesso clínico", trend: "down", icon: "clipboard" },
      ],
      upcoming,
      weeklyRevenue: [{ label: "Seg", value: 2800 }, { label: "Ter", value: 4200 }, { label: "Qua", value: 3500 }, { label: "Qui", value: 5100 }, { label: "Sex", value: 4380 }, { label: "Sáb", value: 1900 }],
      openAlerts: 2,
    };
  },
  async getAppointments(): Promise<Appointment[]> { await wait(); return [...appointments]; },
  async createAppointment(input: Omit<Appointment, "id" | "status">): Promise<Appointment> {
    await wait();
    const start = new Date(input.startAt).getTime();
    const end = start + input.durationMinutes * 60_000;
    const hasAppointmentConflict = appointments.some((appointment) => {
      if (appointment.professionalName !== input.professionalName || appointment.status === "CANCELLED") return false;
      const appointmentStart = new Date(appointment.startAt).getTime();
      const appointmentEnd = appointmentStart + appointment.durationMinutes * 60_000;
      return start < appointmentEnd && end > appointmentStart;
    });
    const hasBlockConflict = calendarBlocks.some((block) => {
      if (block.professionalName !== input.professionalName) return false;
      const blockStart = new Date(block.startAt).getTime();
      const blockEnd = blockStart + block.durationMinutes * 60_000;
      return start < blockEnd && end > blockStart;
    });
    if (hasAppointmentConflict || hasBlockConflict) throw new Error("Conflito de horário para este profissional.");
    const created = { ...input, id: `apt-${Date.now()}`, status: "PENDING" as const };
    appointments = [...appointments, created];
    persistDemoState();
    return created;
  },
  async updateAppointmentStatus(id: string, status: Appointment["status"]): Promise<Appointment> {
    await wait();
    appointments = appointments.map((appointment) => appointment.id === id ? { ...appointment, status } : appointment);
    persistDemoState();
    return appointments.find((appointment) => appointment.id === id) ?? appointments[0];
  },
  async rescheduleAppointment(id: string, startAt: string): Promise<Appointment> {
    await wait();
    const current = appointments.find((appointment) => appointment.id === id);
    if (!current) throw new Error("Consulta não encontrada.");
    const start = new Date(startAt).getTime();
    const end = start + current.durationMinutes * 60_000;
    const conflict = appointments.some((appointment) => {
      if (appointment.id === id || appointment.professionalName !== current.professionalName || appointment.status === "CANCELLED") return false;
      const appointmentStart = new Date(appointment.startAt).getTime();
      const appointmentEnd = appointmentStart + appointment.durationMinutes * 60_000;
      return start < appointmentEnd && end > appointmentStart;
    }) || calendarBlocks.some((block) => {
      if (block.professionalName !== current.professionalName) return false;
      const blockStart = new Date(block.startAt).getTime();
      const blockEnd = blockStart + block.durationMinutes * 60_000;
      return start < blockEnd && end > blockStart;
    });
    if (conflict) throw new Error("Conflito de horário para este profissional.");
    appointments = appointments.map((appointment) => appointment.id === id ? { ...appointment, startAt, status: "PENDING" as const } : appointment);
    persistDemoState();
    return appointments.find((appointment) => appointment.id === id) ?? current;
  },
  async getCalendarBlocks(): Promise<CalendarBlock[]> { await wait(); return [...calendarBlocks]; },
  async createCalendarBlock(input: Omit<CalendarBlock, "id">): Promise<CalendarBlock> {
    await wait();
    const start = new Date(input.startAt).getTime();
    const end = start + input.durationMinutes * 60_000;
    const conflict = appointments.some((appointment) => {
      if (appointment.professionalName !== input.professionalName || appointment.status === "CANCELLED") return false;
      const appointmentStart = new Date(appointment.startAt).getTime();
      const appointmentEnd = appointmentStart + appointment.durationMinutes * 60_000;
      return start < appointmentEnd && end > appointmentStart;
    }) || calendarBlocks.some((block) => {
      if (block.professionalName !== input.professionalName) return false;
      const blockStart = new Date(block.startAt).getTime();
      const blockEnd = blockStart + block.durationMinutes * 60_000;
      return start < blockEnd && end > blockStart;
    });
    if (conflict) throw new Error("Não é possível bloquear um horário ocupado.");
    const created = { ...input, id: `block-${Date.now()}` };
    calendarBlocks = [created, ...calendarBlocks];
    persistDemoState();
    return created;
  },
  async getPatients(): Promise<Patient[]> { await wait(); return [...patients]; },
  async createPatient(input: Pick<Patient, "name" | "email" | "phone">): Promise<Patient> {
    await wait();
    const created: Patient = { ...input, id: `pat-${Date.now()}`, firstName: input.name.split(" ")[0], status: "ACTIVE", lastAppointment: today, tags: ["Novo cadastro"], consentStatus: "REVIEW" };
    patients = [created, ...patients];
    persistDemoState();
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
    persistDemoState();
    return updated;
  },
  async createAddendum(recordId: string, contentPreview: string): Promise<MedicalRecordRevision> {
    await wait();
    const record = records.find((item) => item.id === recordId) ?? records[0];
    const revision: MedicalRecordRevision = { id: `rev-${Date.now()}`, label: `Addendum ${record.addendumCount + 1}`, createdAt: today, author: "Dra. Helena Costa", state: record.state, reason: contentPreview.slice(0, 80) };
    revisions = [revision, ...revisions];
    records = records.map((item) => item.id === record.id ? { ...item, addendumCount: item.addendumCount + 1, revisionCount: item.revisionCount + 1, updatedAt: today } : item);
    persistDemoState();
    return revision;
  },
  async getReceivables(): Promise<Receivable[]> { await wait(); return [...receivables]; },
  async createReceivable(input: Pick<Receivable, "patientName" | "description" | "dueDate" | "amount" | "origin">): Promise<Receivable> {
    await wait();
    const created: Receivable = { ...input, id: `recb-${Date.now()}`, status: "OPEN" };
    receivables = [created, ...receivables];
    persistDemoState();
    return created;
  },
  async getPayments(): Promise<Payment[]> { await wait(); return [...payments]; },
  async getBankTransactions(): Promise<BankTransaction[]> { await wait(); return [...bankTransactions]; },
  async getSettlements(): Promise<ProviderSettlement[]> { await wait(); return [...settlements]; },
  async getFiscalDocuments(): Promise<FiscalDocument[]> { await wait(); return [...fiscalDocuments]; },
  async createFiscalDocument(input: Pick<FiscalDocument, "patientName" | "amount">): Promise<FiscalDocument> {
    await wait();
    const created: FiscalDocument = { ...input, id: `nf-${Date.now()}`, number: `Lote ${Math.floor(Math.random() * 900 + 100)}`, issuedAt: today.slice(0, 10), status: "PROCESSING" };
    fiscalDocuments = [created, ...fiscalDocuments];
    persistDemoState();
    return created;
  },
  async getPackagePlans(): Promise<PackagePlan[]> { await wait(); return [...packagePlans]; },
  async createPackagePlan(input: Pick<PackagePlan, "name" | "sessions" | "price">): Promise<PackagePlan> {
    await wait();
    const created: PackagePlan = { ...input, id: `plan-${Date.now()}`, activeSubscriptions: 0, status: "DRAFT" };
    packagePlans = [created, ...packagePlans];
    persistDemoState();
    return created;
  },
  async updatePackagePlan(id: string, input: Pick<PackagePlan, "name" | "sessions" | "price">): Promise<PackagePlan> {
    await wait();
    packagePlans = packagePlans.map((plan) => plan.id === id ? { ...plan, ...input } : plan);
    persistDemoState();
    return packagePlans.find((plan) => plan.id === id) ?? packagePlans[0];
  },
  async getSubscriptions(): Promise<Subscription[]> { await wait(); return [...subscriptions]; },
  async createSubscription(input: Pick<Subscription, "patientName" | "planName" | "cycle" | "nextCharge">): Promise<Subscription> {
    await wait();
    const created: Subscription = { ...input, id: `sub-${Date.now()}`, status: "ACTIVE" };
    subscriptions = [created, ...subscriptions];
    persistDemoState();
    return created;
  },
  async getDeliveries(): Promise<NotificationDelivery[]> { await wait(); return [...deliveries]; },
  async retryDelivery(id: string): Promise<NotificationDelivery> {
    await wait();
    deliveries = deliveries.map((delivery) => delivery.id === id ? { ...delivery, status: "SENT", providerMessageId: "wamid.…retry" } : delivery);
    persistDemoState();
    return deliveries.find((delivery) => delivery.id === id) ?? deliveries[0];
  },
  async getPreferences(): Promise<NotificationPreference[]> { await wait(); return [...preferences]; },
  async updatePreference(id: string, enabled: boolean): Promise<NotificationPreference> {
    await wait();
    preferences = preferences.map((preference) => preference.id === id && !preference.required ? { ...preference, enabled } : preference);
    persistDemoState();
    return preferences.find((preference) => preference.id === id) ?? preferences[0];
  },
  async getPrivacyRequests(): Promise<PrivacyRequest[]> { await wait(); return [...privacyRequests]; },
  async getAuditEvents(): Promise<AuditEvent[]> { await wait(); return [...auditEvents]; },
  async getSecuritySignals(): Promise<SecuritySignal[]> { await wait(); return [...securitySignals]; },
  resetDemoData(): void {
    applyDemoState(clone(initialDemoState));
    persistDemoState();
  },
};

export type DataSource = typeof mockApi;

export const dataSource: DataSource = mockApi;
