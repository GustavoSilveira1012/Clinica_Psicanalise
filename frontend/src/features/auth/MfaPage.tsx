import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowLeft, KeyRound, ShieldCheck } from "lucide-react";
import { useLocation, useNavigate } from "react-router-dom";
import { Button, Card, Input, Notice } from "../../shared/components/ui";
import { mfaSchema, type MfaValues } from "../../shared/lib/validators";
import { useAuth } from "./AuthContext";

export function MfaPage() {
  const { pendingMfaEmail, pendingMfaEnrollment, pendingMfaChallenge, verifyMfa, setupMfa, confirmMfa } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [formError, setFormError] = useState("");
  const [setup, setSetup] = useState<{ secret: string; otpauthUri: string } | null>(null);
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<MfaValues>({ resolver: zodResolver(mfaSchema) });

  useEffect(() => {
    if (!pendingMfaEmail || !pendingMfaChallenge) { navigate("/login", { replace: true }); return; }
    if (pendingMfaEnrollment && !setup) void setupMfa().then(setSetup).catch((error: unknown) => setFormError(error instanceof Error ? error.message : "Não foi possível iniciar o cadastro do MFA."));
  }, [navigate, pendingMfaChallenge, pendingMfaEmail, pendingMfaEnrollment, setup, setupMfa]);

  async function onSubmit(values: MfaValues) {
    try { setFormError(""); if (pendingMfaEnrollment) await confirmMfa(values.code); else await verifyMfa(values.code); navigate(location.state?.from ?? "/dashboard", { replace: true }); }
    catch (error) { setFormError(error instanceof Error ? error.message : "Código inválido."); }
  }

  return <main className="flex min-h-screen items-center justify-center bg-cream p-5 dark:bg-slate-950"><Card className="w-full max-w-md p-8 shadow-xl"><div className="mb-8 flex h-14 w-14 items-center justify-center rounded-2xl bg-sage-50 text-sage-700 dark:bg-sage-950/40 dark:text-sage-200"><KeyRound size={26} /></div><p className="text-xs font-bold uppercase tracking-[0.18em] text-sage-600">Segundo fator</p><h1 className="mt-2 font-display text-3xl font-extrabold tracking-tight text-ink dark:text-white">{pendingMfaEnrollment ? "Proteja sua conta" : "Confirme sua identidade"}</h1><p className="mt-3 text-sm leading-6 text-slate-500">{pendingMfaEnrollment ? "Cadastre o PsicoGest no seu aplicativo autenticador e confirme o primeiro código." : "Digite o código de 6 dígitos do seu aplicativo autenticador."}</p>{pendingMfaEnrollment && setup && <Notice title="Cadastro do autenticador" tone="success"><span className="block">Chave manual:</span><code className="mt-2 block break-all text-xs">{setup.secret}</code><span className="mt-2 block text-xs">URI: {setup.otpauthUri}</span></Notice>}<form className="mt-7 space-y-5" onSubmit={handleSubmit(onSubmit)} noValidate><Input label="Código MFA" inputMode="numeric" autoComplete="one-time-code" placeholder="000000" maxLength={6} error={errors.code?.message} {...register("code")} />{formError && <p className="text-sm font-medium text-red-600" role="alert">{formError}</p>}<Button className="w-full" type="submit" disabled={isSubmitting || (pendingMfaEnrollment && !setup)}>{isSubmitting ? "Verificando…" : pendingMfaEnrollment ? "Ativar proteção" : "Entrar com segurança"}<ShieldCheck size={17} /></Button></form><button className="mt-6 inline-flex items-center gap-2 text-sm font-semibold text-slate-500 hover:text-sage-700" onClick={() => navigate("/login")}><ArrowLeft size={16} /> Voltar ao login</button></Card></main>;
}
