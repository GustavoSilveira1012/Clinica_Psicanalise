import type {
  Appointment, AuditEvent, BankTransaction, CalendarBlock, DashboardData, FiscalDocument,
  MedicalRecord, NotificationDelivery, NotificationPreference, PackagePlan,
  Patient, Payment, PrivacyRequest, ProviderSettlement, Receivable, RecordState, SecuritySignal,
  Subscription,
} from "../types/domain";
import { apiClient, ApiError } from "./api-client";
import type { DataSource } from "./mock-api";

type ApiPatient = { id: number; name: string; email: string; phone?: string; birthDate?: string; active: boolean };
type ApiAppointment = { id: number; patientId: number; patientName: string; psychoanalystName: string; scheduledStart: string; scheduledEnd: string; appointmentType: "ONLINE" | "IN_PERSON"; status: string };
type ApiRecordSummary = { id: string; authorPsychoanalystId: number; authorName: string; status: RecordState; createdAt: string; finalizedAt?: string };
type ApiRecord = ApiRecordSummary & { patientId: number; content: string; updatedAt: string; version: number };
type ApiReceivable = { id: string; patientId?: number; description: string; netAmount: number; outstandingAmount: number; overdue: boolean; status: string; dueDate: string };
type ApiPayment = { id: string; patientId: number; amount: number; paymentMethod: string; status: string; receivedAt?: string; createdAt: string };
type ApiServiceInvoice = { id: string; status: string; invoiceNumber?: string; nfseId?: string; netAmount: number; createdAt: string };
type ApiNotificationDelivery = { id: string; createdAt: string; recipientLabel: string; channel: "EMAIL" | "WHATSAPP" | "SMS"; eventType: string; status: string; providerMessageId?: string };
type ApiNotificationPreference = { id: string; notificationType: string; channel: "EMAIL" | "WHATSAPP" | "SMS"; label: string; description: string; enabled: boolean; required: boolean };

const unsupported = (capability: string): never => {
  throw new ApiError(`${capability} ainda não está habilitado neste ambiente. Conecte o endpoint do backend antes de operar com dados reais.`, 501, "CAPABILITY_NOT_CONFIGURED");
};

function localDateTime(value: string) {
  return value.length >= 19 ? value.slice(0, 19) : value;
}

function money(value: number | string | undefined) {
  return Number(value ?? 0);
}

function toAppointment(value: ApiAppointment): Appointment {
  const status: Appointment["status"] = value.status === "CONFIRMED" ? "CONFIRMED" : value.status === "COMPLETED" ? "COMPLETED" : value.status === "CANCELLED" ? "CANCELLED" : value.status === "NO_SHOW" ? "NO_SHOW" : "PENDING";
  return {
    id: String(value.id), startAt: value.scheduledStart, durationMinutes: Math.max(1, Math.round((new Date(value.scheduledEnd).getTime() - new Date(value.scheduledStart).getTime()) / 60000)),
    patientId: String(value.patientId), patientName: value.patientName, professionalName: value.psychoanalystName,
    type: value.appointmentType, status,
  };
}

function toPatient(value: ApiPatient): Patient {
  const firstName = value.name.trim().split(/\s+/)[0] || value.name;
  return { id: String(value.id), name: value.name, firstName, email: value.email, phone: value.phone ?? "", status: value.active ? "ACTIVE" : "INACTIVE", lastAppointment: "", tags: [], consentStatus: "REVIEW" };
}

function toFinanceStatus(status: string, overdue = false): Receivable["status"] {
  if (status === "CANCELLED") return "CANCELLED";
  if (status === "PAID") return "PAID";
  if (overdue) return "OVERDUE";
  return "OPEN";
}

function toPaymentMethod(method: string): Payment["method"] {
  if (method === "PIX") return "PIX";
  if (method === "CREDIT_CARD" || method === "DEBIT_CARD") return "CARD";
  if (method === "BANK_TRANSFER") return "TRANSFER";
  return "CASH";
}

function toFiscalStatus(status: string): FiscalDocument["status"] {
  if (status === "AUTHORIZED") return "AUTHORIZED";
  if (status === "REJECTED") return "REJECTED";
  if (status === "CANCELLED") return "CANCELLED";
  return "PROCESSING";
}

function toNotificationStatus(status: string): NotificationDelivery["status"] {
  if (status === "SENT" || status === "DELIVERED") return "SENT";
  if (status === "FAILED" || status === "DEAD_LETTER") return "FAILED";
  if (status === "SUPPRESSED") return "SUPPRESSED";
  return "PENDING";
}

function toRecord(value: ApiRecordSummary, patientId: string): MedicalRecord {
  return { id: value.id, patientId, title: "Acompanhamento clínico", state: value.status, updatedAt: value.finalizedAt ?? value.createdAt, author: value.authorName, contentPreview: "Conteúdo protegido. Abra o registro para visualizar com autorização.", revisionCount: 1, addendumCount: 0 };
}

