export type UserRole = "OWNER" | "CLINICAL" | "ADMIN" | "FINANCE";

export type Permission =
  | "dashboard:read"
  | "clinical:read"
  | "clinical:write"
  | "patients:read"
  | "patients:write"
  | "finance:read"
  | "finance:write"
  | "fiscal:read"
  | "packages:read"
  | "notifications:read"
  | "notifications:manage"
  | "privacy:read"
  | "settings:manage";

export type AppointmentStatus = "CONFIRMED" | "PENDING" | "COMPLETED" | "CANCELLED" | "NO_SHOW";
export type RecordState = "DRAFT" | "FINALIZED";
export type FinanceStatus = "OPEN" | "PAID" | "OVERDUE" | "CANCELLED";
export type NotificationStatus = "SENT" | "PENDING" | "FAILED" | "SUPPRESSED";

export interface Tenant {
  id: string;
  name: string;
  documentLabel: string;
  timezone: string;
  plan: string;
}

export interface AuthUser {
  id: string;
  name: string;
  email: string;
  role: UserRole;
  initials: string;
  tenant: Tenant;
  permissions: Permission[];
}

export interface AuthSession {
  user: AuthUser;
  sessionId: string;
  lastActivityAt: string;
}

export interface Appointment {
  id: string;
  startAt: string;
  durationMinutes: number;
  patientId: string;
  patientName: string;
  professionalName: string;
  type: "ONLINE" | "IN_PERSON";
  status: AppointmentStatus;
  note?: string;
}

export interface Patient {
  id: string;
  name: string;
  firstName: string;
  email: string;
  phone: string;
  status: "ACTIVE" | "INACTIVE";
  lastAppointment: string;
  nextAppointment?: string;
  tags: string[];
  consentStatus: "CURRENT" | "REVIEW";
}

export interface MedicalRecord {
  id: string;
  patientId: string;
  title: string;
  state: RecordState;
  updatedAt: string;
  author: string;
  contentPreview: string;
  revisionCount: number;
  addendumCount: number;
}

export interface MedicalRecordRevision {
  id: string;
  label: string;
  createdAt: string;
  author: string;
  state: RecordState;
  reason: string;
}

export interface Receivable {
  id: string;
  patientName: string;
  description: string;
  dueDate: string;
  amount: number;
  status: FinanceStatus;
  origin: "APPOINTMENT" | "PACKAGE" | "SUBSCRIPTION";
}

export interface Payment {
  id: string;
  patientName: string;
  receivedAt: string;
  amount: number;
  method: "PIX" | "CARD" | "CASH" | "TRANSFER";
  status: "CONFIRMED" | "PENDING" | "REFUNDED";
}

export interface BankTransaction {
  id: string;
  occurredAt: string;
  description: string;
  amount: number;
  status: "PENDING" | "RECONCILED" | "IGNORED";
}

export interface ProviderSettlement {
  id: string;
  providerName: string;
  period: string;
  gross: number;
  fees: number;
  net: number;
  status: "OPEN" | "READY" | "PAID";
}

export interface FiscalDocument {
  id: string;
  number: string;
  patientName: string;
  issuedAt: string;
  amount: number;
  status: "AUTHORIZED" | "PROCESSING" | "REJECTED" | "CANCELLED";
}

export interface PackagePlan {
  id: string;
  name: string;
  sessions: number;
  price: number;
  activeSubscriptions: number;
  status: "ACTIVE" | "DRAFT";
}

export interface Subscription {
  id: string;
  patientName: string;
  planName: string;
  cycle: string;
  nextCharge: string;
  status: "ACTIVE" | "PAST_DUE" | "PAUSED" | "CANCELLED";
}

export interface NotificationDelivery {
  id: string;
  createdAt: string;
  recipientLabel: string;
  channel: "EMAIL" | "WHATSAPP" | "SMS" | "IN_APP";
  eventType: string;
  status: NotificationStatus;
  providerMessageId?: string;
}

export interface NotificationPreference {
  id: string;
  label: string;
  description: string;
  enabled: boolean;
  required: boolean;
}

export interface PrivacyRequest {
  id: string;
  subjectLabel: string;
  type: "ACCESS" | "EXPORT" | "DELETION" | "CORRECTION";
  receivedAt: string;
  status: "OPEN" | "IN_PROGRESS" | "COMPLETED";
}

export interface AuditEvent {
  id: string;
  action: string;
  actor: string;
  occurredAt: string;
  resource: string;
  severity: "INFO" | "WARNING" | "CRITICAL";
}

export interface SecuritySignal {
  id: string;
  type: string;
  description: string;
  detectedAt: string;
  status: "OPEN" | "REVIEWED";
  severity: "LOW" | "MEDIUM" | "HIGH";
}

export interface DashboardData {
  stats: { label: string; value: string; change: string; trend: "up" | "down" | "neutral"; icon: string }[];
  upcoming: Appointment[];
  weeklyRevenue: { label: string; value: number }[];
  openAlerts: number;
}
