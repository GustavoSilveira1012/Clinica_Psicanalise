import { z } from "zod";

export const loginSchema = z.object({
  email: z.string().email("Informe um e-mail válido."),
  password: z.string().min(6, "A senha deve ter pelo menos 6 caracteres."),
});

export const appointmentSchema = z.object({
  patientId: z.string().min(1, "Selecione um paciente."),
  date: z.string().min(1, "Informe a data."),
  time: z.string().min(1, "Informe o horário."),
  professionalName: z.string().min(1, "Informe o profissional."),
  type: z.enum(["ONLINE", "IN_PERSON"]),
});

export const appointmentRescheduleSchema = z.object({
  date: z.string().min(1, "Informe a nova data."),
  time: z.string().min(1, "Informe o novo horário."),
});

export const calendarBlockSchema = z.object({
  date: z.string().min(1, "Informe a data."),
  time: z.string().min(1, "Informe o horário."),
  durationMinutes: z.coerce.number().int().min(15, "Use ao menos 15 minutos."),
  reason: z.string().min(3, "Informe o motivo do bloqueio."),
});

export const patientSchema = z.object({
  name: z.string().min(3, "Informe o nome completo."),
  email: z.string().email("Informe um e-mail válido."),
  phone: z.string().min(10, "Informe um telefone válido."),
});

export const mfaSchema = z.object({
  code: z.string().regex(/^\d{6}$/, "Informe os 6 dígitos do código."),
});

export const recordSchema = z.object({
  content: z.string().min(20, "Adicione pelo menos 20 caracteres ao registro."),
});

export const packagePlanSchema = z.object({
  name: z.string().min(2, "Informe o nome do plano."),
  sessions: z.coerce.number().int().min(1, "Informe ao menos 1 sessão."),
  price: z.coerce.number().positive("Informe um preço válido."),
});

export const subscriptionSchema = z.object({
  patientName: z.string().min(2, "Informe o paciente."),
  planName: z.string().min(2, "Selecione um plano."),
  cycle: z.string().min(1, "Selecione o ciclo."),
  nextCharge: z.string().min(1, "Informe a próxima cobrança."),
});

export const receivableSchema = z.object({
  patientName: z.string().min(2, "Informe o paciente."),
  description: z.string().min(3, "Informe a descrição."),
  dueDate: z.string().min(1, "Informe o vencimento."),
  amount: z.coerce.number().positive("Informe um valor válido."),
  origin: z.enum(["APPOINTMENT", "PACKAGE", "SUBSCRIPTION"]),
});

export const addendumSchema = z.object({
  content: z.string().min(20, "Adicione pelo menos 20 caracteres ao addendum."),
});

export const fiscalDocumentSchema = z.object({
  patientName: z.string().min(2, "Informe o paciente."),
  amount: z.coerce.number().positive("Informe um valor válido."),
});

export const teamInviteSchema = z.object({
  name: z.string().min(3, "Informe o nome completo."),
  email: z.string().email("Informe um e-mail válido."),
  role: z.enum(["CLINICAL", "ADMIN", "FINANCE"]),
});

export const demoRequestSchema = z.object({
  name: z.string().min(3, "Informe seu nome completo."),
  email: z.string().email("Informe um e-mail válido."),
  clinic: z.string().min(2, "Informe o nome da clínica."),
  teamSize: z.enum(["1-5", "6-15", "16+"]),
  requestedDate: z.string().min(1, "Escolha uma data de preferência."),
  timeSlot: z.string().min(1, "Escolha um horário de preferência."),
  message: z.string().min(10, "Conte brevemente o que deseja conhecer."),
});

export type LoginValues = z.infer<typeof loginSchema>;
export type AppointmentValues = z.infer<typeof appointmentSchema>;
export type AppointmentRescheduleValues = z.infer<typeof appointmentRescheduleSchema>;
export type CalendarBlockValues = z.infer<typeof calendarBlockSchema>;
export type CalendarBlockFormValues = z.input<typeof calendarBlockSchema>;
export type PatientValues = z.infer<typeof patientSchema>;
export type MfaValues = z.infer<typeof mfaSchema>;
export type RecordValues = z.infer<typeof recordSchema>;
export type PackagePlanValues = z.infer<typeof packagePlanSchema>;
export type PackagePlanFormValues = z.input<typeof packagePlanSchema>;
export type SubscriptionValues = z.infer<typeof subscriptionSchema>;
export type ReceivableValues = z.infer<typeof receivableSchema>;
export type ReceivableFormValues = z.input<typeof receivableSchema>;
export type AddendumValues = z.infer<typeof addendumSchema>;
export type FiscalDocumentValues = z.infer<typeof fiscalDocumentSchema>;
export type FiscalDocumentFormValues = z.input<typeof fiscalDocumentSchema>;
export type TeamInviteValues = z.infer<typeof teamInviteSchema>;
export type DemoRequestValues = z.infer<typeof demoRequestSchema>;