async function professionalId() {
  const existing = apiClient.getProfessionalId();
  if (existing) return existing;
  const professionals = await apiClient.request<Array<{ id: number; active: boolean }>>("/psychoanalysts");
  const value = professionals.find((item) => item.active)?.id;
  if (!value) throw new ApiError("Nenhum profissional clínico ativo foi encontrado.", 409);
  apiClient.setProfessionalId(value);
  return value;
}

async function getPatients() {
  const response = await apiClient.request<ApiPatient[]>("/patients");
  return response.map(toPatient);
}

async function fetchAppointments() {
  const id = await professionalId();
  const response = await apiClient.request<ApiAppointment[]>(`/psychoanalysts/${id}/appointments`);
  return response.map(toAppointment);
}

export const realApi: DataSource = {
  async getDashboard(): Promise<DashboardData> {
    const [appointments, patients] = await Promise.all([fetchAppointments(), getPatients()]);
    const today = new Date().toISOString().slice(0, 10);
    const upcoming = appointments.filter((item) => item.startAt.slice(0, 10) >= today && item.status !== "CANCELLED").sort((a, b) => a.startAt.localeCompare(b.startAt)).slice(0, 4);
    return { stats: [
      { label: "Consultas hoje", value: String(appointments.filter((item) => item.startAt.slice(0, 10) === today && item.status !== "CANCELLED").length).padStart(2, "0"), change: "Dados do servidor", trend: "neutral", icon: "calendar" },
      { label: "Pacientes ativos", value: String(patients.filter((item) => item.status === "ACTIVE").length), change: "Dados do servidor", trend: "neutral", icon: "users" },
      { label: "A receber", value: "—", change: "Consulte o financeiro", trend: "neutral", icon: "wallet" },
      { label: "Registros para revisar", value: "—", change: "Acesso clínico", trend: "neutral", icon: "clipboard" },
    ], upcoming, weeklyRevenue: [], openAlerts: 0 };
  },
  async getAppointments() { return fetchAppointments(); },
  async createAppointment(input) { const id = await professionalId(); const start = new Date(input.startAt); const end = new Date(start.getTime() + input.durationMinutes * 60000); const response = await apiClient.request<ApiAppointment>(`/psychoanalysts/${id}/appointments`, { method: "POST", body: JSON.stringify({ patientId: Number(input.patientId), scheduledStart: localDateTime(input.startAt), scheduledEnd: localDateTime(end.toISOString()), appointmentType: input.type }) }); return toAppointment(response); },
  async updateAppointmentStatus(id, status) { const professional = await professionalId(); const paths: Record<string, string> = { CONFIRMED: "confirm", COMPLETED: "complete", NO_SHOW: "no-show", CANCELLED: "cancel" }; if (status === "CANCELLED") { const response = await apiClient.request<ApiAppointment>(`/psychoanalysts/${professional}/appointments/${id}/cancel`, { method: "PATCH", body: JSON.stringify({ reason: "Cancelamento solicitado pela equipe" }) }); return toAppointment(response); } const action = paths[status]; if (!action) unsupported("Esta transição de agenda"); const response = await apiClient.request<ApiAppointment>(`/psychoanalysts/${professional}/appointments/${id}/${action}`, { method: "PATCH" }); return toAppointment(response); },
  async rescheduleAppointment(id, startAt) { const professional = await professionalId(); const end = new Date(new Date(startAt).getTime() + 50 * 60000); const response = await apiClient.request<ApiAppointment>(`/psychoanalysts/${professional}/appointments/${id}/reschedule`, { method: "POST", body: JSON.stringify({ scheduledStart: localDateTime(startAt), scheduledEnd: localDateTime(end.toISOString()) }) }); return toAppointment(response); },
  async getCalendarBlocks() {
    const id = await professionalId();
    const from = new Date();
    from.setDate(from.getDate() - 30);
    const to = new Date();
    to.setDate(to.getDate() + 90);
    const params = new URLSearchParams({ from: from.toISOString().slice(0, 10), to: to.toISOString().slice(0, 10) });
    const response = await apiClient.request<Array<{ id: number; date: string; startTime?: string; endTime?: string; fullDay?: boolean; reason?: string }>>(`/psychoanalysts/${id}/availability-exceptions?${params}`);
    return response.filter((item) => item.fullDay || item.startTime).map((item): CalendarBlock => {
      const startAt = `${item.date}T${item.fullDay ? "00:00" : (item.startTime ?? "00:00")}:00-03:00`;
      const start = new Date(startAt).getTime();
      const endAt = item.fullDay ? `${item.date}T23:59:00-03:00` : `${item.date}T${item.endTime ?? item.startTime ?? "00:00"}:00-03:00`;
      return { id: String(item.id), startAt, durationMinutes: Math.max(1, Math.round((new Date(endAt).getTime() - start) / 60000)), professionalName: "Profissional responsável", reason: item.reason ?? "Indisponibilidade" };
    });
  },
  async createCalendarBlock(input) {
    const id = await professionalId();
    const startAt = new Date(input.startAt);
    const endAt = new Date(startAt.getTime() + input.durationMinutes * 60000);
    const response = await apiClient.request<{ id: number; date: string; startTime?: string; endTime?: string; reason?: string }>(`/psychoanalysts/${id}/availability-exceptions`, { method: "POST", body: JSON.stringify({ date: input.startAt.slice(0, 10), type: "BLOCKED", startTime: input.startAt.slice(11, 16), endTime: endAt.toISOString().slice(11, 16), observation: input.reason }) });
    return { ...input, id: String(response.id) };
  },
  async createPatient(input) { const password = crypto.randomUUID() + "Aa1!"; const response = await apiClient.request<ApiPatient>("/patients", { method: "POST", body: JSON.stringify({ ...input, password }) }); return toPatient(response); },
  getPatients,
  async getPatient(id) { try { const response = await apiClient.request<ApiPatient>(`/patients/${id}`); return toPatient(response); } catch (error) { if (error instanceof ApiError && error.status === 404) return undefined; throw error; } },
  async getRecords(patientId) { const response = await apiClient.request<ApiRecordSummary[]>(`/patients/${patientId}/medical-records`); return response.map((item) => toRecord(item, patientId)); },
  async getRecordContent(recordId) { const response = await apiClient.request<ApiRecord>(`/medical-records/${recordId}`); return response.content; },
  async getRevisions(recordId) { const response = await apiClient.request<Array<{ id: string; revisionNumber: number; authorName: string; createdAt: string }>>(`/api/v1/medical-records/${recordId}/revisions`); return response.map((item) => ({ id: item.id, label: `Revisão ${item.revisionNumber}`, createdAt: item.createdAt, author: item.authorName, state: "FINALIZED" as const, reason: "Revisão clínica" })); },
  async saveRecord(recordId, content, state) { const updated = await apiClient.request<ApiRecord>(`/medical-records/${recordId}`, { method: "PUT", body: JSON.stringify({ content }) }); if (state === "FINALIZED") await apiClient.request(`/medical-records/${recordId}/finalize`, { method: "PATCH" }); return toRecord(updated, String(updated.patientId)); },
  async createAddendum(recordId, content) { const response = await apiClient.request<{ id: string; authorName: string; reason: string; createdAt: string }>(`/api/v1/medical-records/${recordId}/addendums`, { method: "POST", body: JSON.stringify({ content, reason: "COMPLEMENT" }) }); return { id: response.id, label: "Adendo", createdAt: response.createdAt, author: response.authorName, state: "FINALIZED" as const, reason: response.reason }; },
  async getReceivables() {
    const [items, patients] = await Promise.all([
      apiClient.request<ApiReceivable[]>("/api/v1/receivables"),
      getPatients(),
    ]);
    const names = new Map(patients.map((patient) => [Number(patient.id), patient.name]));
    return items.map((item): Receivable => ({
      id: item.id,
      patientName: names.get(item.patientId ?? 0) ?? "Paciente não identificado",
      description: item.description,
      dueDate: item.dueDate,
      amount: money(item.netAmount),
      status: toFinanceStatus(item.status, item.overdue),
      origin: "APPOINTMENT",
    }));
  },
  async createReceivable(input) {
    const patients = await getPatients();
    const patient = patients.find((item) => item.name.trim().toLowerCase() === input.patientName.trim().toLowerCase());
    if (!patient) throw new ApiError("Selecione um paciente cadastrado antes de criar o lançamento.", 422, "PATIENT_REQUIRED");
    const response = await apiClient.request<ApiReceivable>("/api/v1/receivables", {
      method: "POST",
      body: JSON.stringify({
        patientId: Number(patient.id),
        description: input.description,
        grossAmount: input.amount,
        discountAmount: 0,
        dueDate: input.dueDate,
      }),
    });
    return { ...input, id: response.id, amount: money(response.netAmount), status: toFinanceStatus(response.status, response.overdue) };
  },
  async getPayments(): Promise<Payment[]> {
    const [items, patients] = await Promise.all([
      apiClient.request<ApiPayment[]>("/api/v1/payments"),
      getPatients(),
    ]);
    const names = new Map(patients.map((patient) => [Number(patient.id), patient.name]));
    return items.map((item): Payment => ({
      id: item.id,
      patientName: names.get(item.patientId) ?? "Paciente não identificado",
      receivedAt: item.receivedAt ?? item.createdAt,
      amount: money(item.amount),
      method: toPaymentMethod(item.paymentMethod),
      status: item.status === "REFUNDED" ? "REFUNDED" : item.status === "PENDING" ? "PENDING" : "CONFIRMED",
    }));
  },
  async getBankTransactions(): Promise<BankTransaction[]> { return unsupported("Conciliação bancária"); },
  async getSettlements(): Promise<ProviderSettlement[]> { return unsupported("Repasses de provedores"); },
  async getFiscalDocuments(): Promise<FiscalDocument[]> {
    const items = await apiClient.request<ApiServiceInvoice[]>("/service-invoices");
    return items.map((item) => ({
      id: item.id,
      number: item.invoiceNumber ?? item.nfseId ?? item.id,
      patientName: "Identificação protegida",
      issuedAt: item.createdAt,
      amount: money(item.netAmount),
      status: toFiscalStatus(item.status),
    }));
  },
  async createFiscalDocument(): Promise<FiscalDocument> { return unsupported("Emissão fiscal"); },
  async getPackagePlans(): Promise<PackagePlan[]> {
    const items = await apiClient.request<Array<{ id: string; name: string; status: string; sessions: number; price: number; activeSubscriptions: number }>>("/api/v1/package-plans");
    return items.map((item) => ({ id: item.id, name: item.name, sessions: item.sessions, price: money(item.price), activeSubscriptions: item.activeSubscriptions, status: item.status === "ACTIVE" ? "ACTIVE" : "DRAFT" }));
  },
  async createPackagePlan(): Promise<PackagePlan> { return unsupported("Criação de planos"); },
  async updatePackagePlan(): Promise<PackagePlan> { return unsupported("Atualização de planos"); },
  async getSubscriptions(): Promise<Subscription[]> {
    const items = await apiClient.request<Array<{ id: string; patientName: string; planName: string; cycle: string; nextCharge?: string; status: string }>>("/api/v1/subscriptions");
    return items.map((item) => ({ id: item.id, patientName: item.patientName, planName: item.planName, cycle: item.cycle, nextCharge: item.nextCharge ?? "", status: item.status === "PAST_DUE" ? "PAST_DUE" : item.status === "PAUSED" ? "PAUSED" : item.status === "CANCELLED" ? "CANCELLED" : "ACTIVE" }));
  },
  async createSubscription(): Promise<Subscription> { return unsupported("Criação de assinaturas clínicas"); },
  async getDeliveries(): Promise<NotificationDelivery[]> {
    const items = await apiClient.request<ApiNotificationDelivery[]>("/api/v1/notifications/deliveries");
    return items.map((item) => ({ id: item.id, createdAt: item.createdAt, recipientLabel: item.recipientLabel, channel: item.channel, eventType: item.eventType, status: toNotificationStatus(item.status), providerMessageId: item.providerMessageId }));
  },
  async retryDelivery(): Promise<NotificationDelivery> { return unsupported("Reprocessamento de notificações"); },
  async getPreferences(): Promise<NotificationPreference[]> {
    const items = await apiClient.request<ApiNotificationPreference[]>("/api/v1/notifications/preferences");
    return items.map((item) => ({ id: item.id, label: item.label, description: item.description, enabled: item.enabled, required: item.required }));
  },
  async updatePreference(id, enabled): Promise<NotificationPreference> {
    const item = await apiClient.request<ApiNotificationPreference>(`/api/v1/notifications/preferences/${id}`, { method: "PUT", body: JSON.stringify({ enabled }) });
    return { id: item.id, label: item.label, description: item.description, enabled: item.enabled, required: item.required };
  },
  async getPrivacyRequests(): Promise<PrivacyRequest[]> {
    const items = await apiClient.request<Array<{ id: string; subjectLabel: string; type: "ACCESS" | "EXPORT" | "DELETION" | "CORRECTION"; receivedAt: string; status: "OPEN" | "IN_PROGRESS" | "COMPLETED" }>>("/api/v1/compliance/privacy-requests");
    return items;
  },
  async getAuditEvents(): Promise<AuditEvent[]> {
    const items = await apiClient.request<Array<{ id: string; action: string; actor: string; occurredAt: string; resource: string; severity: "INFO" | "WARNING" | "CRITICAL" }>>("/api/v1/compliance/audit-events");
    return items;
  },
  async getSecuritySignals(): Promise<SecuritySignal[]> {
    const items = await apiClient.request<Array<{ id: string; type: string; description: string; detectedAt: string; status: "OPEN" | "REVIEWED"; severity: "LOW" | "MEDIUM" | "HIGH" }>>("/api/v1/compliance/security-signals");
    return items;
  },
  resetDemoData() { /* Não há reset de dados reais no cliente. */ },
};
