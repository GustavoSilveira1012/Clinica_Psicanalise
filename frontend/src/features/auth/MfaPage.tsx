import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowLeft, KeyRound, ShieldCheck } from "lucide-react";
import QRCode from "qrcode";
import { useLocation, useNavigate } from "react-router-dom";
import { Button, Card, Input } from "../../shared/components/ui";
import { mfaSchema, type MfaValues } from "../../shared/lib/validators";
import { useAuth } from "./AuthContext";

export function MfaPage() {
  const { pendingMfaEmail, pendingMfaEnrollment, pendingMfaChallenge, verifyMfa, setupMfa, confirmMfa } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [formError, setFormError] = useState("");
  const [setup, setSetup] = useState<{ secret: string; otpauthUri: string } | null>(null);
  const [qrDataUrl, setQrDataUrl] = useState<string | null>(null);
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<MfaValues>({ resolver: zodResolver(mfaSchema) });

  useEffect(() => {
    if (!pendingMfaEmail || !pendingMfaChallenge) { navigate("/login", { replace: true }); return; }
    if (pendingMfaEnrollment && !setup) void setupMfa().then(setSetup).catch((error: unknown) => setFormError(error instanceof Error ? error.message : "Não foi possível iniciar o cadastro do MFA."));
  }, [navigate, pendingMfaChallenge, pendingMfaEmail, pendingMfaEnrollment, setup, setupMfa]);

  useEffect(() => {
    if (!setup) { setQrDataUrl(null); return; }
    let active = true;
    // otpauthUri contém o segredo TOTP: renderizado localmente, sem rede e sem log.
    void QRCode.toDataURL(setup.otpauthUri, { margin: 1, width: 176, errorCorrectionLevel: "M" })
      .then((url) => { if (active) setQrDataUrl(url); })
      .catch(() => { if (active) setQrDataUrl(null); });
    return () => { active = false; };
  }, [setup]);

  async function onSubmit(values: MfaValues) {
    try { setFormError(""); if (pendingMfaEnrollment) await confirmMfa(values.code); else await verifyMfa(values.code); navigate(location.state?.from ?? "/dashboard", { replace: true }); }
    catch (error) { setFormError(error instanceof Error ? error.message : "Código inválido."); }
  }

  return <main className="flex min-h-screen items-center justify-center bg-paper p-5 dark:bg-slate-950"><Card className="w-full max-w-md p-8 shadow-xl"><div className="mb-8 flex h-14 w-14 items-center justify-center rounded-2xl bg-harbor-50 text-harbor-700 dark:bg-harbor-950/40 dark:text-harbor-200"><KeyRound size={26} /></div><p className="text-sm font-medium text-harbor-600 dark:text-harbor-300">Segundo fator</p><h1 className="mt-2 font-display text-3xl font-extrabold tracking-tight text-ink dark:text-white">{pendingMfaEnrollment ? "Proteja sua conta" : "Confirme sua identidade"}</h1><p className="mt-3 text-sm leading-6 text-slate-500 dark:text-slate-400">{pendingMfaEnrollment ? "Cadastre o PsicoGest no seu aplicativo autenticador e confirme o primeiro código." : "Digite o código de 6 dígitos do seu aplicativo autenticador."}</p>{pendingMfaEnrollment && setup && <div className="mt-6 rounded-2xl border border-slate-200 bg-white p-5 dark:border-slate-800 dark:bg-slate-900"><p className="text-sm font-semibold text-ink dark:text-white">Escaneie no seu autenticador</p><p className="mt-1 text-xs leading-5 text-slate-500 dark:text-slate-400">Abra o app (Google Authenticator, Authy, 1Password) e leia o código abaixo.</p>{qrDataUrl ? <img src={qrDataUrl} alt="QR code para cadastrar o PsicoGest no aplicativo autenticador" width={176} height={176} className="mx-auto mt-4 h-44 w-44 rounded-xl border border-slate-200 bg-white p-2 dark:border-slate-700" /> : <div className="mx-auto mt-4 h-44 w-44 animate-pulse rounded-xl bg-slate-100 dark:bg-slate-800" aria-hidden="true" />}<div className="mt-4"><p className="text-xs font-medium text-slate-500 dark:text-slate-400">Não consegue escanear? Digite a chave manualmente:</p><code className="mt-1.5 block break-all rounded-lg bg-slate-50 px-3 py-2 text-xs text-ink dark:bg-slate-950 dark:text-slate-200">{setup.secret}</code></div></div>}<form className="mt-7 space-y-5" onSubmit={handleSubmit(onSubmit)} noValidate><Input label="Código MFA" inputMode="numeric" autoComplete="one-time-code" placeholder="000000" maxLength={6} error={errors.code?.message} {...register("code")} />{formError && <p className="text-sm font-medium text-red-600 dark:text-red-400" role="alert">{formError}</p>}<Button className="w-full" type="submit" disabled={isSubmitting || (pendingMfaEnrollment && !setup)}>{isSubmitting ? "Verificando…" : pendingMfaEnrollment ? "Ativar proteção" : "Entrar com segurança"}<ShieldCheck size={17} /></Button></form><button className="mt-6 inline-flex items-center gap-2 text-sm font-semibold text-slate-500 dark:text-slate-400 hover:text-harbor-700" onClick={() => navigate("/login")}><ArrowLeft size={16} /> Voltar ao login</button></Card></main>;
}
