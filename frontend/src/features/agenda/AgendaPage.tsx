import { useEffect, useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useSearchParams } from "react-router-dom";
import { CalendarDays, Check, ChevronLeft, ChevronRight, Clock3, MapPin, Plus, Search, UserRound, Video, X } from "lucide-react";
import { dataSource } from "../../shared/lib/app-api";
import { appointmentRescheduleSchema, appointmentSchema, calendarBlockSchema, type AppointmentRescheduleValues, type AppointmentValues, type CalendarBlockFormValues, type CalendarBlockValues } from "../../shared/lib/validators";
import { formatDate, formatDateTime } from "../../shared/lib/format";
import type { Appointment } from "../../shared/types/domain";
import { Button, Card, EmptyState, Input, Modal, PageHeader, Select, Skeleton, StatusBadge } from "../../shared/components/ui";
import { useToast } from "../../shared/providers/ToastProvider";

function dateInputValue(date: Date) {
  return date.getFullYear() + "-" + String(date.getMonth() + 1).padStart(2, "0") + "-" + String(date.getDate()).padStart(2, "0");
}

function addDays(date: Date, days: number) {
  const next = new Date(date);
  next.setDate(next.getDate() + days);
  return next;
}

function startOfWeek(date: Date) {
  const start = new Date(date);
  start.setHours(0, 0, 0, 0);
  const day = start.getDay();
  start.setDate(start.getDate() - (day === 0 ? 6 : day - 1));
  return start;
}

function formatWeekRange(start: Date, end: Date) {
  const formatter = new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "long", year: "numeric" });
  return formatter.format(start) + " – " + formatter.format(end);
}

const defaultAppointmentValues: AppointmentValues = { patientId: "", date: dateInputValue(addDays(new Date(), 1)), time: "09:00", professionalName: "Profissional responsável", type: "ONLINE" };
const defaultBlockValues: CalendarBlockFormValues = { date: dateInputValue(addDays(new Date(), 1)), time: "12:00", durationMinutes: 60, reason: "Intervalo da equipe" };

