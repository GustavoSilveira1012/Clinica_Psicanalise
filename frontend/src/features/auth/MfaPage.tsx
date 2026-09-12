import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowLeft, KeyRound, ShieldCheck } from "lucide-react";
import { useLocation, useNavigate } from "react-router-dom";
import { Button, Card, Input } from "../../shared/components/ui";
import { mfaSchema, type MfaValues } from "../../shared/lib/validators";
import { useAuth } from "./AuthContext";

export function MfaPage() {
  const { pendingMfaEmail, verifyMfa } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [formError, setFormError] = useState("");
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<MfaValues>({ resolver: zodResolver(mfaSchema) });
  useEffect(() => { if (!pendingMfaEmail) navigate("/login", { replace: true }); }, [navigate, pendingMfaEmail]);
  async function onSubmit(values: MfaValues) { try { setFormError(""); await verifyMfa(values.code); navigate(location.state?.from ?? "/", { replace: true }); } catch (error) { setFormError(error instanceof Error ? error.message : "Código inválido."); } }
  return <main className="flex min-h-screen items-center justify-center bg-cream p-5 dark:bg-slate-950"><Card className="w-full max-w-md p-8 shadow-xl"><div className="mb-8 flex h-14 w-14 items-center justify-center rounded-2xl bg-sage-50 text-sage-700 dark:bg-sage-950/40 dark:text-sage-200"><KeyRound size={26} /></div><p className="text-xs font-bold uppercase tracking-[0.18em] text-sage-600">Segundo fator</p><h1 className="mt-2 font-display text-3xl font-extrabold tracking-tight text-ink dark:text-white">Confirme sua identidade</h1><p className="mt-3 text-sm leading-6 text-slate-500">Digite o código de 6 dígitos enviado para o seu aplicativo autenticador.</p><form className="mt-7 space-y-5" onSubmit={handleSubmit(onSubmit)} noValidate><Input label="Código MFA" inputMode="numeric" autoComplete="one-time-code" placeholder="000000" maxLength={6} error={errors.code?.message} {...register("code")} />{formError && <p className="text-sm font-medium text-red-600" role="alert">{formError}</p>}<Button className="w-full" type="submit" disabled={isSubmitting}>{isSubmitting ? "Verificando…" : "Entrar com segurança"}<ShieldCheck size={17} /></Button></form><button className="mt-6 inline-flex items-center gap-2 text-sm font-semibold text-slate-500 hover:text-sage-700" onClick={() => navigate("/login")}><ArrowLeft size={16} /> Voltar ao login</button></Card></main>;
}
