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

export type LoginValues = z.infer<typeof loginSchema>;
export type AppointmentValues = z.infer<typeof appointmentSchema>;
export type PatientValues = z.infer<typeof patientSchema>;
export type MfaValues = z.infer<typeof mfaSchema>;
export type RecordValues = z.infer<typeof recordSchema>;