export function AgendaPage() {
  const demoMode = import.meta.env.DEV && import.meta.env.VITE_DEMO_MODE === "true";
  const [searchParams] = useSearchParams();
  const preselectedPatientId = searchParams.get("patientId") ?? "";
  const [open, setOpen] = useState(Boolean(preselectedPatientId));
  const [blockOpen, setBlockOpen] = useState(false);
  const [rescheduleOpen, setRescheduleOpen] = useState(false);
  const [selectedAppointment, setSelectedAppointment] = useState<Appointment | null>(null);
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState("ALL");
  const [weekOffset, setWeekOffset] = useState(0);
  const queryClient = useQueryClient();
  const toast = useToast();
  const appointments = useQuery({ queryKey: ["appointments"], queryFn: dataSource.getAppointments });
  const blocks = useQuery({ queryKey: ["calendar-blocks"], queryFn: dataSource.getCalendarBlocks });
  const patients = useQuery({ queryKey: ["patients"], queryFn: dataSource.getPatients });
  const createAppointment = useMutation({
    mutationFn: dataSource.createAppointment,
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ["appointments"] }); queryClient.invalidateQueries({ queryKey: ["dashboard"] }); setOpen(false); toast(demoMode ? "Consulta de demonstração criada como pendente de confirmação." : "Consulta criada como pendente de confirmação."); },
    onError: (error) => toast(error instanceof Error ? error.message : "Não foi possível criar a consulta."),
  });
  const updateStatus = useMutation({
    mutationFn: ({ id, nextStatus }: { id: string; nextStatus: Appointment["status"] }) => dataSource.updateAppointmentStatus(id, nextStatus),
    onSuccess: (_appointment, variables) => { queryClient.invalidateQueries({ queryKey: ["appointments"] }); queryClient.invalidateQueries({ queryKey: ["dashboard"] }); setSelectedAppointment(null); toast(`Consulta marcada como ${statusLabel(variables.nextStatus).toLowerCase()}.`); },
    onError: () => toast("Não foi possível atualizar o status da consulta."),
  });
  const reschedule = useMutation({
    mutationFn: ({ id, startAt }: { id: string; startAt: string }) => dataSource.rescheduleAppointment(id, startAt),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ["appointments"] }); queryClient.invalidateQueries({ queryKey: ["dashboard"] }); setRescheduleOpen(false); setSelectedAppointment(null); toast(demoMode ? "Reagendamento de demonstração salvo como pendente de confirmação." : "Reagendamento salvo como pendente de confirmação."); },
    onError: (error) => toast(error instanceof Error ? error.message : "Não foi possível reagendar."),
  });
  const createBlock = useMutation({
    mutationFn: dataSource.createCalendarBlock,
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ["calendar-blocks"] }); setBlockOpen(false); toast(demoMode ? "Bloqueio de demonstração criado." : "Bloqueio de horário criado."); },
    onError: (error) => toast(error instanceof Error ? error.message : "Não foi possível bloquear o horário."),
  });
  const appointmentForm = useForm<AppointmentValues>({ resolver: zodResolver(appointmentSchema), defaultValues: { ...defaultAppointmentValues, patientId: preselectedPatientId } });
  const rescheduleForm = useForm<AppointmentRescheduleValues>({ resolver: zodResolver(appointmentRescheduleSchema) });
  const blockForm = useForm<CalendarBlockFormValues, unknown, CalendarBlockValues>({ resolver: zodResolver(calendarBlockSchema), defaultValues: defaultBlockValues });
  useEffect(() => {
    if (preselectedPatientId && patients.data?.some((patient) => patient.id === preselectedPatientId)) {
      appointmentForm.reset({ ...defaultAppointmentValues, patientId: preselectedPatientId });
      setOpen(true);
    }
  }, [appointmentForm, patients.data, preselectedPatientId]);
  const weekStart = useMemo(() => startOfWeek(addDays(new Date(), weekOffset * 7)), [weekOffset]);
  const weekEnd = useMemo(() => addDays(weekStart, 6), [weekStart]);
  const periodAppointments = useMemo(() => (appointments.data ?? []).filter((appointment) => { const time = new Date(appointment.startAt).getTime(); return time >= weekStart.getTime() && time < addDays(weekEnd, 1).getTime(); }), [appointments.data, weekEnd, weekStart]);
  const periodBlocks = useMemo(() => (blocks.data ?? []).filter((block) => { const time = new Date(block.startAt).getTime(); return time >= weekStart.getTime() && time < addDays(weekEnd, 1).getTime(); }), [blocks.data, weekEnd, weekStart]);
  const filtered = useMemo(() => periodAppointments.filter((appointment) => (status === "ALL" || appointment.status === status) && appointment.patientName.toLowerCase().includes(query.toLowerCase())), [periodAppointments, query, status]);
  const visibleBlocks = status === "ALL" ? periodBlocks : [];

  async function onSubmit(values: AppointmentValues) {
    const patient = patients.data?.find((item) => item.id === values.patientId);
    if (!patient) return;
    await createAppointment.mutateAsync({ patientId: patient.id, patientName: patient.name, startAt: toDemoIso(values.date, values.time), durationMinutes: 50, professionalName: values.professionalName, type: values.type });
    appointmentForm.reset(defaultAppointmentValues);
  }
  function openDetails(appointment: Appointment) { setSelectedAppointment(appointment); setRescheduleOpen(false); }
  function openReschedule(appointment: Appointment) { setSelectedAppointment(appointment); rescheduleForm.reset({ date: appointment.startAt.slice(0, 10), time: appointment.startAt.slice(11, 16) }); setRescheduleOpen(true); }
  function onReschedule(values: AppointmentRescheduleValues) { if (selectedAppointment) reschedule.mutate({ id: selectedAppointment.id, startAt: toDemoIso(values.date, values.time) }); }
  function onBlock(values: CalendarBlockValues) { createBlock.mutate({ startAt: toDemoIso(values.date, values.time), durationMinutes: values.durationMinutes, professionalName: "Profissional responsável", reason: values.reason }); }

  return <div>
    <PageHeader eyebrow="Clínica / Agenda" title="Agenda" description="Organize a operação dos profissionais sem perder o contexto do atendimento." actions={<>
      <Button variant="secondary" onClick={() => { setWeekOffset((offset) => offset - 1); setQuery(""); setStatus("ALL"); }}><ChevronLeft size={16} /> Semana anterior</Button>
      <Button variant="secondary" onClick={() => { setWeekOffset(0); setQuery(""); setStatus("ALL"); }}>Hoje</Button>
      <Button variant="secondary" onClick={() => { setWeekOffset((offset) => offset + 1); setQuery(""); setStatus("ALL"); }}>Próxima semana <ChevronRight size={16} /></Button>
      <Button variant="secondary" onClick={() => setBlockOpen(true)}><CalendarDays size={17} /> Bloquear horário</Button>
      <Button onClick={() => setOpen(true)}><Plus size={17} /> Nova consulta</Button>
    </>} />
    <div className="mb-6 grid gap-3 sm:grid-cols-[1fr_180px]">
      <label className="relative block"><span className="sr-only">Buscar por paciente</span><Search className="absolute left-3.5 top-3.5 text-slate-400" size={17} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Buscar por paciente…" className="min-h-11 w-full rounded-xl border border-slate-200 bg-white pl-10 pr-4 text-sm outline-none focus:border-sage-500 focus:ring-2 focus:ring-sage-500/20 dark:border-slate-700 dark:bg-slate-900 dark:text-white" /></label>
      <Select aria-label="Filtrar por status" value={status} onChange={(event) => setStatus(event.target.value)}><option value="ALL">Todos os status</option><option value="CONFIRMED">Confirmadas</option><option value="PENDING">Pendentes</option><option value="COMPLETED">Concluídas</option><option value="CANCELLED">Canceladas</option><option value="NO_SHOW">Faltas</option></Select>
    </div>
    <Card className="overflow-hidden">
      <div className="flex flex-col gap-3 border-b border-slate-100 p-5 sm:flex-row sm:items-center sm:justify-between dark:border-slate-800"><div><p className="text-xs font-bold uppercase tracking-wider text-slate-600">{formatWeekRange(weekStart, weekEnd)}</p><h2 className="mt-1 font-display text-lg font-bold text-ink dark:text-white">{weekOffset === 0 ? "Semana atual" : "Semana selecionada"}</h2></div><div className="flex flex-wrap items-center gap-2 text-xs text-slate-500"><span className="h-2 w-2 rounded-full bg-emerald-500" /> Disponível <span className="ml-2 h-2 w-2 rounded-full bg-amber-500" /> Pendente <span className="ml-2 inline-flex items-center gap-1 text-slate-600"><span className="h-2 w-2 rounded-full bg-slate-500" /> Bloqueio</span></div></div>
      {appointments.isPending ? <div className="space-y-3 p-5">{[1, 2, 3].map((item) => <Skeleton key={item} className="h-16" />)}</div> : appointments.isError ? <div className="p-5"><EmptyState title="Agenda indisponível" description="Não foi possível buscar os horários agora." action={<Button onClick={() => appointments.refetch()}>Tentar novamente</Button>} /></div> : filtered.length === 0 && !visibleBlocks.length ? <div className="p-5"><EmptyState title="Nenhuma consulta encontrada" description="Ajuste os filtros, navegue para outra semana ou crie uma nova consulta." action={<Button onClick={() => setOpen(true)}>Nova consulta</Button>} /></div> : <div className="divide-y divide-slate-100 dark:divide-slate-800">
        {filtered.map((appointment) => <div key={appointment.id} className="grid gap-3 p-5 transition hover:bg-slate-50/70 md:grid-cols-[130px_1fr_auto] md:items-center dark:hover:bg-slate-800/30"><div><p className="font-display text-lg font-extrabold text-ink dark:text-white">{new Date(appointment.startAt).toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" })}</p><p className="mt-1 text-xs text-slate-500">{formatDate(appointment.startAt, { weekday: "short", day: "2-digit", month: "short" })}</p></div><div className="flex min-w-0 items-center gap-3"><div className="grid h-10 w-10 shrink-0 place-items-center rounded-full bg-sage-100 text-sm font-bold text-sage-700 dark:bg-sage-900 dark:text-sage-200">{appointment.patientName.split(" ").map((part) => part[0]).slice(0, 2).join("")}</div><div className="min-w-0"><p className="truncate text-sm font-bold text-ink dark:text-white">{appointment.patientName}</p><p className="mt-1 flex flex-wrap items-center gap-2 text-xs text-slate-500"><span className="inline-flex items-center gap-1">{appointment.type === "ONLINE" ? <Video size={13} /> : <MapPin size={13} />}{appointment.type === "ONLINE" ? "Online" : "Sala 02"}</span><span className="inline-flex items-center gap-1"><Clock3 size={13} />{appointment.durationMinutes} min</span><span className="hidden sm:inline">· {appointment.professionalName}</span></p></div></div><div className="flex flex-wrap items-center gap-2 md:justify-end"><StatusBadge value={appointment.status} /><Button variant="ghost" className="min-h-8 px-2 text-xs" onClick={() => openDetails(appointment)}>Detalhes</Button></div></div>)}
        {visibleBlocks.map((block) => <div key={block.id} className="grid gap-3 bg-slate-50 p-5 md:grid-cols-[130px_1fr_auto] md:items-center dark:bg-slate-800/40"><div><p className="font-display text-lg font-extrabold text-slate-500 dark:text-slate-300">{new Date(block.startAt).toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" })}</p><p className="mt-1 text-xs text-slate-600">{formatDate(block.startAt, { weekday: "short", day: "2-digit", month: "short" })}</p></div><div className="flex min-w-0 items-center gap-3"><div className="grid h-10 w-10 shrink-0 place-items-center rounded-full bg-slate-200 text-slate-600 dark:bg-slate-700 dark:text-slate-300"><X size={17} /></div><div><p className="text-sm font-bold text-slate-600 dark:text-slate-200">Horário bloqueado</p><p className="mt-1 text-xs text-slate-500">{block.reason} · {block.professionalName} · {block.durationMinutes} min</p></div></div><span className="text-xs font-bold text-slate-600">Indisponível</span></div>)}
      </div>}
    </Card>
    <div className="mt-6 grid gap-4 md:grid-cols-3"><Card className="p-5"><p className="text-sm font-semibold text-slate-500">Consultas no período</p><p className="mt-2 font-display text-2xl font-extrabold text-ink dark:text-white">{filtered.length}</p><p className="mt-1 text-xs text-slate-500">Total calculado a partir da agenda carregada</p></Card><Card className="p-5"><p className="text-sm font-semibold text-slate-500">Taxa de confirmação</p><p className="mt-2 font-display text-2xl font-extrabold text-ink dark:text-white">{filtered.length ? `${Math.round((filtered.filter((item) => item.status === "CONFIRMED").length / filtered.length) * 100)}%` : "—"}</p><p className="mt-1 text-xs text-slate-500">Somente consultas do período selecionado</p></Card><Card className="p-5"><p className="text-sm font-semibold text-slate-500">Bloqueios no período</p><p className="mt-2 font-display text-2xl font-extrabold text-ink dark:text-white">{visibleBlocks.length}</p><p className="mt-1 text-xs text-slate-500">Validados pelo servidor</p></Card></div>
    <Modal open={Boolean(selectedAppointment) && !rescheduleOpen} onClose={() => setSelectedAppointment(null)} title="Detalhes da consulta" description={demoMode ? "Ambiente de demonstração: nenhuma mensagem é enviada ao paciente." : "As alterações são persistidas no backend; o envio de mensagens depende dos provedores configurados."}>{selectedAppointment && <div className="space-y-5"><div className="rounded-2xl bg-sage-50 p-4 dark:bg-sage-950/30"><p className="font-display text-xl font-extrabold text-ink dark:text-white">{selectedAppointment.patientName}</p><p className="mt-2 text-sm text-slate-600 dark:text-slate-300">{formatDateTime(selectedAppointment.startAt)} · {selectedAppointment.durationMinutes} min</p><p className="mt-1 flex items-center gap-2 text-sm text-slate-600"><UserRound size={15} /> {selectedAppointment.professionalName}</p></div><div className="flex flex-wrap gap-2"><StatusBadge value={selectedAppointment.status} /><StatusBadge value={selectedAppointment.type} /></div><div className="flex flex-wrap justify-end gap-2 border-t border-slate-100 pt-4 dark:border-slate-800"><Button variant="secondary" onClick={() => openReschedule(selectedAppointment)} disabled={selectedAppointment.status === "COMPLETED" || selectedAppointment.status === "CANCELLED"}><CalendarDays size={15} /> Reagendar</Button>{selectedAppointment.status === "PENDING" && <Button onClick={() => updateStatus.mutate({ id: selectedAppointment.id, nextStatus: "CONFIRMED" })} disabled={updateStatus.isPending}><Check size={15} /> Confirmar</Button>}{selectedAppointment.status === "CONFIRMED" && <><Button variant="secondary" onClick={() => updateStatus.mutate({ id: selectedAppointment.id, nextStatus: "NO_SHOW" })} disabled={updateStatus.isPending}>Registrar falta</Button><Button onClick={() => updateStatus.mutate({ id: selectedAppointment.id, nextStatus: "COMPLETED" })} disabled={updateStatus.isPending}>Marcar atendida</Button></>}{selectedAppointment.status !== "COMPLETED" && selectedAppointment.status !== "CANCELLED" && <Button variant="danger" onClick={() => updateStatus.mutate({ id: selectedAppointment.id, nextStatus: "CANCELLED" })} disabled={updateStatus.isPending}>Cancelar</Button>}</div></div>}</Modal>
    <Modal open={rescheduleOpen} onClose={() => { setRescheduleOpen(false); setSelectedAppointment(null); }} title="Reagendar consulta" description="O novo horário será validado contra consultas e bloqueios do profissional."><form className="space-y-4" onSubmit={rescheduleForm.handleSubmit(onReschedule)} noValidate><div className="grid gap-4 sm:grid-cols-2"><Input label="Nova data" type="date" error={rescheduleForm.formState.errors.date?.message} {...rescheduleForm.register("date")} /><Input label="Novo horário" type="time" error={rescheduleForm.formState.errors.time?.message} {...rescheduleForm.register("time")} /></div><div className="flex justify-end gap-2 border-t border-slate-100 pt-4 dark:border-slate-800"><Button variant="secondary" type="button" onClick={() => { setRescheduleOpen(false); setSelectedAppointment(null); }}>Cancelar</Button><Button type="submit" disabled={reschedule.isPending}>{reschedule.isPending ? "Validando…" : "Salvar reagendamento"}</Button></div></form></Modal>
    <Modal open={open} onClose={() => setOpen(false)} title="Nova consulta" description={demoMode ? "Ambiente de demonstração: o horário será salvo apenas localmente." : "A consulta será validada e persistida no backend. Nenhuma mensagem será enviada sem provedor configurado."}><form className="space-y-4" onSubmit={appointmentForm.handleSubmit(onSubmit)} noValidate><Select label="Paciente" error={appointmentForm.formState.errors.patientId?.message} {...appointmentForm.register("patientId")}><option value="">Selecione um paciente</option>{patients.data?.filter((patient) => patient.status === "ACTIVE").map((patient) => <option key={patient.id} value={patient.id}>{patient.name}</option>)}</Select><div className="grid gap-4 sm:grid-cols-2"><Input label="Data" type="date" error={appointmentForm.formState.errors.date?.message} {...appointmentForm.register("date")} /><Input label="Horário" type="time" error={appointmentForm.formState.errors.time?.message} {...appointmentForm.register("time")} /></div><Input label="Profissional" error={appointmentForm.formState.errors.professionalName?.message} {...appointmentForm.register("professionalName")} /><Select label="Formato" {...appointmentForm.register("type")}><option value="ONLINE">Online</option><option value="IN_PERSON">Presencial · Sala 02</option></Select><div className="flex justify-end gap-2 border-t border-slate-100 pt-4 dark:border-slate-800"><Button variant="secondary" type="button" onClick={() => setOpen(false)}>Cancelar</Button><Button type="submit" disabled={appointmentForm.formState.isSubmitting || createAppointment.isPending}>{createAppointment.isPending ? "Salvando…" : "Criar consulta"}</Button></div></form></Modal>
    <Modal open={blockOpen} onClose={() => setBlockOpen(false)} title="Bloquear horário" description={demoMode ? "Ambiente de demonstração: o bloqueio será salvo localmente." : "O bloqueio será persistido como exceção de disponibilidade do profissional."}><form className="space-y-4" onSubmit={blockForm.handleSubmit(onBlock)} noValidate><div className="grid gap-4 sm:grid-cols-2"><Input label="Data" type="date" error={blockForm.formState.errors.date?.message} {...blockForm.register("date")} /><Input label="Horário" type="time" error={blockForm.formState.errors.time?.message} {...blockForm.register("time")} /></div><Input label="Duração em minutos" type="number" min="15" step="15" error={blockForm.formState.errors.durationMinutes?.message} {...blockForm.register("durationMinutes")} /><Input label="Motivo" placeholder="Ex.: reunião, intervalo, férias" error={blockForm.formState.errors.reason?.message} {...blockForm.register("reason")} /><div className="rounded-xl bg-slate-50 p-3 text-xs leading-5 text-slate-500 dark:bg-slate-800/60">O bloqueio será aplicado ao profissional clínico selecionado pelo servidor.</div><div className="flex justify-end gap-2 border-t border-slate-100 pt-4 dark:border-slate-800"><Button variant="secondary" type="button" onClick={() => setBlockOpen(false)}>Cancelar</Button><Button type="submit" disabled={createBlock.isPending}>{createBlock.isPending ? "Salvando…" : "Bloquear horário"}</Button></div></form></Modal>
  </div>;
}

function toDemoIso(date: string, time: string) { return new Date(`${date}T${time}:00-03:00`).toISOString(); }
function statusLabel(status: Appointment["status"]) { return { CONFIRMED: "Confirmada", PENDING: "Pendente", COMPLETED: "Concluída", CANCELLED: "Cancelada", NO_SHOW: "Falta" }[status]; }
